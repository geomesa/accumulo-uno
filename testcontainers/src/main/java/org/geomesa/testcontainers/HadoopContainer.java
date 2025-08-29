package org.geomesa.testcontainers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class HadoopContainer
      extends UnoContainer<HadoopContainer> {

    private static final Logger logger = LoggerFactory.getLogger(HadoopContainer.class);

    private final int namenodePort = getFreePort();
    private final int datanodePort = getFreePort();
    private final int datanodeIpcPort = getFreePort();
    private final int journalnodeRpcPort = getFreePort();
    private final int resourceManagerSchedulerPort = getFreePort();
    private final int resourceManagerTrackerPort = getFreePort();
    private final int resourceManagerPort = getFreePort();
    private final int jobHistoryPort = getFreePort();

    public HadoopContainer() {
        this(DEFAULT_IMAGE);
    }

    @SuppressWarnings("resource")
    public HadoopContainer(DockerImageName imageName) {
        super(imageName);
        addEnv("UNO_SERVICE", "hadoop");
        addEnv("NAMENODE_PORT", Integer.toString(namenodePort));
        addEnv("DATANODE_PORT", Integer.toString(datanodePort));
        addEnv("DATANODE_IPC_PORT", Integer.toString(datanodeIpcPort));
        addEnv("JOURNALNODE_RPC_PORT", Integer.toString(journalnodeRpcPort));
        addEnv("YARN_RESOURCEMANAGER_SCHEDULER_PORT", Integer.toString(resourceManagerSchedulerPort));
        addEnv("YARN_RESOURCEMANAGER_TRACKER_PORT", Integer.toString(resourceManagerTrackerPort));
        addEnv("YARN_RESOURCEMANAGER_PORT", Integer.toString(resourceManagerPort));
        addEnv("MAPRED_JOBHISTORY_PORT", Integer.toString(jobHistoryPort));
        addFixedExposedPort(namenodePort, namenodePort);
        addFixedExposedPort(datanodePort, datanodePort);
        addFixedExposedPort(datanodeIpcPort, datanodeIpcPort);
        addFixedExposedPort(journalnodeRpcPort, journalnodeRpcPort);
        addFixedExposedPort(resourceManagerSchedulerPort, resourceManagerSchedulerPort);
        addFixedExposedPort(resourceManagerTrackerPort, resourceManagerTrackerPort);
        addFixedExposedPort(resourceManagerPort, resourceManagerPort);
        addFixedExposedPort(jobHistoryPort, jobHistoryPort);
        addExposedPorts(8088, 9870);// resource manager UI, namenode UI
        // noinspection resource
        waitingFor(Wait.forLogMessage(".*Running hadoop complete.*", 1));
        // noinspection resource
        withLogConsumer(new HadoopLogConsumer());
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("The NameNode UI is available locally at: " + getHost() + ":" + getMappedPort(9870) + "/");
        logger.info("The ResourceManager UI is available locally at: " + getHost() + ":" + getMappedPort(8088) + "/");
    }

    /**
     * Get the HDFS url used to connect to this instance
     *
     * @return the hdfs url
     */
    public String getHdfsUrl() {
        return "hdfs://" + getHost() + ":" + namenodePort;
    }

    /**
     * Get the port that HDFS is listening on
     *
     * @return the port
     */
    public int getHdfsPort() {
        return namenodePort;
    }

    /**
     * Get the core-site.xml configuration for this cluster
     *
     * @return xml
     */
    public String getConfigurationXml() {
        return "<configuration>\n" +
               "  <property>\n" +
               "    <name>fs.defaultFS</name>\n" +
               "    <value>" + getHdfsUrl() + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>dfs.namenode.rpc-address</name>\n" +
               "    <value>" + getHost() + ":" + namenodePort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>dfs.datanode.address</name>\n" +
               "    <value>" + getHost() + ":" + datanodePort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>dfs.datanode.ipc.address</name>\n" +
               "    <value>" + getHost() + ":" + datanodeIpcPort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>dfs.journalnode.rpc-address</name>\n" +
               "    <value>" + getHost() + ":" + journalnodeRpcPort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>yarn.resourcemanager.scheduler.address</name>\n" +
               "    <value>" + getHost() + ":" + resourceManagerSchedulerPort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>yarn.resourcemanager.resource-tracker.address</name>\n" +
               "    <value>" + getHost() + ":" + resourceManagerTrackerPort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>yarn.resourcemanager.address</name>\n" +
               "    <value>" + getHost() + ":" + resourceManagerPort + "</value>\n" +
               "  </property>\n" +
               "  <property>\n" +
               "    <name>mapreduce.jobhistory.address</name>\n" +
               "    <value>" + getHost() + ":" + jobHistoryPort + "</value>\n" +
               "  </property>\n" +
                "  <property>\n" +
                // required to for networking outside the docker container
                "    <name>dfs.client.use.datanode.hostname</name>\n" +
                "    <value>true</value>\n" +
                "  </property>\n" +
               "</configuration>";
    }

    private static int getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Unable to get free port", e);
        }
    }

    private static class HadoopLogConsumer
          extends Slf4jLogConsumer {

        private boolean output = true;

        public HadoopLogConsumer() {
            super(LoggerFactory.getLogger("hadoop"), true);
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            if (output) {
                super.accept(outputFrame);
                if (outputFrame.getUtf8StringWithoutLineEnding().matches(".*Running hadoop complete.*")) {
                    output = false;
                    byte[] msg = "Container started - suppressing further output".getBytes(StandardCharsets.UTF_8);
                    super.accept(new OutputFrame(OutputFrame.OutputType.STDOUT, msg));
                }
            }
        }
    }
}
