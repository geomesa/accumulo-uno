#!/bin/bash

# exit on command error
set -e

UNO_HOME="${UNO_HOME:-/opt/fluo-uno}"
UNO_HOST="${UNO_HOST:-$(hostname -s)}" # use hostname if not specified
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

if [[ -n "$ZOOKEEPER_PORT" ]] && [[ $ZOOKEEPER_PORT != "2181" ]]; then
  echo "Setting zookeeper port to $ZOOKEEPER_PORT"
  sed -i "s/2181/$ZOOKEEPER_PORT/g" "$UNO_HOME"/install/zookeeper/conf/zoo.cfg
  sed -i "s/2181/$ZOOKEEPER_PORT/g" "$UNO_HOME"/install/accumulo/conf/accumulo-it.properties
  sed -i "s/2181/$ZOOKEEPER_PORT/g" "$UNO_HOME"/install/accumulo/conf/accumulo-client.properties
  sed -i "s/2181/$ZOOKEEPER_PORT/g" "$UNO_HOME"/install/accumulo/conf/accumulo.properties
fi

if [[ -n "$TSERVER_PORT" ]] && [[ $TSERVER_PORT != "9997" ]]; then
  echo "Setting tserver.port.client to $TSERVER_PORT"
  echo -e "\ntserver.port.client=${TSERVER_PORT}" >> "$UNO_HOME"/install/accumulo/conf/accumulo.properties
fi

if [[ -n "$MANAGER_PORT" ]] && [[ $MANAGER_PORT != "9999" ]]; then
  echo "Setting manager.port.client to $MANAGER_PORT"
  echo -e "\nmanager.port.client=${MANAGER_PORT}" >> "$UNO_HOME"/install/accumulo/conf/accumulo.properties
fi

# sets a hadoop config value - assumes that the value does not already exist
# params: <config file name> <config property> <config value>
function setHadoopConf() {
  echo "Setting $2 to $3"
  sed -i '/<\/configuration>/d' "$UNO_HOME/install/hadoop/etc/hadoop/$1"
  {
    echo "  <property>"
    echo "    <name>$2</name>"
    echo "    <value>$3</value>"
    echo "  </property>"
    echo "</configuration>"
    echo ""
  } >> "$UNO_HOME/install/hadoop/etc/hadoop/$1"
}

if [[ -n "$NAMENODE_PORT" ]] && [[ $NAMENODE_PORT != "8020" ]]; then
  setHadoopConf hdfs-site.xml dfs.namenode.rpc-address "$UNO_HOST:$NAMENODE_PORT"
  echo "Setting fs.defaultFS to $UNO_HOST:$NAMENODE_PORT"
  sed -i "s/REPLACE_HOST:8020/$UNO_HOST:$NAMENODE_PORT/" "$UNO_HOME/install/hadoop/etc/hadoop/core-site.xml"
fi
if [[ -n "$DATANODE_PORT" ]] && [[ $DATANODE_PORT != "9866" ]]; then
  setHadoopConf hdfs-site.xml dfs.datanode.address "$UNO_HOST:$DATANODE_PORT"
fi
if [[ -n "$DATANODE_IPC_PORT" ]] && [[ $DATANODE_IPC_PORT != "9867" ]]; then
  setHadoopConf hdfs-site.xml dfs.datanode.ipc.address "$UNO_HOST:$DATANODE_IPC_PORT"
fi
if [[ -n "$JOURNALNODE_RPC_PORT" ]] && [[ $JOURNALNODE_RPC_PORT != "8485" ]]; then
  setHadoopConf hdfs-site.xml dfs.journalnode.rpc-address "$UNO_HOST:$JOURNALNODE_RPC_PORT"
fi
if [[ -n "$YARN_RESOURCEMANAGER_SCHEDULER_PORT" ]] && [[ $YARN_RESOURCEMANAGER_SCHEDULER_PORT != "8030" ]]; then
  setHadoopConf yarn-site.xml yarn.resourcemanager.scheduler.address "$UNO_HOST:$YARN_RESOURCEMANAGER_SCHEDULER_PORT"
fi
if [[ -n "$YARN_RESOURCEMANAGER_TRACKER_PORT" ]] && [[ $YARN_RESOURCEMANAGER_TRACKER_PORT != "8031" ]]; then
  setHadoopConf yarn-site.xml yarn.resourcemanager.resource-tracker.address "$UNO_HOST:$YARN_RESOURCEMANAGER_TRACKER_PORT"
fi
if [[ -n "$YARN_RESOURCEMANAGER_PORT" ]] && [[ $YARN_RESOURCEMANAGER_PORT != "8032" ]]; then
  setHadoopConf yarn-site.xml yarn.resourcemanager.address "$UNO_HOST:$YARN_RESOURCEMANAGER_PORT"
fi
if [[ -n "$MAPRED_JOBHISTORY_PORT" ]] && [[ $MAPRED_JOBHISTORY_PORT != "10020" ]]; then
  setHadoopConf mapred-site.xml mapreduce.jobhistory.address "$UNO_HOST:$MAPRED_JOBHISTORY_PORT"
fi

# make everything in hdfs world-writable for easier development
setHadoopConf hdfs-site.xml dfs.permissions.enabled false

echo "Setting host to $UNO_HOST"
grep -rl REPLACE_HOST "$UNO_HOME"/install/ | xargs sed -i "s/REPLACE_HOST/$UNO_HOST/g"

# shellcheck disable=SC1090
source <("$UNO_HOME"/bin/uno env)
if ! "$UNO_HOME"/bin/uno "$UNO_COMMAND" "$UNO_SERVICE"; then
  cat "$UNO_HOME"/install/logs/setup/*
  exit 1
fi

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
