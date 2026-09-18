#Defines a target
FROM debian:bullseye

#install some dependencies: java and ffmpeg
RUN apt-get update
RUN apt-get install -y --no-install-recommends git default-jdk ffmpeg

#To make sure the platform is supported we compile the
#dependent native code in the container
#On x86_64 this could be skipped
#install compilers
RUN apt-get install -y --no-install-recommends make gcc g++ libc-dev

#install lmdb library to a path java searches for libs (/lib)
WORKDIR /
RUN git clone --depth 1 https://git.openldap.org/openldap/openldap.git
WORKDIR /openldap/libraries/liblmdb
RUN make
RUN mv liblmdb.so /lib/

#Do the same for JGaborator
WORKDIR /
RUN git clone --depth 1 https://github.com/JorenSix/JGaborator
WORKDIR /JGaborator/gaborator/
ENV JAVA_HOME=/usr/lib/jvm/default-java
RUN make plain
RUN cp ../src/main/resources/jni/libjgaborator.so /lib/

#Now install Panako
WORKDIR /
COPY . /Panako
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
WORKDIR /Panako
RUN ./gradlew shadowJar
RUN ./gradlew install
RUN cp resources/defaults/panako /usr/local/bin/

#Cleanup: we do not need the temporary source files
# any more
RUN rm -rf /Panako
RUN rm -rf /JGaborator
RUN rm -rf /openldap
RUN apt-get clean

#Make and switch to the audio directory
# This directory will be mounted and used as passthrough
# from host to container
RUN mkdir -p /root/audio
WORKDIR /root/audio
