# accumulo-uno

This project is a wrapper for [fluo-uno](https://github.com/apache/fluo-uno).

Note that the container is meant for quick tests, and data may be corrupted or lost on shutdown. If data needs to be
preserved, use the environment variable `UNO_GRACEFUL_STOP=true` to perform a graceful shutdown.

## Quick Start - Docker

    docker pull ghcr.io/geomesa/accumulo-uno:2.1
    docker run --rm \
      --name accumulo \
      -p 2181:2181 -p 9997:9997 -p 9999:9999 \
      --hostname $(hostname -s) \
      ghcr.io/geomesa/accumulo-uno:2.1

Note that the `hostname` must be set to the hostname of the host in order for Accumulo's networking to work.

The Accumulo connection properties are available in the container:

    docker cp accumulo:/opt/fluo-uno/install/accumulo/conf/accumulo-client.properties .

### Using the Docker with GeoMesa

In order to use GeoMesa, the distributed runtime JAR must be mounted in to the container. The distributed runtime
JAR is available from [GeoMesa](https://github.com/locationtech/geomesa/releases):

    wget 'https://github.com/locationtech/geomesa/releases/download/geomesa-5.0.1/geomesa-accumulo_2.12-5.0.1-bin.tar.gz'
    tar -xf geomesa-accumulo_2.12-5.0.1-bin.tar.gz
    docker run --rm \
      --name accumulo \
      -p 2181:2181 -p 9997:9997 -p 9999:9999 \
      --hostname $(hostname -s) \
      -v "$(pwd)"/geomesa-accumulo_2.12-5.0.1/dist/accumulo/geomesa-accumulo-distributed-runtime_2.12-5.0.1.jar:/opt/fluo-uno/install/accumulo/lib/geomesa-accumulo-distributed-runtime.jar \
      ghcr.io/geomesa/accumulo-uno:2.1

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

* `UNO_COMMAND` - the command used to run fluo-uno. One of:
  * `run` (default) - initialize and run a new cluster
  * `start` - will start an existing cluster (requires persistent state from a previous run)
  * `debug` - will run the container but not start any services
  * `add-volumes` - will run `accumulo init --add-volumes` before startup, i.e. for S3 support (requires persistent state from a previous run)
* `UNO_GRACEFUL_STOP` - enable a graceful shutdown to prevent data loss
* `UNO_HOST` - bind the Accumulo processes to the specified host
* `UNO_SERVICE` - the service to run, one of `accumulo` (default), `hadoop` or `zookeeper`

### Port Configuration

The following environment variables are supported to override the ports used by various services:

* `DATANODE_IPC_PORT` - override the default Hadoop data node IPC port
* `DATANODE_PORT` - override the default Hadoop data node port
* `JOURNALNODE_RPC_PORT` - override the default Hadoop journal node RPC port
* `MANAGER_PORT` - override the default manager client port
* `MAPRED_JOBHISTORY_PORT` - override the default Yarn job history port
* `NAMENODE_PORT` - override the default Hadoop name node rpc port
* `TSERVER_PORT` - override the default tablet server port
* `YARN_RESOURCEMANAGER_SCHEDULER_PORT` - override the default Yarn scheduler port
* `YARN_RESOURCEMANAGER_TRACKER_PORT` - override the default Yarn resource tracker port
* `YARN_RESOURCEMANAGER_PORT` - override the default Yarn resource manager port
* `ZOOKEEPER_PORT` - override the default Zookeeper port

### S3 Configuration

The following environment variables are supported for storing data in S3:

* `AWS_ACCESS_KEY_ID` - credentials for connecting to S3
* `AWS_SECRET_ACCESS_KEY` - credentials for connecting to S3
* `S3_ENDPOINT` - override the default S3 endpoint (e.g. for use with minio)
* `S3_PATH_STYLE_ACCESS` - Boolean to enable or disable path-style access (default true)
* `S3_REGION` - override the default S3 region
* `S3_SSL_ENABLED` - Boolean to enable or disable SSL in S3 connections
* `S3_VOLUME` - `s3a://` path to a volume used for storing data. Requires a run with `UNO_COMMAND=add-volumes` before it can be used.

## Tags and Versions

### Tag `2.1`, `2.1.3`

* Accumulo 2.1.3
* Hadoop 3.3.6

### Tag `2.1.2`

* Accumulo 2.1.2
* Hadoop 3.3.6
