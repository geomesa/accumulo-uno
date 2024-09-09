# accumulo-uno

This project is a wrapper for [fluo-uno](https://github.com/apache/fluo-uno).

Note that the container is meant for quick tests, and data may be corrupted or lost on shutdown. If data needs to be
preserved, `exec` into the running container and run `$UNO_HOME/bin/uno stop accumulo` to perform a graceful shutdown.

## Quick Start - Docker

    docker pull ghcr.io/geomesa/accumulo-uno:2.1.2
    docker run --rm \
      -p 2181:2181 -p 9997:9997 -p 9999:9999 \
      --hostname $(hostname -s) \
      ghcr.io/geomesa/accumulo-uno:2.1.2

Note that the `hostname` must be set to the hostname of the host in order for Accumulo's networking to work.

The Accumulo connection properties are available in the container:

    docker cp $(docker ps | grep accumulo-uno | awk '{ print $1 }'):/opt/fluo-uno/install/accumulo/conf/accumulo-client.properties .

### Using the Docker with GeoMesa

In order to use GeoMesa, the distributed runtime JAR must be mounted in to the container. The distributed runtime
JAR is available from [GeoMesa](https://github.com/locationtech/geomesa/releases):

    wget 'https://github.com/locationtech/geomesa/releases/download/geomesa-4.0.5/geomesa-accumulo_2.12-4.0.5-bin.tar.gz'
    tar -xf accumulo_2.12-4.0.5-bin.tar.gz
    docker run --rm \
      -p 2181:2181 -p 9997:9997 -p 9999:9999 \
      --hostname $(hostname -s) \
      -v "$(pwd)"/geomesa-accumulo_2.12-4.0.5/dist/accumulo/geomesa-accumulo-distributed-runtime_2.12-4.0.5.jar:/opt/fluo-uno/install/accumulo/lib/geomesa-accumulo-distributed-runtime.jar \
      ghcr.io/geomesa/accumulo-uno:2.1.2

## Quick Start - Testcontainers

Add the following dependencies:

    <dependency>
      <groupId>org.geomesa.testcontainers</groupId>
      <artifactId>testcontainers-accumulo</artifactId>
      <version>1.4.1</version>
      <scope>test</scope>
    </dependency>
    <!-- only required for GeoMesa support -->
    <dependency>
      <groupId>org.locationtech.geomesa</groupId>
      <artifactId>geomesa-accumulo-distributed-runtime_2.12</artifactId>
      <version>5.0.1</version>
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

* Accumulo 2.1.3
* Hadoop 3.3.6

## Ports

Most functionality should work with the following ports exposed:

* `2181` - Zookeeper
* `9997` - Accumulo Tablet Server
* `9999` - Accumulo Manager Server

See the Accumulo [docs](https://accumulo.apache.org/docs/2.x/administration/in-depth-install#network) for a full list of ports.

## Persistence

To persist data between runs, mount a persistent volume at `/opt/fluo-uno/install/data`. To prevent data loss
and recovery on startup, set `UNO_GRACEFUL_STOP` to `true`. When starting the docker with an existing volume,
set `UNO_COMMAND` to `start`. 

## Configuration

The following environment variables are supported at runtime:

* `ZOOKEEPER_PORT` - override the default Zookeeper port
* `TSERVER_PORT` - override the default tablet server port
* `MANAGER_PORT` - override the default manager client port
* `UNO_HOST` - bind the Accumulo processes to the specified host
* `UNO_COMMAND` - the command used to run fluo-uno, default `run`
* `UNO_GRACEFUL_STOP` - enable a graceful shutdown to prevent data loss
