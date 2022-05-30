#Defines a target
FROM debian:bullseye

#install some dependencies: java and ffmpeg
RUN apt-get update
RUN apt-get install -y --no-install-recommends git default-jdk ffmpeg

#To make sure the platform is supported we compile the
#dependent native code in the container
#On x86_64 this could be skipped
#install compilers
RUN apt-get install -y --no-install-recommends  build-essential

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
RUN make
RUN cp ../build/precompiled/libjgaborator.so /lib/

#Now install Panako
WORKDIR /
RUN git clone --depth 1 https://github.com/JorenSix/Panako.git
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
WORKDIR /Panako
RUN ./gradlew shadowJar
RUN ./gradlew install
RUN cp resources/defaults/panako /usr/local/bin/

#Test Panako
RUN apt-get install -y --no-install-recommends wget
RUN wget 'https://filesamples.com/samples/audio/mp3/sample3.mp3'
#CMD panako AVAILABLE_PROCESSORS=1  STRATEGY=PANAKO store sample3.mp3
