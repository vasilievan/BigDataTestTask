#!/bin/bash
opt/kafka/bin/zookeeper-server-start.sh -daemon opt/kafka/config/zookeeper.properties &
opt/kafka/bin/kafka-server-start.sh -daemon opt/kafka/config/server.properties &
opt/kafka/bin/kafka-topics.sh --create --topic alerts --bootstrap-server localhost:9092
java -jar /home/BigDataTT/build/libs/app-1.0-SNAPSHOT.jar