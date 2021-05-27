/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2021 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/

package be.panako.util;

/**
 * Defines which values can be configured and their respective
 * default values.
 * @author Joren Six
 */
public enum Key{
	/**
	 * Checks the data store if the file # is already added. Set this to
	 * false if a large number of unique files are added for a small
	 * performance gain.
	 */
	CHECK_DUPLICATE_FILE_NAMES("TRUE"), 
	
	/**
	 * The maximum file size (in MB) of files that are stored. Default is 6000 megabytes.
	 */
	MAX_FILE_SIZE("6000"),
	
	/**
	 * The step size while monitoring a long audio fragment, in seconds.
	 */
	MONITOR_STEP_SIZE("25"),

	/**
	 * The overlap, also in seconds. By default detection resolution is 
	 * 25-5=20 seconds.
	 */
	MONITOR_OVERLAP("5"),
	
	
	/**
	 * Enabling JLibAV allows support for almost all audio formats in the
	 * known universe. When disabled Panako supports Flac, MP3, and Vorbis,
	 * using buggy Java implementations Configuring libAV and enabling
	 * JLibAV is strongly advised (see readme.txt).
	 */
	DECODER("PIPE"),
	
	/**
	 * The pipe command environment
	 */
	DECODER_PIPE_ENVIRONMENT("/bin/bash"),
	/**
	 * The pipe command argument
	 */
	DECODER_PIPE_ENVIRONMENT_ARG("-c"),

	/**
	 * The command that streams PCM audio to a pipe
	 */
	DECODER_PIPE_COMMAND("ffmpeg -ss %input_seeking%  %number_of_seconds% -i \"%resource%\" -vn -ar %sample_rate% -ac %channels% -sample_fmt s16 -f s16le pipe:1"),

	/**
	 * The buffer used to cache the results from 
	 * the pipe. 44100 bytes is half a second.
	 */
	DECODER_PIPE_BUFFER_SIZE("44100"),
	
	/**
	 * The log file for the pipe decoder.
	 */
	DECODER_PIPE_LOG_FILE("decoder_log.txt"),
	
	
	/**
	 * The number of processors available to Panako. If zero (or less) all
	 * available processors are used.
	 */
	AVAILABLE_PROCESSORS("0"), 	
	
	/**
	 * The strategy (algorithm) to use, OLAF|PANAKO|PCH.
	 */
	STRATEGY("OLAF"),	
	
	NUMBER_OF_QUERY_RESULTS(1000), 
	
	
	PCH_FILES("dbs/pch"),
	PCH_SAMPLE_RATE(22050),
	PCH_OVERLAP(1024),
	PCH_SIZE(2048), 
	
	
	/**
	 * The storage to use: MEM|FILE|LMDB
	 * Stands for Memory, files on disk or the LMDB key-value store
	 */
	OLAF_STORAGE("LMDB"), 
	/**
	 * The folder to store the LMDB database
	 */
	OLAF_LMDB_FOLDER("~/.panako/dbs/olaf_db"), 
	/**
	 * File cache directory for bulk import
	 */
	OLAF_CACHE_FOLDER("~/.panako/cache"), 
	
	OLAF_SAMPLE_RATE("16000"),
	OLAF_SIZE("1024"), 
	OLAF_STEP_SIZE("128"), 
	
	OLAF_MIN_HITS_UNFILTERED("10"), 
	OLAF_MIN_HITS_FILTERED("5"), 
	OLAF_MIN_TIME_FACTOR("0.9"), 
	OLAF_MAX_TIME_FACTOR("1.1"),
	
	OLAF_FREQ_MAX_FILTER_SIZE(103), 
	OLAF_TIME_MAX_FILTER_SIZE(25), 
	OLAF_FP_MIN_FREQ_DIST(1), 
	OLAF_FP_MAX_FREQ_DIST(128), 
	OLAF_FP_MIN_TIME_DIST(2), 
	OLAF_FP_MAX_TIME_DIST(33),
	
	OLAF_CACHE_TO_FILE("FALSE"), 
	OLAF_QUERY_RANGE(1), 
	OLAF_USE_CACHED_PRINTS("FALSE"),
	
	
	//Event point filter settings
	PANAKO_FREQ_MAX_FILTER_SIZE(103),
	PANAKO_TIME_MAX_FILTER_SIZE(25),
	
	PANAKO_FP_MIN_FREQ_DIST(1), 
	PANAKO_FP_MAX_FREQ_DIST(128), 
	PANAKO_FP_MIN_TIME_DIST(2), 
	PANAKO_FP_MAX_TIME_DIST(33),
	
	//audio dispatcher config
	PANAKO_AUDIO_BLOCK_SIZE(8192),
	PANAKO_AUDIO_BLOCK_OVERLAP(0),
	PANAKO_SAMPLE_RATE(16000),
	
	//Spectral tranform configuration
	PANAKO_TRANSF_MIN_FREQ(110),//min frequency (Hz)
	PANAKO_TRANSF_MAX_FREQ(7040),//max frequency (Hz), 6 octaves above 110Hz
	PANAKO_TRANSF_REF_FREQ(440),//reference frequency (Hz), determines bin bin centers
	PANAKO_TRANSF_BANDS_PER_OCTAVE(85),//bins for each octave
	PANAKO_TRANSF_TIME_RESOLUTION(128),//audio samples at 16kHz
	
	//query config
	PANAKO_QUERY_RANGE(1),
	
	PANAKO_MIN_HITS_UNFILTERED("10"), 
	PANAKO_MIN_HITS_FILTERED("5"), 
	PANAKO_MIN_TIME_FACTOR("0.8"), 
	PANAKO_MAX_TIME_FACTOR("1.2"),
	PANAKO_MIN_FREQ_FACTOR("0.8"), 
	PANAKO_MAX_FREQ_FACTOR("1.2"), 
	
	PANAKO_LMDB_FOLDER("~/.panako/dbs/panako_db"),
	;
	
	
	String defaultValue;
	private Key(String defaultValue){
		this.defaultValue = defaultValue;
	}
	private Key(int defaultValue){
		this(String.valueOf(defaultValue));
	}
	private Key(float defaultValue){
		this(String.valueOf(defaultValue));
	}
	public String getDefaultValue() {
		return defaultValue;
	}		
}
