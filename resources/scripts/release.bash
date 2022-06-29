#!/bin/bash

#generate new html readme file
./docs2html.rb

#build and deploy a release
cd ../../
./gradlew shadowJar #Builds the core Panako library
./gradlew javadoc #Builds the docs

filename=$(basename build/libs/Panako-*)
version=${filename:7:3}

ssh joren@panako.be rm -rf /var/www/be.panako/releases/Panako-latest
ssh joren@panako.be rm -rf /var/www/be.panako/releases/Panako-$version

ssh joren@panako.be mkdir -p /var/www/be.panako/releases/Panako-$version/media

scp  build/libs/Panako-* joren@panako.be:/var/www/be.panako/releases/Panako-$version/
scp  resources/scripts/readme.html joren@panako.be:/var/www/be.panako/releases/Panako-$version/
scp  resources/media/*svg joren@panako.be:/var/www/be.panako/releases/Panako-$version/media
scp -r build/docs/javadoc joren@panako.be:/var/www/be.panako/releases/Panako-$version/

#link latest
ssh joren@panako.be mkdir /var/www/be.panako/releases/Panako-latest
ssh joren@panako.be ln -s /var/www/be.panako/releases/Panako-$version/readme.html /var/www/be.panako/releases/Panako-latest/readme.html
ssh joren@panako.be ln -s /var/www/be.panako/releases/Panako-$version/doc /var/www/be.panako/releases/Panako-latest/doc
