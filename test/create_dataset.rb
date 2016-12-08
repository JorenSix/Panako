require 'rubygems'
require 'json'
require 'uri'
require 'net/http'
require 'fileutils'


def file_length(file)
  $1.to_i if `sox "#{file}" -n stat 2>&1` =~ /.*Length .*:\s*(\d*)\..*/
end

def seconds_to_samples(seconds)
  '%ds' % (seconds * 44100)
end


def cut_piece(input_file,target_file,start,piece_length)
  puts input_file
  command = "sox \"#{input_file}\" \"#{target_file}\" trim #{seconds_to_samples(start)} #{seconds_to_samples(piece_length)}"
  puts command
  system(command)
  start
end

def cut_random_piece(input_file,target_file,piece_length)
  puts input_file
  start =  rand(file_length(input_file) - piece_length) #in seconds
  command = "sox \"#{input_file}\" \"#{target_file}\" trim #{seconds_to_samples(start)} #{seconds_to_samples(piece_length)}"
  puts command
  system(command)
  start
end

def compressor(input_file,target_file)
  command = "sox \"#{input_file}\" \"#{target_file}\" compand 0.3,1 -90,-90,-70,-70,-60,-20,0,0 -10 0 0.2"
  puts command
  system(command) 
end


def download_audio_files(id_file,files) 	
  ids = File.read(id_file).split("\n").shuffle[0..files]
  ids.each do |id|
    url = "http://storage-new.newjamendo.com/download/track/#{id}/mp31/?from=app-00523d33"
	  #url = url.gsub("mp31","mp32") #downloads better quality mp3
	  output_file = "#{id}.mp3"
    output_wav_file = "#{id}.wav"
    command = "wget \"#{url}\" -O \"#{output_file}\""
    if File.exists?(output_wav_file)
      puts "Skipping: #{output_file}"
    else
      system(command)
      system("ffmpeg -i #{output_file} -ac 1 -ar 44100  -acodec  pcm_s16le #{output_wav_file}")
    end
  end
end


query_folder = 'be/panako/tests/res/query'
dataset_folder = 'be/panako/tests/res/dataset'

Dir.mkdir(query_folder) unless File.exists? query_folder
Dir.mkdir(dataset_folder) unless File.exists? dataset_folder
files = 5
download_audio_files("jamendo_track_ids.csv",files)
Dir.glob("*.wav") do |input_file|
  start = cut_random_piece(input_file,'temp.wav',15)
  compressor('temp.wav',File.join(query_folder,start.to_s + '_' + input_file))
  system("rm temp.wav")
  system("mv #{input_file} #{File.join(dataset_folder,input_file)}")
end

system("rm *.mp3")