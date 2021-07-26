FROM ubuntu

MAINTAINER ALEKSEY VASILEV <enthusiastic.programmer@yandex.ru>

RUN apt-get update

RUN apt-get install -y openjdk-16-jdk-headless

RUN apt-get install -y wget

RUN apt-get install -y tar

RUN apt-get install -y libpcap-dev

RUN apt-get install -y sqlite3

RUN cd /home

RUN wget https://apache-mirror.rbc.ru/pub/apache/kafka/2.8.0/kafka_2.13-2.8.0.tgz

RUN tar -xzf kafka_2.13-2.8.0.tgz

RUN rm -rf kafka_2.13-2.8.0.tgz

RUN cp -a kafka_2.13-2.8.0 /opt/kafka

RUN rm -rf kafka_2.13-2.8.0

ADD run.sh /home

ADD my.db /home

ADD BigDataTT /home/BigDataTT

RUN chmod +x /home/run.sh

ENTRYPOINT ["bash", "/home/run.sh"]