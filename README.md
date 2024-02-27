# accumulo-uno

This project is a wrapper for [fluo-uno](https://github.com/apache/fluo-uno).

Accumulo, Hadoop and Zookeeper are all configured by IP address when the container is started. As such, they should
generally be accessible from the host machine (usually using `localhost` through exposed ports) or through a docker
network (using the docker `host`.)

Note that the container is meant for quick tests, and data may be corrupted or lost on shutdown. If data needs to be
preserved, `exec` into the running container and run `$UNO_HOME/bin/uno stop accumulo` to perform a graceful shutdown.

## Quick Start - Docker

    docker pull ghcr.io/geomesa/accumulo-uno:2.1.2
    docker run --rm -p 2181:2181 -p 9997:9997 ghcr.io/geomesa/accumulo-uno:2.1.2

## Quick Start - Testcontainers

Add the following dependencies:

    <dependency>
      <groupId>org.geomesa.testcontainers</groupId>
      <artifactId>testcontainers-accumulo</artifactId>
      <version>1.0.0</version>
      <scope>test</scope>
    </dependency>
    <!-- only required for GeoMesa support -->
    <dependency>
      <groupId>org.locationtech.geomesa</groupId>
      <artifactId>geomesa-accumulo-distributed-runtime_2.12</artifactId>
      <version>4.0.5</version>
      <scope>test</scope>
    </dependency>

Write unit tests against Accumulo:

    import org.geomesa.testcontainers.AccumuloContainer;

    static AccumuloContainer accumulo = new AccumuloContainer().withGeoMesaDistributedRuntime();
    
    @BeforeAll
    static void beforeAll() {
      accumulo.start();
    }
    
    @AfterAll
    static void afterAll() {
      accumulo.stop();
    }

    @Test
    void testAccumuloConnection() {
      try(AccumuloClient client = accumulo.clien();) {
        client.tableOperations().create("foo");
        assertTrue(client.tableOperations().exists("foo"));
      }
    }
    
## Versions

The docker currently uses:

* Accumulo 2.1.2
* Hadoop 3.3.6

## Ports

Most functionality should work with the following ports exposed:

* `2181` - Zookeeper
* `9997` - Accumulo Tablet Server

See the Accumulo [docs](https://accumulo.apache.org/docs/2.x/administration/in-depth-install#network) for a full list of ports.

## Configuration

The following environment variables are supported at runtime:

* `ZOOKEEPER_PORT` - override the default Zookeeper port
* `TSERVER_PORT` - override the default tablet server port
