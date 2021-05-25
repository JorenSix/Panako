#!/usr/bin/env ruby

# Panako is distrubuted as a "Fat JAR": it repackages dependent jars into
# a single larger jar-file. 
# 
# However, the jar-files Panako depend on cointain META-INF folders.
# These folder contains meta-data on the jar which is not relevant for the
# Panako fat jar and can even prevent it from working.
#
# This script removes meta-inf directories from jar files to make 
# the creation of a fat jar more streightforward.

Dir.glob("../lib/**/*.jar").each do |z|
	puts "Remove META-INF from: #{z}"
	system "zip -d '#{z}' \"META-INF/*\""
end
