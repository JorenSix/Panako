
# panako query list_of_mps3.txt > query_results.csv
# ruby results_filter.rb query_results.csv > filtered_results.csv

require_relative 'result_line'

match_result_file = ARGV[0]

#filter configuration
MIN_DURATION = 3
MAX_DURATION = 500000
MIN_MATCHES_PER_SECOND = 5
MAX_EMPTY_SECONDS = 0.7 #70% of the matching seconds are allowed to have no matching prints, more is not allowed
MATCH_SCORE_MIN = MIN_DURATION * MIN_MATCHES_PER_SECOND

all_results = []
content = File.read(match_result_file)
lines = content.split("\n")
lines.each do |l|
  all_results << ResultLine.new(l)
end

#tag symmetric matches
results_by_ordered_pair = Hash.new
all_results.each do |r|
  key = r.ordered_key
  results_by_ordered_pair[key] = Array.new unless results_by_ordered_pair.has_key? key
  results_by_ordered_pair[key] << r
end

results_by_ordered_pair.each do |key,result_list|
	symetric = result_list.size == 2	
  result_list.each do |r|
    r.set_symetric if symetric
  end
end

puts all_results.size.to_s
#filter results
filtered_results = []
all_results.each do |r|
  filtered_results << r if r.acceptable?
end

#order by score (highest first)
filtered_results = filtered_results.sort{|a,b| b.match_score <=> a.match_score}

filtered_results.each do |r|
	puts r.to_s
end
