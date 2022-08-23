import matplotlib.pyplot as plt                                                                                                                                                               
import numpy as np
import csv

seconds = []
query_speed = []
store_speed = []

with open('olaf_results.csv', mode ='r')as file: 
  # reading the CSV file
  csvFile = csv.reader(file)
  header = next(csvFile) 
  # displaying the contents of the CSV file
  for row in csvFile:
        seconds.append(float(row[1]))
        query_speed.append(float(row[3]))
        store_speed.append(float(row[4]))

plt.scatter(seconds,query_speed, label='Query speed')
plt.scatter(seconds,store_speed, label='Store speed')
plt.legend()
plt.title("Olaf query/store speed")
plt.xlabel("Index size (s)")
plt.ylabel("Processing time (s of audio proccessed/s)")
plt.xscale("log")
plt.savefig("olaf_benchmark_results.svg", format="svg")


seconds = []
query_speed = []
store_speed = []
with open('panako_results.csv', mode ='r')as file: 
  # reading the CSV file
  csvFile = csv.reader(file)
  header = next(csvFile) 
  # displaying the contents of the CSV file
  for row in csvFile:
        seconds.append(float(row[1]))
        query_speed.append(float(row[3]))
        store_speed.append(float(row[4]))

plt.figure().clear()
plt.close()
plt.cla()
plt.clf()

plt.scatter(seconds,query_speed, label='Query speed')
plt.scatter(seconds,store_speed, label='Store speed')
plt.legend()
plt.title("Panako query/store speed")
plt.xlabel("Index size (s)")
plt.ylabel("Processing time (s of audio proccessed/s)")
plt.xscale("log")
plt.savefig("panako_benchmark_results.svg", format="svg")