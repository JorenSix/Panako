require 'rubygems'
require 'json'
require 'uri'
require 'net/http'
require 'fileutils'

class Jamendo

  def download_audio_files(json_file)  	
  	file_array = JSON.parse File.read(json_file) 
  	FileUtils.mkdir("music") unless File.exists?("music")
  	file_array.each do |file|
  	  id = file["id"]
	    #downloads better quality mp3
  	  url = file["audiodownload"].gsub("mp31","mp32")
  	  output_file = "music/#{id}.mp3"
	    command = "wget \"#{url}\" -O \"#{output_file}\""
	    File.open("music/#{id}.txt",'w'){|f| f.puts(file.to_json)} unless File.exists?("music/#{id}.txt")
      if File.exists?(output_file)
	      puts "Skipping: #{output_file}"
      else
        system(command)
      end
    end
  end
end

j = Jamendo.new
j.download_audio_files("all_meta_data.json")

