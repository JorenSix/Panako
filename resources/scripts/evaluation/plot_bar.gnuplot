set style data histograms
set term png
set output output_file

set datafile separator ';'

set ylabel "Precentage" # label for the Y axis

set title t

set yrange [0:1.0]

set boxwidth 0.8
set style fill solid

plot data_file using 4:xtic(2)  title columnhead, "" using 5  title columnhead , "" using 6  title columnhead
