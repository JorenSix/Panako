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
ruby create_queries.rb

# Run panako and store the results:
panako query queries_20s/*.mp3 > 20s_queries_results.txt
panako query queries_40s/*.mp3 > 40s_queries_results.txt
panako query queries_60s/*.mp3 > 60s_queries_results.txt

# Parse the query results
parse_query_results.rb

# Analyze the results in the provided spreadsheet.