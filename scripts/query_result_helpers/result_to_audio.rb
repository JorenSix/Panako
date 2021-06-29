# Play first the first matched seconds of a query and a result
# This can be used to check results quickly
# the only argument is a 'result line'

# For example:
#
# ruby result_to_audio.rb "3 ; 4090 ; /094/11 Track 11.m4a ; 61.104 ; 520.952 ; z02z31s75s.m4a ; 1662235492 ; 465.608 ; 957.864 ; 6 ; 0.93 % ; 1.00 %; 1.00"
#
# Notes:
# ffmpeg is used to select audio parts. ffplay is used to play audio: a cli tool available 
# in the ffmpeg suite. Install ffmpeg and ffplay or change the script to use tools which work
#  on your system (e.g. afplay on macOS)

require './result_line'

l = ARGV[0]

duration_in_seconds = 5

if l.split(';').size >= 10
  r = ResultLine.new(l)
  r.store_and_play_files(duration_in_seconds)
end
