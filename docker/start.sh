#!/bin/bash

# exit on command error
set -e

UNO_HOME="${UNO_HOME:-/opt/fluo-uno}"
UNO_HOST="${UNO_HOST:-$(hostname -I | awk '{ print $1 }')}" # use IP address if not specified
UNO_SERVICE="${UNO_SERVICE:-accumulo}"
UNO_COMMAND="${UNO_COMMAND:-run}"
UNO_GRACEFUL_STOP="${UNO_GRACEFUL_STOP:-}"

if ! pgrep -x sshd &>/dev/null; then
  /usr/sbin/sshd
fi

SECONDS=0
while true; do
  if ssh-keyscan localhost 2>&1 | grep -q OpenSSH; then
    echo "ssh is up"
    break
  fi
  if [ "$SECONDS" -gt 20 ]; then
    echo "FAILED: ssh failed to come up after 20 secs"
    exit 1
  fi
  echo "waiting for ssh to come up"
  sleep 1
  ((SECONDS+=1))
done

(
ssh-keyscan localhost        || :
ssh-keyscan 0.0.0.0          || :
ssh-keyscan "$UNO_HOST"      || :
ssh-keyscan "$(hostname -f)" || :
) 2>/dev/null >> /root/.ssh/known_hosts

echo "Setting host to $UNO_HOST"
grep -rl REPLACE_HOST "$UNO_HOME"/install/ | xargs sed -i "s/REPLACE_HOST/$UNO_HOST/g"

ZK_PORT=${ZOOKEEPER_PORT:-2181}
if [[ $ZK_PORT != "2181" ]]; then
  echo "Setting zookeeper port to $ZK_PORT"
  sed -i "s/2181/$ZK_PORT/g" "$UNO_HOME"/install/zookeeper/conf/zoo.cfg
  sed -i "s/2181/$ZK_PORT/g" "$UNO_HOME"/install/accumulo/conf/accumulo-it.properties
  sed -i "s/2181/$ZK_PORT/g" "$UNO_HOME"/install/accumulo/conf/accumulo-client.properties
  sed -i "s/2181/$ZK_PORT/g" "$UNO_HOME"/install/accumulo/conf/accumulo.properties
fi

TSERVER_PORT=${TSERVER_PORT:-9997}
if [[ $TSERVER_PORT != "9997" ]]; then
  echo "Setting tserver port to $TSERVER_PORT"
  echo -e "\ntserver.port.client=${TSERVER_PORT}" >> "$UNO_HOME"/install/accumulo/conf/accumulo.properties
fi

# make everything in hdfs world-writable for easier development
sed -i '/<\/configuration>/d' "$UNO_HOME"/install/hadoop/etc/hadoop/hdfs-site.xml
{
  echo "  <property>"
  echo "    <name>dfs.permissions.enabled</name>"
  echo "    <value>false</value>"
  echo "  </property>"
  echo "</configuration>"
  echo ""
} >> "$UNO_HOME"/install/hadoop/etc/hadoop/hdfs-site.xml

# shellcheck disable=SC1090
source <("$UNO_HOME"/bin/uno env)
"$UNO_HOME"/bin/uno "$UNO_COMMAND" "$UNO_SERVICE"

# handle stopping on kill signal
_stop() {
  echo "Shutting down..."
  if [[ -n "$UNO_GRACEFUL_STOP" ]]; then
    "$UNO_HOME"/bin/uno stop "$UNO_SERVICE"
  else
    kill "$child" 2>/dev/null
  fi
}

trap _stop TERM INT
tail -f /dev/null "$UNO_HOME"/install/logs/"$UNO_SERVICE"/* &
child=$!
wait "$child"
