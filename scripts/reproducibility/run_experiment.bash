#!/bin/bash

unzip all_meta_data.json.zip

# Download the data set, please only do this if
# you are going to use the files, spare the jamendo servers!
ruby download_dataset.rb 

# Find all downloaded mp3 files, put them in a file
find $PWD -name "*.mp3" > mp3_files.txt

# Store the reference audio
panako store mp3_files.txt

# Create query files (uses mp3_files.txt)
ruby create_queries.rb 20 40 60

# Run panako and store the results:
panako query queries_20s/*.mp3 | tee 20s_queries_results.csv
panako query queries_40s/*.mp3 | tee 40s_queries_results.csv
panako query queries_60s/*.mp3 | tee 60s_queries_results.csv

# Parse the query results
parse_query_results.rb 20s_queries_results.csv 40s_queries_results.csv 60s_queries_results.csv

# Analyze the results in the provided spreadsheet.