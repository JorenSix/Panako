require 'threach'
require '../query_result_helpers/result_line'
require 'fileutils'
require './create_queries'
require './parse_query_results'


QUERY_LENGTHS = [10,20]
STORE_SPLIT = 80
RANDOM_SEED=0
AUDIO_EXT = "mp3"
NUM_QUERIES = 100
RANDOM = Random.new(RANDOM_SEED)
DATA_FOLDER = "eval_panako"
PARAMS_FILE = "panako_eval.properties"
OVERWRITE_RESULTS = false

config = File.read(PARAMS_FILE).split("\n").delete_if{|l| (l.strip == "" or l.strip[0] == "#")}.join(" ")
puts config

#Seed random number generator for reproducible evaluations
srand(RANDOM_SEED)

audio_archive_folder = ARGV[0]

# Make a list of all audio files
all_audio_files = Dir.glob(File.join(audio_archive_folder,"**/*#{AUDIO_EXT}")).sort.shuffle(random: RANDOM)

# Divide the list of all audio files in a list to store and a list to hold back
# Store in index to check true positives
# part not to store to checks true negatives
stop_index = all_audio_files.size * STORE_SPLIT / 100
true_positive_part = all_audio_files[0..stop_index]
true_negative_part = all_audio_files[(stop_index+1)..-1]

# Index the files that are used to check for true positives
File.write("true_positive_part.txt",true_positive_part.join("\n"));
system("panako #{config} store true_positive_part.txt")
FileUtils.rm("true_positive_part.txt")

# Create queries
def create_queries(target_dir_prefix,input_files)
  QUERY_LENGTHS.each do |query_length|
    target_dir = "#{target_dir_prefix}_#{query_length}s"
    FileUtils.mkdir_p(target_dir) unless File.exists?(target_dir)

    input_files.each_with_index do |f, index|
      puts "#{index+1}/#{input_files.size} #{File.basename(f)} #{query_length}s query length"
      cut_file = cut_random_piece_to_dir(f,target_dir,query_length)
      cut_file = File.join(target_dir,cut_file)
      create_panako_next_gen_queries([cut_file],target_dir,AUDIO_EXT)
    end
  end
end

# Create to sets of queries a set to check true positives and a set to check true negatives:
create_queries("#{DATA_FOLDER}/tp_queries",true_positive_part.shuffle(random:RANDOM)[1..NUM_QUERIES])
create_queries("#{DATA_FOLDER}/tn_queries",true_negative_part.shuffle(random:RANDOM)[1..NUM_QUERIES])

#create lists with queries:

QUERY_LENGTHS.each do |query_length|
  ["tp","tn"].each do |type|
    queries = Dir.glob(File.join("#{DATA_FOLDER}/#{type}_queries_#{query_length}s","*#{AUDIO_EXT}")).sort.shuffle(random: RANDOM)
    queries_list_file = "#{type}_queries_#{query_length}s_list.txt"

    File.write(queries_list_file ,queries.join("\n"))

    queries_results_file = "#{DATA_FOLDER}/#{type}_queries_#{query_length}s_results.csv"
    if(OVERWRITE_RESULTS or ! File.exists? queries_results_file)
      system("panako #{config} query #{queries_list_file} | tee #{queries_results_file}")
    end
    FileUtils.rm(queries_list_file)
  end
end

#parse results

FileUtils.mkdir_p("#{DATA_FOLDER}/results/") unless File.exists?("#{DATA_FOLDER}/results/")
QUERY_LENGTHS.each do |query_length|
  ["tp","tn"].each do |type|
    queries_results_file = "#{DATA_FOLDER}/#{type}_queries_#{query_length}s_results.csv"
    filename_prefix = "#{DATA_FOLDER}/results/#{type}_queries_#{query_length}s_"
    title_suffix="#{type} #{query_length}s"
    print_results(queries_results_file,filename_prefix,title_suffix,NUM_QUERIES,type)
  end
end

