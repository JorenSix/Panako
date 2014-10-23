#!/bin/bash

#build a release
ant release

filename=$(basename Panako-*)
version=${filename:7:3}

ssh joren@panako.be rm -rf /var/www/be.panako/releases/Panako-latest
ssh joren@panako.be rm -rf /var/www/be.panako/releases/Panako-$version

scp -r Panako-$version joren@panako.be:/var/www/be.panako/releases

ssh joren@panako.be mkdir /var/www/be.panako/releases/Panako-latest
ssh joren@panako.be ln -s /var/www/be.panako/releases/Panako-$version/readme.html /var/www/be.panako/releases/Panako-latest/readme.html
ssh joren@panako.be ln -s /var/www/be.panako/releases/Panako-$version/Panako-$version-src.zip /var/www/be.panako/releases/Panako-latest/Panako-latest-src.zip
ssh joren@panako.be ln -s /var/www/be.panako/releases/Panako-$version/doc /var/www/be.panako/releases/Panako-latest/doc





