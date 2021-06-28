set term png
set output output_file

set datafile separator ';'

set key autotitle columnhead # use the first line as title
set ylabel "Precentage" # label for the Y axis
set xlabel 'Modification' # label for the X axis

set yrange [0:*]

set xrange [0.84:1.16]

set title t

plot data_file using 2:3 with lines, '' using 1:3 with lines
