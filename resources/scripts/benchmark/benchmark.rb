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
RANDOM = Random.new(RANDOM_SEED)
BENCHMARK_AUDIO_FOLDER=ARGV[0]
CONFIGS_TO_BENCHMARK=["STRATEGY=OLAF AVAILABLE_PROCESSORS=0", "STRATEGY=PANAKO  AVAILABLE_PROCESSORS=0"]

srand(RANDOM_SEED)

all_audio_files = Dir.glob(File.join(BENCHMARK_AUDIO_FOLDER,"/**/*#{AUDIO_EXT}"))

def array_to_text_file(array)
	File.write("list_file.txt",array.join("\n"))
end

def audio_file_duration(audio_file)
    duration = `ffprobe -i '#{audio_file}' -v quiet -show_entries format=duration -hide_banner -of default=noprint_wrappers=1:nokey=1`
    #puts "Determined audio file duration for '#{audio_file}': #{duration}"
    duration.to_f
end

def audio_file_duration_from_array(array)
    duration_sum = 0
    array.each do |audio_file|
        duration_sum = duration_sum + audio_file_duration(audio_file)
    end
    duration_sum
end

CONFIGS_TO_BENCHMARK.each do |config|
  start_index = 0
  stop_index = 2
  total_stored_duration = 0
  puts
  puts config
  puts "Files (#), Audio (s), Fingerprints (#), Query speed (s/s) , Store speed (s/s)"

  ITERATIONS.times do |i|

    stored_audio_files = all_audio_files[start_index..(stop_index-1)]
    array_to_text_file(stored_audio_files)

    store_start = Time.now
    system "panako store #{config} list_file.txt > store_#{start_index}.csv"
    store_time = Time.now  - store_start

    query_files = all_audio_files.shuffle[0..(NUM_RANDOM_QUERIES-1)]
    array_to_text_file(query_files)

    query_start = Time.now
    system "panako query #{config} list_file.txt  > query_#{start_index}.csv"
    query_time = Time.now - query_start

    files_in_db = stop_index
    num_stored_files = stored_audio_files.size

    s = `panako stats #{config}`
    s  =~ /.* (\d+) fingerprint hashes.*/
    num_fps = $1

    stored_audio_duration = audio_file_duration_from_array(stored_audio_files)
    total_stored_duration = total_stored_duration + stored_audio_duration

    query_duration = audio_file_duration_from_array(query_files)
    query_speed = query_duration / query_time
    store_speed = stored_audio_duration  / store_time

    puts "#{files_in_db}, #{total_stored_duration.to_i}, #{num_fps}, #{"%.3f" % query_speed}, #{"%.3f" % store_speed}"

    start_index = stop_index
    stop_index = stop_index + stop_index
  end
end