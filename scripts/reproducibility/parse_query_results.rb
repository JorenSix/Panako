require '../query_result_helpers/result_line'

results_files = ARGV

def parse(line)
  

  r = ResultLine.new(line)
  puts line
  query_file = File.basename(r.query)

  query_file_name,ref_start_expected,ref_stop_expected,query_mod,query_mod_factor = "","","","",""

  if query_file =~ /(.*)_(\d*)s-(\d*)s___(.*?).(mp3|gsm)/
    query_file_name,ref_start_expected,ref_stop_expected,query_mod = $1,$2,$3,$4
  elsif query_file =~ /(.*)_(\d*)s-(\d*)s.(mp3|gsm)/
    #no modification
    query_file_name,ref_start_expected, ref_stop_expected = $1,$2,$3
  end

  if  query_mod =~ /(speed_up)_(.*)/ or 
      query_mod =~ /(pitch_shift)_(.*)_cents/ or 
      query_mod =~ /(time_stretched)_(.*)/
    query_mod,query_mod_factor = $1, $2
  end

  ref_file_name = File.basename(r.ref).strip.gsub(".mp3","")
  correct = ref_file_name == query_file_name ? 1 : 0

  puts ref_file_name , query_file_name

  #return the usual match info
  #with some extra info extracted from the structured file name:
  #   ref_start_expected in seconds
  #   ref_stop_expected in seconds
  #   query_mod: type of modification
  #   query_mod_factor: the modification parameter (cents, percentage,...)
  #   correct: wheter the correct file was matched or not
  [r, ref_start_expected, ref_stop_expected, query_mod, query_mod_factor, correct]
end

def print_results(results_file)
  modification_hash = Hash.new

  File.read(results_file).split("\n").each do |line|
    next unless ResultLine.valid?(line)
    r, ref_start_expected, ref_stop_expected, query_mod, query_mod_factor, correct = parse(line)

    #Clean up the name
    query_mod = "Reference" if (query_mod == "")
    query_mod = "Band-passed" if (query_mod == "band_passed_2000Hz")
    query_mod = "Echo" if (query_mod == "echo")
    query_mod = "Chorus" if (query_mod == "chorus")
    query_mod = "Flanger" if (query_mod == "flanger")
    query_mod = "Tremolo" if (query_mod == "tremolo")

    query_mod_factor = query_mod_factor.to_f
    correct = correct.to_i

    unless modification_hash.key? query_mod
      modification_hash[query_mod] = Hash.new
    end
    unless modification_hash[query_mod].key? query_mod_factor
      modification_hash[query_mod][query_mod_factor] = [0,0,0]
    end

    modification_hash[query_mod][query_mod_factor][0] = modification_hash[query_mod][query_mod_factor][0] + correct
    modification_hash[query_mod][query_mod_factor][1] = modification_hash[query_mod][query_mod_factor][1] + 0
    modification_hash[query_mod][query_mod_factor][2] = modification_hash[query_mod][query_mod_factor][2] + 0
  end

  #add the reference value
  modification_hash["pitch_shift"][0] = modification_hash["Reference"][0.0]
  modification_hash["speed_up"][1.0] = modification_hash["Reference"][0.0]
  modification_hash["time_stretched"][1.0] = modification_hash["Reference"][0.0]

  puts modification_hash
  modification_hash.each do |query_mod,sub_hash|
    sub_hash.keys.sort.each do |query_mod_factor|
      results = modification_hash[query_mod][query_mod_factor]
      puts "#{query_mod};#{query_mod_factor};#{results.join(";")}"
    end
  end
end

results_files.each do |result_file|
  print_results(result_file)
end
