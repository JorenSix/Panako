/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2015 - Joren Six / IPEM                             *
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



package be.panako.strategy.fft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.fft.storage.FFTFingerprintQueryMatch;
import be.panako.strategy.fft.storage.FFTMapDBStorage;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class FFTStrategy extends Strategy{
	private final static Logger LOG = Logger.getLogger(FFTStrategy.class.getName());
	
	private final FFTMapDBStorage storage;
	
	public FFTStrategy(){
		storage = FFTMapDBStorage.getInstance();
	}
	
	public double store(String resource, final String description) {
		final int identifier = FileUtils.getIdentifier(resource);
		final double durationInSeconds;
		LOG.info("Store " + description + "...");
		if(storage.hasDescription(description)){
			LOG.warning("Skipping " + description + ", it is already present in the database");
			durationInSeconds = -1L;
		}else{
			final FFTFingerprintProcessor rhap = new FFTFingerprintProcessor(Config.getInt(Key.FFT_FINGERPRINTS_PER_SECOND_FOR_STORAGE),1);
		
			AudioDispatcher d = getAudioDispatcher(resource);
			d.addAudioProcessor(rhap);
			d.addAudioProcessor(new AudioProcessor() {
					@Override
					public boolean process(AudioEvent audioEvent) {
						for(FFTFingerprint fingerprint: rhap.getFingerprints()){
							storage.addFingerprint(identifier, fingerprint.t1, fingerprint.hash());
						}
						rhap.clear();
						return true;
					}
					@Override
					public void processingFinished() {
						// Store the meta data.
						storage.addAudio(identifier, description);
						// Commit the changes to store the fingerprints
						storage.audioObjectAdded((int) Math.round(rhap.getTimeStamp()));
					}
				});
			d.run();
			durationInSeconds = rhap.getTimeStamp();
		}
		return durationInSeconds;
	}
	
	public void query(String query, final int maxNumberOfResults, QueryResultHandler handler){
	
		final FFTMapDBStorage storage = FFTMapDBStorage.getInstance();
		
		final List<FFTFingerprintQueryMatch> queryMatches = new ArrayList<FFTFingerprintQueryMatch>();
	
		LOG.info("Query " + query + "...");
		
		final FFTFingerprintProcessor rhap = new FFTFingerprintProcessor(Config.getInt(Key.FFT_LANDMARKS_PER_SECOND_FOR_QUERY),4);

		AudioDispatcher d = getAudioDispatcher(query);
		d.addAudioProcessor(rhap);
		d.addAudioProcessor(new AudioProcessor() {
				@Override
				public boolean process(AudioEvent audioEvent) {
					return true;
				}
				@Override
				public void processingFinished() {
					List<FFTFingerprint> fingerprints = new ArrayList<FFTFingerprint>(rhap.getFingerprints());
					queryMatches.addAll(storage.getMatches(fingerprints, maxNumberOfResults));
				}
			});
		d.run();
		
		double queryDuration = d.secondsProcessed();
		
		if(queryMatches.isEmpty()){
			QueryResult result = QueryResult.emptyQueryResult(0,queryDuration);
			handler.handleEmptyResult(result);
		}else{
			for(FFTFingerprintQueryMatch match : queryMatches){
				String description = storage.getAudioDescription(match.identifier);
				handler.handleQueryResult(new QueryResult(0,queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
			}
		}
	}

	@Override
	public boolean isStorageAvailable() {
		return FFTMapDBStorage.getInstance() != null;
	}
	
	@Override
	public boolean hasResource(String resource) {
		return FFTMapDBStorage.getInstance().hasDescription(new File(resource).getName());
	}

	private AudioDispatcher getAudioDispatcher(String resource){
	
		int sampleRate = Config.getInt(Key.FFT_SAMPLE_RATE);
		
		// two minutes fits in memory comfortably and contains a lot of songs.
		int twoMinutesInSamples = (2*60) * sampleRate;
		
		//set the step size to five minutes  and overlap to zero
		// if the song is shorter than five minutes, use that as the step size
		return AudioDispatcherFactory.fromPipe(resource, sampleRate, twoMinutesInSamples, 0);
	}

	@Override
	public void printStorageStatistics() {
		FFTMapDBStorage storage = FFTMapDBStorage.getInstance();
		System.out.println("Number of audio objects: " + storage.getNumberOfAudioObjects());
		System.out.println("Number of fingerprints: " + storage.getNumberOfFingerprints());
		System.out.println("Number of seconds: " + storage.getNumberOfSeconds());
		System.out.println("Number of fingerprints/second: " + storage.getNumberOfFingerprints()/storage.getNumberOfSeconds());
	}

	@Override
	public void monitor(String query, final int maxNumberOfReqults,
			final QueryResultHandler handler) {
		final FFTMapDBStorage storage = FFTMapDBStorage.getInstance();
		AudioDispatcher d;
		
			int sampleRate = Config.getInt(Key.FFT_SAMPLE_RATE);			
		
			//every 20 seconds, execute a query and overlap with 5 seconds (resolution for detection = 10 seconds)
			final int stepSizeSeconds = Config.getInt(Key.MONITOR_STEP_SIZE);
			//final int maxScore = Config.getInt(Key.FFT_FINGERPRINTS_PER_SECOND_FOR_STORAGE ) * stepSizeSeconds;
			int stepSize = stepSizeSeconds * sampleRate;
			int overlap = Config.getInt(Key.MONITOR_OVERLAP) * sampleRate;
			
			final FFTFingerprintProcessor rhap = new FFTFingerprintProcessor(Config.getInt(Key.FFT_LANDMARKS_PER_SECOND_FOR_QUERY),4);
			
			d = AudioDispatcherFactory.fromPipe(query, sampleRate, stepSize,overlap);			
			
			d.addAudioProcessor(rhap);
			
			d.addAudioProcessor(new AudioProcessor() {
				@Override
				public boolean process(AudioEvent audioEvent) {
					List<FFTFingerprint> fingerprints = new ArrayList<FFTFingerprint>(rhap.getFingerprints());
					List<FFTFingerprintQueryMatch> queryMatches = storage.getMatches(fingerprints, maxNumberOfReqults);
					
					double queryOffsetStart = audioEvent.getTimeStamp();
					if(queryOffsetStart != 0 ){
						queryOffsetStart -= Config.getInt(Key.MONITOR_OVERLAP);
					}
					double queryOffsetStop = queryOffsetStart + Config.getInt(Key.MONITOR_STEP_SIZE);
					
					if(queryMatches.isEmpty()){
						QueryResult result = QueryResult.emptyQueryResult(queryOffsetStart,queryOffsetStop);
						handler.handleEmptyResult(result);
					}else{
						for(FFTFingerprintQueryMatch match : queryMatches){
							String description = storage.getAudioDescription(match.identifier);
							handler.handleQueryResult(new QueryResult(queryOffsetStart,queryOffsetStop,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
						}
					}
					
					queryMatches.addAll(storage.getMatches(fingerprints, maxNumberOfReqults));
					
					rhap.clear();
					rhap.samplesProcessed = 0;
					return true;
				}
				@Override
				public void processingFinished() {
					
				}
			});
			d.run();
	}	
	
	@Override
	public String resolve(String filename) {
		return "" + FileUtils.getIdentifier(filename);
	}
}
