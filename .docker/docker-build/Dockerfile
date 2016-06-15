FROM ubuntu:16.04

ARG USER_UID=1000
ARG USER_GID=1000

ADD bin/accept-apt-repository /usr/local/bin/
ADD bin/accept-oracle-licence /usr/local/bin/

RUN apt-get update -y -q                                                                         \
    && apt-get install -y -q wget curl expect git-core                                           \
    && apt-get install -y -q software-properties-common                                          \
    && accept-apt-repository add-apt-repository ppa:webupd8team/java && apt-get update -y -q     \
    && accept-oracle-licence apt-get install -y -q oracle-java7-installer oracle-java8-installer

ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

RUN groupadd --gid $USER_GID builder && useradd -m --uid $USER_UID --gid $USER_GID builder

RUN apt-get install -y -q nodejs nodejs-legacy npm
RUN npm install webpack -g

RUN chown builder:builder /opt
USER builder
WORKDIR /opt

RUN curl -fsSL https://dl.bintray.com/sbt/native-packages/sbt/0.13.11/sbt-0.13.11.tgz | tar -xzv

ENV SBT_HOME /opt/sbt
ENV PATH ${PATH}:${SBT_HOME}/bin

RUN echo "-sbt-boot /data/sbt/boot" >> /opt/sbt/conf/sbtopts
RUN echo "-ivy      /data/sbt/ivy2" >> /opt/sbt/conf/sbtopts
ADD conf/m2/settings.xml /home/builder/.m2/
