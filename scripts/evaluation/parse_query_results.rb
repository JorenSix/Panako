require '../query_result_helpers/result_line'
require 'fileutils'
require 'threach'

#esults_files = ARGV

class Query
  attr_reader :matches

  def initialize(query_file_name,mod,mod_param)
    @matches = Array.new
    @mod = mod
    @mod_param = mod_param.to_f
    @query_file_name = query_file_name

    prettify
  end

  def prettify
    #Clean up the name
    @mod = "Reference" if (@mod == "")
    @mod = "Band-passed" if (@mod == "band_passed_2000Hz")
    @mod = "Echo" if (@mod == "echo")
    @mod = "Chorus" if (@mod == "chorus")
    @mod = "Flanger" if (@mod == "flanger")
    @mod = "Tremolo" if (@mod == "tremolo")

    @mod = "Pitch shift" if (@mod == "pitch_shift")
    @mod = "Speed up" if (@mod == "speed_up")
    @mod = "Time stretch" if (@mod == "time_stretched")
  end

  def add_match(m)
    @matches << m
  end

  def key
    "#{@query_file_name}_#{@mod}_#{@mod_param}"
  end

  def first_match_correct
    return false unless has_matches?

    return match_correct(@matches.first)
  end

  def match_correct(m)
    ref = m.ref
    ref_basename = File.basename(ref,File.extname(ref))

    return ref_basename == @query_file_name
  end

  def has_matches?
    @matches.size != 0
  end

  def add_to_mod_hash(modification_hash)
    unless modification_hash.key? @mod
      modification_hash[@mod] = Hash.new
    end

    unless modification_hash[@mod].key? @mod_param
      modification_hash[@mod][@mod_param] = [0,0,0]
    end

    #tp
    modification_hash[@mod][@mod_param][0] = modification_hash[@mod][@mod_param][0] + 1 if first_match_correct
    #fp
    modification_hash[@mod][@mod_param][1] = modification_hash[@mod][@mod_param][1] + 1 if (has_matches? and !first_match_correct)
    #fn
    modification_hash[@mod][@mod_param][2] = modification_hash[@mod][@mod_param][2] + 1 if (!has_matches?)
  end

end

def populate_query_hash(line,query_hash) 

  r = ResultLine.new(line)

  query_file = File.basename(r.query)

  query_file_name,ref_start_expected,ref_stop_expected,query_mod,query_mod_factor = "","","","",""

  if query_file =~ /(.*)_(\d*)s-(\d*)s___(.*?).(mp3|gsm)/
    query_file_name,ref_start_expected,ref_stop_expected,query_mod = $1,$2,$3,$4
  elsif query_file =~ /(.*)_(\d*)s-(\d*)s.(mp3|gsm)/
    #no modification
    query_file_name,ref_start_expected, ref_stop_expected = $1,$2,$3
  end

  if  query_mod =~ /(speed_up)_(.*)/ or 
      query_mod =~ /(pitch_shift)_(.*)/ or 
      query_mod =~ /(time_stretched)_(.*)/
    query_mod,query_mod_factor = $1, $2
  end

  q = Query.new(query_file_name,query_mod,query_mod_factor)

  unless query_hash.has_key? q.key    
    query_hash[q.key] = q
  end

  unless r.ref == "null" 
    query_hash[q.key].add_match(r)
  end

  query_hash[q.key]
end

def print_results(results_file,file_name_prefix,title_suffix)

  query_hash = Hash.new

  tot_reference = 0
  File.read(results_file).split("\n").each do |line|
    next unless ResultLine.valid?(line)
    populate_query_hash(line,query_hash)
  end

  modification_hash = Hash.new
  query_hash.values.each do |q|
    q.add_to_mod_hash(modification_hash)
  end

  #add the reference value
  mods = ["Pitch shift","Speed up","Time stretch"]
  mods.each{|mod| modification_hash[mod][1.0] = modification_hash["Reference"][0.0]}

  mods.each do |mod|
    result_file_name = file_name_prefix+mod+".csv"
    File.open(result_file_name,"w") do |f|
      modification_hash[mod].keys.sort.each do |query_mod_factor|
        results = modification_hash[mod][query_mod_factor]
        f.puts "#{mod};#{query_mod_factor};#{results.join(";")}"
      end
    end
    modification_hash.delete(mod)
    title = mod + " " + title_suffix
    puts "Plotting #{title}: " + result_file_name
    output_file = result_file_name.gsub(".csv",".png")
    system("gnuplot -p -e \"data_file='#{result_file_name}';t='#{title}';output_file='#{output_file}'\" line_plot.gnuplot")
  end

  result_file_name = file_name_prefix+"other_mods.csv"
  File.open(result_file_name,"w") do |f|
    i = 0
    modification_hash.each do |query_mod,sub_hash|
      modification_hash[query_mod].keys.sort.each_with_index do |query_mod_factor|
        results = modification_hash[query_mod][query_mod_factor]
        f.puts "#{i+1};#{query_mod};#{query_mod_factor};#{results.join(";")}"
        i = i + 1
      end
    end
  end


  title = "Other mods " + title_suffix
  puts "Plotting #{title}: " + result_file_name
  output_file = result_file_name.gsub(".csv",".png")
  system("gnuplot -p -e \"data_file='#{result_file_name}';t='#{title}';output_file='#{output_file}'\" bar_plot.gnuplot")

  

end






