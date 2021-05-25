class ResultLine
  #query info
  attr_reader :query, :query_start, :query_stop
  #ref info
  attr_reader :ref,   :ref_start  , :ref_stop
  #match info
  attr_reader :match_score , :time_factor, :frequency_factor, :empty_seconds

  def self.valid?(line)
    line.split(";").size == 13
  end

  def initialize(line)
    data = line.split(";").map(&:strip)

    @query_index  = data[0].to_i
    @total_queries  = data[1].to_i

    @query  = data[2]
    @query_start = data[3].to_f
    @query_stop = data[4].to_f

    @ref  = data[5]
    @ref_id  = data[6].to_i
    @ref_start  = data[7].to_f
    @ref_stop  = data[8].to_f

    @match_score = data[9].to_i
    @time_factor  = data[10].to_f
    @frequency_factor  = data[11].to_f
    @empty_seconds  = data[12].to_f
  end

  def to_s
    [
    @query_index ,
    @total_queries,

    @query,
    @query_start,
    @query_stop,

    @ref,
    @ref_id,
    @ref_start,
    @ref_stop,

    @match_score,
    @time_factor,
    @frequency_factor,
    @empty_seconds
    ].join(";")
  end

  def ordered_key
    [@query,@ref].sort()
  end

  def identity_match?
    @query == @ref 
  end

  def acceptable?
    match_duration = @query_stop - @query_start

    ! identity_match? and 
    @match_score >= MATCH_SCORE_MIN and 
    match_duration >= MIN_DURATION and 
    match_duration <= MAX_DURATION and
    @match_score / match_duration > MIN_MATCHES_PER_SECOND and
    @empty_seconds <= MAX_EMPTY_SECONDS
  end

  def store_and_play_files(duration_in_seconds)
    ref_basename = File.basename(@ref,File.extname(@ref))
    q_basename = File.basename(@query,File.extname(@query))

    ref_name = "#{ref_basename}_#{q_basename}_ref#{File.extname(@ref)}"
    system "ffmpeg -hide_banner -loglevel panic -y -ss #{@ref_start} -t #{duration_in_seconds} -i '#{@ref}' '#{ref_name}'"
    
    query_name = "#{ref_basename}_#{q_basename}_query#{File.extname(@ref)}"
    system "ffmpeg -hide_banner -loglevel panic -y -ss #{@query_start} -t #{duration_in_seconds} -i '#{@query}' '#{query_name}' "

    system "ffplay '#{query_name}'"
    system "ffplay '#{ref_name}'"
  end
end
