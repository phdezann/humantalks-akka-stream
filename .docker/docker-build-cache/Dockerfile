FROM ubuntu:16.04

ARG USER_UID=1000
ARG USER_GID=1000

RUN groupadd --gid $USER_GID builder && useradd -m --uid $USER_UID --gid $USER_GID builder

RUN mkdir /data && chown builder:builder /data
VOLUME /data

USER builder
