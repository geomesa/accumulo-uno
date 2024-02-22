# accumulo-uno-docker

This project is a wrapper for [fluo-uno](https://github.com/apache/fluo-uno).

Accumulo, Hadoop and Zookeeper are all configured by IP address when the container is started. As such, they should
generally be accessible from the host machine (usually using `localhost` through exposed ports) or through a docker
network (using the docker `host`.)

Note that the container is meant for quick tests, and data may be corrupted or lost on shutdown. If data needs to be
preserved, `exec` into the running container and run `$UNO_HOME/bin/uno stop accumulo` to perform a graceful shutdown.

## Versions

The docker currently uses:

* Accumulo 2.1.2
* Hadoop 3.3.6

## Ports

Most functionality should work with the following ports exposed:

* `2181` - Zookeeper
* `9997` - Accumulo Tablet Server

See the Accumulo [docs](https://accumulo.apache.org/docs/2.x/administration/in-depth-install#network) for a full
list of ports.

## Configuration

The following environment variables are supported at runtime:

* `ZOOKEEPER_PORT` - override the default Zookeeper port
* `TSERVER_PORT` - override the default tablet server port
