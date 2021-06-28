
set term png
set output output_file

set datafile separator ';'

set ylabel "Precentage" # label for the Y axis

set title t

set yrange [0:*]

set boxwidth 0.5
set style fill solid

plot data_file using 1:4:xtic(2) with boxes
