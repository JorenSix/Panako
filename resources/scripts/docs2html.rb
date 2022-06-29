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
	docs = rr("../../README.textile")
	html = template.gsub("__content__",docs)
	html = html.gsub("resources/media/panako_interactive_session.svg","media/panako_interactive_session.svg")
	html = html.gsub("resources/media/general_acoustic_fingerprinting_schema.svg","media/general_acoustic_fingerprinting_schema.svg")
	File.open("readme.html", 'w') {|f| f.write(html) }
end

render
