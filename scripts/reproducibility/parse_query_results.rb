require '../query_result_helpers/result_line'

results_files = %w(20s_queries_results.txt 40s_queries_results.txt 60s_queries_results.txt)

def is_same(one,other)
  one == other
end

def parse(line)
  r = ResultLine.new(line)
  query_file = r.query

  file_name,from,to,modification,factor,recoginzed_file_name,recognized_from,recognized_to,score,pitch_factor,time_factor = "","","","","","","","","","",""
  correct = "0"
  false_positive = "0"

  if query_file =~ /(.*)_(\d*)s-(\d*)s___(.*?).(mp3|gsm)/
    file_name,from,to,modification = $1,$2,$3,$4
  elsif query_file =~ /(.*)_(\d*)s-(\d*)s.(mp3|gsm)/
    file_name,from,to = $1,$2,$3
  end

  if modification =~ /(speed_up)_(.*)/
    modification,factor = $1,$2
  elsif modification =~ /(pitch_shift)_(.*)_cents/
    modification,factor = $1,$2
  elsif modification =~ /(time_stretched)_(.*)/
    modification,factor = $1,$2
  end

  if data.size > 1
    recoginzed_file_name = data[2].strip.gsub(".mp3","")
    recognized_from = data[3]
    recognized_to = recognized_from.to_f + (to.to_i  - from.to_i)
    score = data[4]
    pitch_factor,time_factor =  data[5], data[6]
    correct = "1" if is_same(recoginzed_file_name,file_name)
    false_positive = "1" if ! is_same(recoginzed_file_name,file_name)
  end

  [file_name,from,to,modification,factor,recoginzed_file_name,recognized_from,recognized_to,score,pitch_factor,time_factor,correct,false_positive]
end

#puts "file_name;from;to;modification;factor;recoginzed_file_name;recognized_from;recognized_to;score;pitch_factor;time_factor;correct;false_positive"
def print_results(results_file)
  modification_hash = Hash.new

  File.read(results_file).split("\n").each do |line|
    line = line.gsub("&gt;","")
    data = parse(line)
    modification = data[3]
    modification = "Reference" if (modification == "")
    modification = "Band-passed" if (modification == "band_passed_2000Hz")
    modification = "Echo" if (modification == "echo")
    modification = "Chorus" if (modification == "chorus")
    modification = "Flanger" if (modification == "flanger")
    modification = "Tremolo" if (modification == "tremolo")

    factor = data[4].to_f
    correct = data[11].to_i
    false_positive = data[12].to_i
    if (correct == 0 and  false_positive == 0)
      no_recognition = 1
    else
      no_recognition = 0
    end

    unless modification_hash.key? modification
      modification_hash[modification] = Hash.new
    end
    unless modification_hash[modification].key? factor
      modification_hash[modification][factor] = [0,0,0]
    end
    modification_hash[modification][factor][0] = modification_hash[modification][factor][0] + correct
    modification_hash[modification][factor][1] = modification_hash[modification][factor][1] + false_positive
    modification_hash[modification][factor][2] = modification_hash[modification][factor][2] + no_recognition
  end

  #add the reference value
  modification_hash["pitch_shift"][0] = modification_hash["Reference"][0.0]
  modification_hash["speed_up"][1.0] = modification_hash["Reference"][0.0]
  modification_hash["time_stretched"][1.0] = modification_hash["Reference"][0.0]
  modification_hash.sort.each do |modification,sub_hash|
    modification_hash[modification].keys.sort.each do |factor|
      results = modification_hash[modification][factor]
      puts "#{modification};#{factor};#{results.join(";")}"
    end
  end

  puts ""

  modification_hash.sort.each do |modification,sub_hash|
    if (modification == "pitch_shift" or modification == "time_stretched" or modification == "speed_up")
      puts ""
      modification_hash[modification].keys.sort.each do |factor|
        results = modification_hash[modification][factor]
        puts "#{factor}\t#{results.join("\t")}"
      end
    end
  end
end

results_files.each do |result_file|
  puts "\n\nparsing #{result_file}\n"
  print_results(result_file)
end
