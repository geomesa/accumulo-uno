# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG JAVA_VERSION=17
FROM eclipse-temurin:${JAVA_VERSION}-jdk

# head commit as of 08/29/2025
ARG UNO_COMMIT=cdaccba54a7c7b130b1ece3610ebe08a480ad425
ARG ACCUMULO_VERSION=2.1.4
# note: hadoop 3.4 does not start up cleanly
ARG HADOOP_VERSION=3.3.6

ENV UNO_HOME=/opt/fluo-uno
ENV UNO_SERVICE=accumulo
ENV UNO_COMMAND=run
ENV ACCUMULO_VERSION=${ACCUMULO_VERSION}
ENV HADOOP_VERSION=${HADOOP_VERSION}
ENV ACCUMULO_USE_NATIVE_MAP=true
ENV HDFS_NAMENODE_USER=root
ENV HDFS_DATANODE_USER=root
ENV HDFS_SECONDARYNAMENODE_USER=root
ENV YARN_RESOURCEMANAGER_USER=root
ENV YARN_NODEMANAGER_USER=root

RUN apt-get update && apt-get install -y \
    openssh-server \
    unzip \
    perl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /root/.ssh && \
    chmod 0700 /root/.ssh && \
    ssh-keygen -t rsa -f /root/.ssh/id_rsa -N "" && \
    cp -v /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys && \
    chmod -v 0400 /root/.ssh/authorized_keys && \
    echo "Host *" > /root/.ssh/config && \
    echo "    StrictHostKeyChecking no" >> /root/.ssh/config && \
    chmod 644 /root/.ssh/config && \
    mkdir /run/sshd

# checkout a specific commit for repeatability, tip of main as of 2023-07-10
RUN cd "$UNO_HOME"/.. && \
    curl -L "https://github.com/apache/accumulo-fluo-uno/archive/$UNO_COMMIT.zip" -o accumulo-fluo-uno.zip && \
    unzip accumulo-fluo-uno.zip && \
    ln -s "accumulo-fluo-uno-$UNO_COMMIT" fluo-uno && \
    cd fluo-uno && \
    # this line removes the check for maven, which is only needed if you're building modules against source code
    sed -i '/hash mvn/{N;N;N;d}' bin/impl/commands.sh && \
    ./bin/uno fetch accumulo && \
    UNO_HOST=REPLACE_HOST ./bin/uno install accumulo && \
    ln -s "$UNO_HOME"/install/hadoop-* "$UNO_HOME"/install/hadoop && \
    ln -s "$UNO_HOME"/install/accumulo-* "$UNO_HOME"/install/accumulo && \
    ln -s "$UNO_HOME"/install/apache-zookeeper-* "$UNO_HOME"/install/zookeeper && \
    # clean up downloaded artifacts to reduce image size
    rm ../accumulo-fluo-uno.zip && \
    rm downloads/* && \
    # remove some extra hadoop files that don't seem to be needed but take up a lot of disk
    rm -r install/hadoop/share/doc/ && \
    rm -r install/hadoop/share/hadoop/tools/lib/*aliyun* && \
    rm -r install/hadoop/share/hadoop/tools/lib/*azure* && \
    rm -r install/hadoop/share/hadoop/tools/lib/*kafka* && \
    rm -r install/hadoop/share/hadoop/tools/lib/jdk.tools-*.jar

COPY --chmod=777 start.sh /opt/start.sh

ENTRYPOINT [ "/opt/start.sh" ]

WORKDIR $UNO_HOME
