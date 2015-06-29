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



package be.panako.strategy.cteq;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.cteq.storage.CteQFingerprintQueryMatch;
import be.panako.strategy.cteq.storage.CteQMapDBStorage;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;


public class CteQStrategy extends Strategy {
	
	private final static Logger LOG = Logger.getLogger(CteQStrategy.class.getName());
	
	public double store(String resource, String description) {
		CteQFingerprintProcessor constantQ;
		int eventPointsPerSecondForStorage = Config.getInt(Key.CTEQ_EVENT_POINTS_PER_SECOND_FOR_STORAGE);
		int branchingFactor = Config.getInt(Key.CTEQ_EVENT_POINT_BRANCHING_FOR_STORAGE);
		float secondsProcessed = -1;
				
		constantQ = new CteQFingerprintProcessor(eventPointsPerSecondForStorage,branchingFactor);
		int size = constantQ.getFFTlength();
		int overlap = size - constantQ.getHopSize();

		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(resource, Config.getInt(Key.CTEQ_SAMPLE_RATE), size, overlap);
		adp.addAudioProcessor(constantQ);
		adp.run();
		int hashesAdded = 0;
		//store
		CteQMapDBStorage storage = CteQMapDBStorage.getInstance();
		
		int identifier = FileUtils.getIdentifier(resource);
		
		float bucketFillFactorSum = 0.0f;
		for(CteQFingerprint fingerprint : constantQ.getFingerprints()){
			float fillFactor = storage.addFingerprint(identifier, fingerprint.t1, fingerprint.hash(),fingerprint.timeDelta(),fingerprint.f1);
			bucketFillFactorSum += fillFactor;
			if(fillFactor != 1.0){
				hashesAdded++;
			}
		}
		LOG.info(String.format("Average hash bucket fill factor for %d hashes %.2f %%", hashesAdded, 100 * bucketFillFactorSum / (float)hashesAdded ) );
		secondsProcessed = adp.secondsProcessed();
		
		storage.addAudio(identifier, description);
		CteQMapDBStorage.getInstance().audioObjectAdded(hashesAdded, Math.round(secondsProcessed));

		return secondsProcessed;
	}


	public void query(String query, int maxNumberOfResults, QueryResultHandler handler){
		CteQFingerprintProcessor constantQ;
		final int eventPointsPerSecondForQuery = Config.getInt(Key.CTEQ_EVENT_POINTS_PER_SECOND_FOR_QUERY);
			
		int branchingFactor = Config.getInt(Key.CTEQ_EVENT_POINT_BRANCHING_FOR_QUERY);
		constantQ = new CteQFingerprintProcessor(eventPointsPerSecondForQuery,branchingFactor);
		int size = constantQ.getFFTlength();
		int overlap = size - constantQ.getHopSize();
		
		Set<CteQFingerprint>  fingerprints = new HashSet<CteQFingerprint>();
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(query, Config.getInt(Key.CTEQ_SAMPLE_RATE), size, overlap);
		adp.addAudioProcessor(constantQ);
		adp.run();
		float queryDuration = adp.secondsProcessed();
		fingerprints.addAll(constantQ.getFingerprints());
		
		
		CteQMapDBStorage storage = CteQMapDBStorage.getInstance();
		List<CteQFingerprintQueryMatch> matches = storage.getFingerprintMatches(new ArrayList<CteQFingerprint>(fingerprints),maxNumberOfResults);
		
		if(matches.isEmpty()){
			QueryResult result = QueryResult.emptyQueryResult(0,queryDuration);
			handler.handleEmptyResult(result);
		}else{
			for(CteQFingerprintQueryMatch match : matches){
				String description = storage.getAudioDescription(match.identifier);
				QueryResult result = new QueryResult(0,queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),match.timeRatio,match.frequencyRatio);
				handler.handleQueryResult(result);
			}
		}
			

	}
	
	public boolean isStorageAvailable() {
		return  CteQMapDBStorage.getInstance()!=null;
	}


	@Override
	public boolean hasResource(String resource) {
		return CteQMapDBStorage.getInstance().hasDescription(new File(resource).getName());
	}


	@Override
	public void printStorageStatistics() {
		CteQMapDBStorage storage = CteQMapDBStorage.getInstance();
		System.out.println("Number of audio objects: " + storage.getNumberOfAudioObjects());
		System.out.println("Number of fingerprints: " + storage.getNumberOfFingerprints());
		System.out.println("Number of seconds: " + storage.getNumberOfSeconds());
		System.out.println("Number of fingerprints/second: " + storage.getNumberOfFingerprints()/storage.getNumberOfSeconds());
	}


	@Override
	public void monitor(String query, final int maxNumberOfResults,
			final QueryResultHandler handler) {
	
			final int sampleRate = Config.getInt(Key.CTEQ_SAMPLE_RATE);
			final int monitorStepInSeconds = Config.getInt(Key.MONITOR_STEP_SIZE);
			int monitorOverlapInSeconds = Config.getInt(Key.MONITOR_OVERLAP);
			final int eventPointsPerSecond = Config.getInt(Key.CTEQ_EVENT_POINTS_PER_SECOND_FOR_QUERY);
			final int branchingFactor = Config.getInt(Key.CTEQ_EVENT_POINT_BRANCHING_FOR_QUERY);
			
			int size = monitorStepInSeconds * sampleRate;
			int overlap = monitorOverlapInSeconds * sampleRate;
			
			AudioDispatcher adp = AudioDispatcherFactory.fromPipe(query, Config.getInt(Key.CTEQ_SAMPLE_RATE), size, overlap);
			adp.addAudioProcessor(new AudioProcessor() {
				CteQFingerprintProcessor constantQ = new CteQFingerprintProcessor(eventPointsPerSecond,branchingFactor);
				@Override
				public boolean process(AudioEvent audioEvent) {
					
					int size = constantQ.getFFTlength();
					int overlap = size - constantQ.getHopSize();
					
					AudioDispatcher subDispatcher;
					try {
						constantQ.clear();
						subDispatcher = AudioDispatcherFactory.fromFloatArray(audioEvent.getFloatBuffer(), sampleRate, size, overlap);
						subDispatcher.addAudioProcessor(constantQ);
						subDispatcher.run();
						
						
						CteQMapDBStorage storage = CteQMapDBStorage.getInstance();
						ArrayList<CteQFingerprint> fingerprints = new ArrayList<CteQFingerprint>(constantQ.getFingerprints());
						List<CteQFingerprintQueryMatch> matches = storage.getFingerprintMatches(fingerprints,maxNumberOfResults);
						
						double queryOffsetStart = audioEvent.getTimeStamp();
						if(queryOffsetStart != 0 ){
							queryOffsetStart -= Config.getInt(Key.MONITOR_OVERLAP);
						}
						double queryOffsetStop = queryOffsetStart + Config.getInt(Key.MONITOR_STEP_SIZE);
						
						if(matches.isEmpty()){
							QueryResult result = QueryResult.emptyQueryResult(queryOffsetStart,queryOffsetStop);
							handler.handleEmptyResult(result);
						}else{
							for(CteQFingerprintQueryMatch match : matches){
								String description = storage.getAudioDescription(match.identifier);
								QueryResult result = new QueryResult(queryOffsetStart,queryOffsetStop,String.valueOf(match.identifier), description, match.score, match.getStartTime(),match.timeRatio,match.frequencyRatio);
								handler.handleQueryResult(result);
							}
						}
					} catch (UnsupportedAudioFileException e) {
						e.printStackTrace();
					}
					return true;
				}
				
				@Override
				public void processingFinished() {
				}
			});
			adp.run();
	
		
	}	
}
