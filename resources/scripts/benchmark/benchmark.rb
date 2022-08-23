# A benchmark script to measure the computational time needed to index and query a growing index.
#
# The index starts of with 2 audio files and is doubled until it contains 16k files.
# Each time the index size is doubled
#     - The amount of time needed to double the index is measured.
#     - The amount of time needed to query the index is measured.
#
# The benchmark script works with the FMA Medium dataset which contains 25k 30s music fragments.
# To run the script, first download The FMA Medium dataset https://github.com/mdeff/fma/.

RANDOM_SEED=0
AUDIO_EXT = "mp3"
ITERATIONS = 14
NUM_RANDOM_QUERIES=10
AUDIO_FILE_DURATION=30.0 #seconds
RANDOM = Random.new(RANDOM_SEED)
FMA_MEDIUM_FOLDER="/downloaded/folder/for/fma_medium"
CONFIGS_TO_BENCHMARK=["STRATEGY=OLAF AVAILABLE_PROCESSORS=0", "STRATEGY=PANAKO  AVAILABLE_PROCESSORS=0"]


srand(RANDOM_SEED)

all_audio_files = Dir.glob(File.join(FMA_MEDIUM_FOLDER,"/**/*#{AUDIO_EXT}"))

def array_to_text_file(array)
	File.write("list_file.txt",array.join("\n"))
end


CONFIGS_TO_BENCHMARK.each do |config|
  start_index = 0
  stop_index = 2
  puts
  puts config
  puts "Files (#), Audio (s), Fingerprints (#), Query speed (s/s) , Store speed (s/s)"

  ITERATIONS.times do |i|

    array_to_text_file(all_audio_files[start_index..(stop_index-1)])

    store_start = Time.now
    system "panako store #{config} list_file.txt > store_#{start_index}.csv"
    store_time = Time.now  - store_start

    array_to_text_file(all_audio_files.shuffle[0..(NUM_RANDOM_QUERIES-1)])

    query_start = Time.now
    system "panako query #{config} list_file.txt  > query_#{start_index}.csv"
    query_time = Time.now - query_start

    files_in_db = stop_index
    num_stored_files = stop_index - start_index

    s = `panako stats #{config}`
    s  =~ /.* (\d+) fingerprint hashes.*/
    num_fps = $1

    query_speed = AUDIO_FILE_DURATION / (query_time / NUM_RANDOM_QUERIES.to_f )
    store_speed = AUDIO_FILE_DURATION / (store_time / num_stored_files.to_f )

    puts "#{files_in_db}, #{(AUDIO_FILE_DURATION * files_in_db).to_i}, #{num_fps}, #{"%.3f" % query_speed}, #{"%.3f" % store_speed}"

    start_index = stop_index
    stop_index = stop_index + stop_index
  end
end