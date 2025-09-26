package org.geomesa.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class UnoContainer<T extends UnoContainer<T>> extends GenericContainer<T> {

    private static final Logger logger = LoggerFactory.getLogger(UnoContainer.class);

    static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("ghcr.io/geomesa/accumulo-uno").withTag("2.1.4-jdk17");

    public UnoContainer(DockerImageName imageName) {
        super(imageName);
        // to get networking right, we need to make the container use the same hostname as the host -
        // that way when it returns server locations, they will map to localhost and go through the correct port bindings
        String hostname = null;
        try {
            hostname =
                    Runtime.getRuntime()
                            .exec("hostname -s")
                            .onExit()
                            .thenApply((p) -> {
                                try (InputStream is = p.getInputStream()) {
                                    return IOUtils.toString(is, StandardCharsets.UTF_8).trim();
                                } catch (IOException e) {
                                    logger.error("Error reading hostname:", e);
                                    return null;
                                }
                            })
                            .get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error("Error reading hostname, networking may not work correctly:", e);
        }
        if (hostname != null) {
            String finalHostname = hostname;
            // noinspection resource
            withCreateContainerCmdModifier((cmd) -> cmd.withHostName(finalHostname));
            // noinspection resource
            withNetworkAliases(hostname);
        }
    }
}
