#!/usr/bin/env ruby

#Converts the readme textile file to a html file

require 'rubygems'
require 'RedCloth'

#read redcloth file
def rr(file)
	RedCloth.new(File.read(file)).to_html
end

def render
	template = File.read("docs2html_template.html")
	docs = rr("../README.textile")
	html = template.gsub("__content__",docs)
	File.open("../build/readme.html", 'w') {|f| f.write(html) }
end

render
