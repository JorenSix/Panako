

#remvoves al manifest info (signed jars etc) from dependencies:

Dir.glob("**/*.jar").each{|z| system "zip -d '#{z}' \"META-INF/*\""}
