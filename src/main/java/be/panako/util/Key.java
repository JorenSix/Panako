/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2022 - Joren Six / IPEM                             *
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
	MAX_FILE_SIZE(6000),
	
	/**
	 * The step size while monitoring a long audio fragment, in seconds.
	 */
	MONITOR_STEP_SIZE(25),

	/**
	 * The overlap, also in seconds. By default detection resolution is 
	 * 25-5=20 seconds.
	 */
	MONITOR_OVERLAP(5),
	
	/**
	 * Enabling the ffmpeg pipe allows support for almost all audio formats in the
	 * known universe. When disabled Panako supports 16bit mono WAV.
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
	DECODER_PIPE_BUFFER_SIZE(44100),
	
	/**
	 * The log file for the pipe decoder.
	 */
	DECODER_PIPE_LOG_FILE("decoder_log.txt"),

	/**
	 * By default ffprobe is used to determine the duration - in seconds -
	 * of an audio file.
	 *
	 * Alternatively any command which returns the duration in seconds can be used.
	 */
	AUDIO_DURATION_COMMAND("ffprobe -i \"%resource%\" -v quiet -show_entries format=duration -hide_banner -of default=noprint_wrappers=1:nokey=1"),
	
	
	/**
	 * The number of processors available to Panako. If zero (or less) all
	 * available processors are used.
	 */
	AVAILABLE_PROCESSORS("1"),
	
	/**
	 * The strategy (algorithm) to use, OLAF|PANAKO|PCH.
	 */
	STRATEGY("OLAF"),

	/**
	 * Maximum number of items returned for a query
	 * Normally only a handful of matches are expected
	 * So use a large number (more than 50) to return all results
	 * Use 1 if you only want the best ranked result
	 */
	NUMBER_OF_QUERY_RESULTS(1000),


	///////////////////PCH config

	/**
	 * Where to store the pitch class histograms
	 */
	PCH_FILES("~/.panako/dbs/pch"),
	/**
	 * The sample rate to use for the PCH algorithm
	 */
	PCH_SAMPLE_RATE(22050),
	/**
	 * Take audio buffers with a 1024 sample overlap
	 */
	PCH_OVERLAP(1024),
	/**
	 * Take audio buffers with a 2048 sample size
	 */
	PCH_SIZE(2048),


	///////////////////OLAF config

	/**
	 * The storage to use: MEM|LMDB
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
	OLAF_CACHE_FOLDER("~/.panako/dbs/olaf_cache"),
	/**
	 * Cache the fingerprint extraction process by storing them in a file.
	 */
	OLAF_CACHE_TO_FILE("TRUE"),
	/**
	 * Check if there are cached fingerprints and use them.
	 */
	OLAF_USE_CACHED_PRINTS("TRUE"),
	/**
	 * The sample rate to use for the Olaf algorithm.
	 * If audio with lower sample rate is used it is upsampled!
	 */
	OLAF_SAMPLE_RATE(16000),
	/**
	 * The size of an audio buffer
	 */
	OLAF_SIZE(1024),
	/**
	 * The amount of samples to step forward
	 */
	OLAF_STEP_SIZE(128),

	/**
	 * Matching step configuration: a limit to the unfiltered hits
	 */
	OLAF_MIN_HITS_UNFILTERED(10),
	/**
	 * Matching step configuration: a limit to the filtered hits
	 */
	OLAF_MIN_HITS_FILTERED(5),
	/**
	 * Matching step configuration: allow max 10% time stretching
	 */
	OLAF_MIN_TIME_FACTOR(0.9),
	/**
	 * Matching step configuration: allow max 10% time stretching
	 */
	OLAF_MAX_TIME_FACTOR(1.1),
	/**
	 * Matching step configuration: a match needs to
	 * have corresponding fingerprints between reference and
	 * index for 20% of the seconds.
	 */
	OLAF_MIN_SEC_WITH_MATCH(0.2),
	/**
	 * Matching step configuration: a match needs to have a duration of 5 seconds
	 */
	OLAF_MIN_MATCH_DURATION(5),

	/**
	 * Use an alternative histogram based matching strategy. This is advised for more noisy queries and
	 * when more random hits are expected (OLAF_EPS_PER_FP = 2). It can cause false positives.
	 */
	OLAF_MATCH_FALLBACK_TO_HIST("FALSE"),

	/**
	 * Event point extraction configuration: minimum frequency bin index
	 */
	OLAF_EP_MIN_FREQ_BIN(9),

	/**
	 * The number of event points to use for each fingerprint. Use 3 for 'clean' queries and
	 * large indexes. Use 2 for smaller indexes and more noisy queries (for example over the air
	 * queries).
	 */
	OLAF_EPS_PER_FP(3),

	/**
	 * Event point extraction configuration: a max filter of x cents is used vertically (frequency)
	 * The max filter is not linearly applied: it is following a log curve inspired by human hearing.
	 */
	OLAF_FREQ_MAX_FILTER_SIZE(20),
	/**
	 * Event point extraction configuration: a max filter of x bins is used horizontally (time)
	 */
	OLAF_TIME_MAX_FILTER_SIZE(25),
	/**
	 * Fingerprint construction: frequency bins of coupled event points
	 * need to be at least x apart
	 */
	OLAF_FP_MIN_FREQ_DIST(1),
	/**
	 * Fingerprint construction: frequency bins of coupled event points
	 * need to be at most x apart
	 */
	OLAF_FP_MAX_FREQ_DIST(128),
	/**
	 * Fingerprint construction: time indexes of coupled event points
	 * need to be at least x apart
	 */
	OLAF_FP_MIN_TIME_DIST(2),
	/**
	 * Fingerprint construction: time indexes of coupled event points
	 * need to be at most x apart
	 */
	OLAF_FP_MAX_TIME_DIST(33),

	/**
	 * Matching: allow hashes which are slightly off (+-2)
	 */
	OLAF_QUERY_RANGE(2),

	/**
	 * The list of hits is divided into a starting and ending part.
	 * The max length of this list is determined here.
	 *
	 * */
	OLAF_HIT_PART_MAX_SIZE(250),

	/**
	 * The list of hits is divided into a starting and ending part.
	 * The start and ending parts are, by default, one fifth of the total length.
	 * To use the full list divide by 2
	 * See: https://github.com/JorenSix/Panako/issues/36
	 * */
	OLAF_HIT_PART_DIVIDER(5),





	///////////////////PANAKO config


	/**
	 * Event point extraction: the size of the max filter vertically (freq)
	 */
	PANAKO_FREQ_MAX_FILTER_SIZE(103),
	/**
	 * Event point extraction: the size of the max filter horizontally (time)
	 */
	PANAKO_TIME_MAX_FILTER_SIZE(25),

	/**
	 * Fingerprint construction: min diff for frequency bins
	 */
	PANAKO_FP_MIN_FREQ_DIST(1),
	/**
	 * Fingerprint construction: max diff for frequency bins
	 */
	PANAKO_FP_MAX_FREQ_DIST(128),
	/**
	 * Fingerprint construction: min diff for time bins
	 */
	PANAKO_FP_MIN_TIME_DIST(2),
	/**
	 * Fingerprint construction: ax diff for time bins
	 */
	PANAKO_FP_MAX_TIME_DIST(33),
	
	//audio dispatcher config
	/**
	 * The block size for the audio processor (in samples)
	 */
	PANAKO_AUDIO_BLOCK_SIZE(8192),
	/**
	 * The overlap in the audio processor (in samples)
	 */
	PANAKO_AUDIO_BLOCK_OVERLAP(0),
	/**
	 * The audio sample frequency in Hz
	 */
	PANAKO_SAMPLE_RATE(16000),
	
	//Spectral tranform configuration
	/**
	 * The minimum frequency (Hz)
	 */
	PANAKO_TRANSF_MIN_FREQ(110),
	/**
	 * The maximum frequency (Hz). By default it is chosen to be 6
	 * octaves above 110Hz. Below the nyquist frequency of 16000Hz/2.
	 */
	PANAKO_TRANSF_MAX_FREQ(7040),//max frequency (Hz),
	/**
	 * The frequency to align the center of a frequency bin to (Hz).
	 */
	PANAKO_TRANSF_REF_FREQ(440),//reference frequency (Hz), determines bin bin centers
	/**
	 * The number of frequency bands to use for each octave.
	 * 6 octaves * 85 is 510 bands in total which is close to 512.
	 */
	PANAKO_TRANSF_BANDS_PER_OCTAVE(85),//bins for each octave
	/**
	 * The number of frequency bands to use for each octave
	 */
	PANAKO_TRANSF_TIME_RESOLUTION(128),//audio samples at 16kHz
	
	//query config
	/**
	 * Allow a small deviation of fingerprint hash hits of +-2
	 */
	PANAKO_QUERY_RANGE(2),

	/**
	 * Before filtering hits in the matching step, a true positive match should have at least this amount of hits
	 */
	PANAKO_MIN_HITS_UNFILTERED(10),

	/**
	 * While filtering the hit list is divided into a first and last part.
	 * See FIG 1 of https://archives.ismir.net/ismir2021/latebreaking/000039.pdf
	 * This List length is max this size for performance reasons.
	 * Set to Integer.MAX_VALUE and PANAKO_HIT_PART_DIVIDER to 2 to
	 * potentially increase retrieval rate with a performance cost.
	 */
	PANAKO_HIT_PART_MAX_SIZE(250),

	/**
	 * While filtering the hit list is divided into a first and last part.
	 * By default the first fifth and last fifth are taken into consideration.
	 * To use the full list, divide by 2.
	 * https://github.com/JorenSix/Panako/issues/36
	 */
	PANAKO_HIT_PART_DIVIDER(5),


	/**
	 * After filtering hits in the matching step, a true positive  match should have at least this amount of hits
	 */
	PANAKO_MIN_HITS_FILTERED(5),
	/**
	 * Matching config: This limits the
	 * time/frequency differences between query/reference audio to +-20%
	 */
	PANAKO_MIN_TIME_FACTOR(0.8),
	/**
	 * Matching config: This limits the
	 * time/frequency differences between query/reference audio to +-20%
	 */
	PANAKO_MAX_TIME_FACTOR(1.2),
	/**
	 * Matching config: This limits the
	 * time/frequency differences between query/reference audio to +-20%
	 */
	PANAKO_MIN_FREQ_FACTOR(0.8),
	/**
	 * Matching config: This limits the
	 * time/frequency differences between query/reference audio to +-20%
	 */
	PANAKO_MAX_FREQ_FACTOR(1.2),
	/**
	 * Matching config: This limits the
	 * time/frequency differences between query/reference audio to +-20%
	 */
	PANAKO_MIN_SEC_WITH_MATCH(0.2),
	/**
	 * Matching config: A match is only reported if it has a duration of more than 5 seconds.
	 */
	PANAKO_MIN_MATCH_DURATION(5),
	
	/**
	 * The storage to use: MEM|LMDB
	 * Stands for Memory, files on disk or the LMDB key-value store
	 */
	PANAKO_STORAGE("LMDB"),

	/**
	 * Folder to store the lmdb databese
	 */
	PANAKO_LMDB_FOLDER("~/.panako/dbs/panako_db"),
	/**
	 * Folder to store the cached fingerprints
	 */
	PANAKO_CACHE_FOLDER("~/.panako/dbs/panako_cache"),
	/**
	 * Cache fingerprints to a file
	 */
	PANAKO_CACHE_TO_FILE("TRUE"),
	/**
	 * Use the cached fingerprints to skip fingerprint extraction if possible
	 */
	PANAKO_USE_CACHED_PRINTS("TRUE"),
	/**
	 * Use the default (CPU based JGaborator) Event point extractor or use CUDA/MPS for event point
	 * extraction. The python server needs to be running if set to true.
	 */
	PANAKO_USE_GPU_EP_EXTRACTOR("FALSE");


    String defaultValue;

	/**
	 * Initialize a new key with default value as a string
	 * @param defaultValue The default value
	 */
	Key(String defaultValue){
		this.defaultValue = defaultValue;
	}

	/**
	 * Initialize a new key with default value as an integer
	 * @param defaultValue The default value
	 */
	Key(int defaultValue){
		this(String.valueOf(defaultValue));
	}

	/**
	 * Initialize a new key with default value as a double
	 * @param defaultValue The default value
	 */
	Key(double defaultValue){
		this(String.valueOf(defaultValue));
	}

	/**
	 * Get the default value for this configuration
	 * @return The default value
	 */
	public String getDefaultValue() {
		return defaultValue;
	}		
}
