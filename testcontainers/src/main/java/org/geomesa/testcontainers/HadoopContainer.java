package org.geomesa.testcontainers;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class HadoopContainer
      extends GenericContainer<HadoopContainer> {

    public HadoopContainer() {
        this(DockerImageName.parse("ghcr.io/geomesa/accumulo-uno").withTag("2.1"));
    }

    public HadoopContainer(DockerImageName imageName) {
        super(imageName);
        addEnv("UNO_SERVICE", "hadoop");
        addExposedPorts(8020);
        withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("hadoop"), true));
    }

    public String getHdfsUrl() {
        return "hdfs://" + getHost() + ":" + getFirstMappedPort();
    }
}
