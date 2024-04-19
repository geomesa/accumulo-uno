package org.geomesa.testcontainers;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;

public class AccumuloContainer
      extends UnoContainer<AccumuloContainer> {

    private static final Logger logger = LoggerFactory.getLogger(AccumuloContainer.class);

    private final String instanceName = "uno";
    private final String username = "root";
    private final String password = "secret";
    private final int zookeeperPort = getFreePort();

    public AccumuloContainer() {
        this(DEFAULT_IMAGE);
    }

    public AccumuloContainer(DockerImageName imageName) {
        super(imageName);
        int tserverPort = getFreePort();
        int managerPort = getFreePort();
        addFixedExposedPort(zookeeperPort, zookeeperPort);
        addFixedExposedPort(tserverPort, tserverPort);
        addFixedExposedPort(managerPort, managerPort);
        addExposedPorts(9995); // accumulo monitor, for debugging
        addEnv("ZOOKEEPER_PORT", Integer.toString(zookeeperPort));
        addEnv("TSERVER_PORT", Integer.toString(tserverPort));
        addEnv("MANAGER_PORT", Integer.toString(managerPort));
        // noinspection resource
        withLogConsumer(new AccumuloLogConsumer());
    }

    public AccumuloContainer withGeoMesaDistributedRuntime() {
        return withGeoMesaDistributedRuntime(findDistributedRuntime());
    }

    public AccumuloContainer withGeoMesaDistributedRuntime(String jarHostPath) {
        logger.info("Binding to host path {}", jarHostPath);
        return withFileSystemBind(jarHostPath,
                                  "/opt/fluo-uno/install/accumulo/lib/geomesa-accumulo-distributed-runtime.jar",
                                  BindMode.READ_ONLY);
    }

    public AccumuloClient client() {
        return client(username, password);
    }

    public AccumuloClient client(String username, String password) {
        return client(username, new PasswordToken(password.getBytes(StandardCharsets.UTF_8)));
    }

    public AccumuloClient client(String username, AuthenticationToken password) {
        Properties props = new Properties();
        props.setProperty(ClientProperty.INSTANCE_NAME.getKey(), instanceName);
        props.setProperty(ClientProperty.INSTANCE_ZOOKEEPERS.getKey(), getZookeepers());
        return Accumulo.newClient().from(props).as(username, password).build();
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getZookeepers() {
        return getHost() + ":" + zookeeperPort;
    }

    private static int getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Unable to get free port", e);
        }
    }

    private static final String DISTRIBUTED_RUNTIME_PROPS = "geomesa-accumulo-distributed-runtime.properties";

    private static String findDistributedRuntime() {
        String path = null;
        try {
            URL url = AccumuloContainer.class.getClassLoader().getResource(DISTRIBUTED_RUNTIME_PROPS);
            URI uri = url == null ? null : url.toURI();
            logger.debug("Distributed runtime lookup: {}", uri);
            if (uri != null && uri.toString().endsWith("/target/classes/" + DISTRIBUTED_RUNTIME_PROPS)) {
                // running through an IDE
                File targetDir = Paths.get(uri).toFile().getParentFile().getParentFile();
                File[] names = targetDir.listFiles((dir, name) ->
                                                         name.startsWith("geomesa-accumulo-distributed-runtime_") &&
                                                         (name.endsWith("-SNAPSHOT.jar") || name.matches(
                                                               ".*-[0-9]+\\.[0-9]+\\.[0-9]+\\.jar")));
                if (names != null && names.length == 1) {
                    path = names[0].getAbsolutePath();
                }
            } else if (uri != null && "jar".equals(uri.getScheme())) {
                // running through maven
                String jar = uri.toString().substring(4).replaceAll("\\.jar!.*", ".jar");
                path = Paths.get(URI.create(jar)).toFile().getAbsolutePath();
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not load geomesa-accumulo-distributed-runtime JAR from classpath", e);
        }
        if (path == null) {
            throw new RuntimeException(
                  "Could not load geomesa-accumulo-distributed-runtime JAR from classpath");
        }
        return path;
    }

    private static class AccumuloLogConsumer
          extends Slf4jLogConsumer {

        private boolean output = true;

        public AccumuloLogConsumer() {
            super(LoggerFactory.getLogger("accumulo"), true);
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            if (output) {
                super.accept(outputFrame);
                if (outputFrame.getUtf8StringWithoutLineEnding().matches(".*Running accumulo complete.*")) {
                    output = false;
                    byte[] msg = "Container started - suppressing further output".getBytes(StandardCharsets.UTF_8);
                    super.accept(new OutputFrame(OutputFrame.OutputType.STDOUT, msg));
                }
            }
        }
    }
}
