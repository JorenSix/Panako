#Creates a test data-set

require 'rubygems'
require 'json'
require 'uri'
require 'net/http'
require 'fileutils'

NUMBER_OF_FILES=5
QUERY_LENGTH_IN_SEC = 15
#Supply a random seed to make the procedure deterministic,
#for random dataset supply RANDOM_SEED = Kernel.srand 
RANDOM_SEED = 11

QUERY_FOLDER = File.join('dataset','queries')
DATASET_FOLDER = File.join('dataset','reference_items')
SPEED_UP = true

#seed the random number generator
Kernel.srand(RANDOM_SEED)

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

def speed_change(input_file,target_file,percentage)
  command = "sox \"#{input_file}\" \"#{target_file}\" speed #{percentage/100.0}"
  puts command
  system(command)
end


def download_audio_files(id_file,number_of_files,dataset_folder) 	
  ids = File.read(id_file).split("\n").shuffle[0..(number_of_files -1)]
  ids.each do |id|

    url = "http://storage-new.newjamendo.com/download/track/#{id}/mp31/?from=app-00523d33"
	  #url = url.gsub("mp31","mp32") #downloads better quality mp3
	  output_file = File.join(dataset_folder,"#{id}.mp3")
    output_wav_file = File.join(dataset_folder,"#{id}.wav")
    
    if File.exists?(output_wav_file)
      puts "Skipping download: #{output_file}"
    else
      download_command = "wget -nv --no-check-certificate \"#{url}\" -O \"#{output_file}\""
      system(download_command)
      system("ffmpeg  -hide_banner -loglevel error -i #{output_file} -ac 1 -ar 44100  -acodec  pcm_s16le #{output_wav_file}")
    end
  end
end

FileUtils.mkdir_p(QUERY_FOLDER) unless File.exists? QUERY_FOLDER
FileUtils.mkdir_p(DATASET_FOLDER) unless File.exists? DATASET_FOLDER
#Download the audio files
download_audio_files("jamendo_track_ids.csv",NUMBER_OF_FILES,DATASET_FOLDER)

dataset_files = Dir.glob(File.join(DATASET_FOLDER,"*.wav"))

speeds = Array.new(dataset_files.length)
speeds = speeds.map{93+rand(15)}

#Pick a random part from the downloaded files and modify it
#with a compressor or compressor + speedup
dataset_files.each_with_index do |input_file,index|
  start = cut_random_piece(input_file,'temp.wav',QUERY_LENGTH_IN_SEC)

  speed = SPEED_UP ? speeds[index] : 100

  output_file = File.join(QUERY_FOLDER,File.basename(input_file,".*") + '_' + start.to_s + '_' + speed.to_s + '.wav' )

  unless File.exists? output_file

    #modifications
    if speed == 100
      compressor('temp.wav',output_file)
    else
      compressor('temp.wav','compressed.wav')
      speed_change("compressed.wav", output_file,speed)
      system("rm compressed.wav")
    end
  end
  system("rm temp.wav")
end

system("rm #{File.join(DATASET_FOLDER,'*.mp3')}")