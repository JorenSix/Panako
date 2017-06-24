/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
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




package be.panako.strategy.ncteq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.ncteq.storage.NCteQFingerprintQueryMatch;
import be.panako.strategy.ncteq.storage.NCteQMapDBStorage;
import be.panako.util.Config;
//import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.panako.util.PitchUnit;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class NCteQStrategy extends Strategy {
	
	private final static Logger LOG = Logger.getLogger(NCteQStrategy.class.getName());

	@Override
	public double store(String resource, String description) {
		
		ConstantQ constantQ = createConstantQ();
		NCteQMapDBStorage storage = NCteQMapDBStorage.getInstance();
		
		int sampleRate = Config.getInt(Key.NCTEQ_SAMPLE_RATE);
		int size = constantQ.getFFTlength();
		int overlap = size - Config.getInt(Key.NCTEQ_STEP_SIZE);
		NCteQEventPointProcessor eventPointProcessor = new NCteQEventPointProcessor(constantQ,sampleRate,Config.getInt(Key.NCTEQ_STEP_SIZE));
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, sampleRate, size , overlap);
		d.addAudioProcessor(eventPointProcessor);
		d.run();
		
		int identifier = FileUtils.getIdentifier(resource);
		int hashesAdded = 0;
		
		float bucketFillFactorSum = 0.0f;
		for(NCteQFingerprint fingerprint : eventPointProcessor.getFingerprints()){
			float fillFactor = storage.addFingerprint(identifier, fingerprint.t1, fingerprint.hash(),fingerprint.timeDelta(),fingerprint.f1);
			bucketFillFactorSum += fillFactor;
			if(fillFactor != 1.0){
				hashesAdded++;
			}
		}
		LOG.info(String.format("Average hash bucket fill factor for %d hashes %.2f %%", hashesAdded, 100 * bucketFillFactorSum / (float)hashesAdded ) );
		float secondsProcessed = d.secondsProcessed();
		
		storage.addAudio(identifier, description);
		storage.audioObjectAdded(hashesAdded, Math.round(secondsProcessed));
		
		return secondsProcessed;
	}
	
	
	private ConstantQ createConstantQ(){
		int binsPerOctave = Config.getInt(Key.NCTEQ_BINS_PER_OCTAVE);
		int sampleRate = Config.getInt(Key.NCTEQ_SAMPLE_RATE);
		int minFreqInCents = Config.getInt(Key.NCTEQ_MIN_FREQ);
		int maxFreqInCents = Config.getInt(Key.NCTEQ_MAX_FREQ);
		
		float minFreqInHerz = (float)  PitchUnit.HERTZ.convert(minFreqInCents,PitchUnit.ABSOLUTE_CENTS);
		float maxFreqInHertz = (float) PitchUnit.HERTZ.convert(maxFreqInCents,PitchUnit.ABSOLUTE_CENTS);
			
		return new ConstantQ(sampleRate, minFreqInHerz, maxFreqInHertz, binsPerOctave);
	}

	@Override
	public void query(String query, int maxNumberOfResults,Set<Integer> avoid,QueryResultHandler handler) {
		
		ConstantQ constantQ = createConstantQ();
		NCteQMapDBStorage storage = NCteQMapDBStorage.getInstance();
		
		int sampleRate = Config.getInt(Key.NCTEQ_SAMPLE_RATE);
		int size = constantQ.getFFTlength();
		int overlap = size - Config.getInt(Key.NCTEQ_STEP_SIZE);
		NCteQEventPointProcessor eventPointProcessor = new NCteQEventPointProcessor(constantQ,sampleRate,Config.getInt(Key.NCTEQ_STEP_SIZE));
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, sampleRate, size , overlap);
		d.addAudioProcessor(eventPointProcessor);
		d.run();
		
		List<NCteQFingerprint> fingerprints = new ArrayList<>();
		fingerprints.addAll(eventPointProcessor.getFingerprints());
		
		
		float queryDuration = d.secondsProcessed();
		List<NCteQFingerprintQueryMatch> matches = storage.getFingerprintMatches(fingerprints,maxNumberOfResults);
		
		if(matches.isEmpty()){
			QueryResult result = QueryResult.emptyQueryResult(0,queryDuration);
			handler.handleEmptyResult(result);
		}else{
			for(NCteQFingerprintQueryMatch match : matches){
				String description = storage.getAudioDescription(match.identifier);
				QueryResult result = new QueryResult(0,queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),match.timeRatio,match.frequencyRatio);
				handler.handleQueryResult(result);
			}
		}
	}

	@Override
	public void monitor(String query, final int maxNumberOfReqults,Set<Integer> avoid,
			final QueryResultHandler handler) {
		
		int samplerate = Config.getInt(Key.NCTEQ_SAMPLE_RATE);
		int size = Config.getInt(Key.MONITOR_STEP_SIZE) * samplerate;
		int overlap = Config.getInt(Key.MONITOR_OVERLAP) * samplerate;
		final ConstantQ constanQ = createConstantQ();
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				double timeStamp = audioEvent.getTimeStamp() - Config.getInt(Key.MONITOR_OVERLAP);
				processMonitorQuery(audioEvent.getFloatBuffer().clone(), maxNumberOfReqults, handler,timeStamp,constanQ);
				return true;
			}
			
			@Override
			public void processingFinished() {
			}
		});
		d.run();

	}
	
	private void processMonitorQuery(float[] audioBuffer,int maxNumberOfResults,
			QueryResultHandler handler,double queryOffset, ConstantQ constantQ){
		int samplerate = Config.getInt(Key.NCTEQ_SAMPLE_RATE);
		
		
		int size = constantQ.getFFTlength();
		int overlap = size - Config.getInt(Key.NCTEQ_STEP_SIZE);
		NCteQEventPointProcessor eventPointProcessor = new NCteQEventPointProcessor(constantQ,samplerate,Config.getInt(Key.NCTEQ_STEP_SIZE));
		
		AudioDispatcher d;

		try {
			d= AudioDispatcherFactory.fromFloatArray(audioBuffer, samplerate, size, overlap);
			d.addAudioProcessor(eventPointProcessor);
			d.run();
			
			
			List<NCteQFingerprint> fingerprints = new ArrayList<>();
			fingerprints.addAll(eventPointProcessor.getFingerprints());
			
			NCteQMapDBStorage storage = NCteQMapDBStorage.getInstance();
			float queryDuration = d.secondsProcessed();
			List<NCteQFingerprintQueryMatch> matches = storage.getFingerprintMatches(fingerprints,maxNumberOfResults);
			
			if(matches.isEmpty()){
				QueryResult result = QueryResult.emptyQueryResult(queryOffset,queryOffset+queryDuration);
				handler.handleEmptyResult(result);
			}else{
				for(NCteQFingerprintQueryMatch match : matches){
					String description = storage.getAudioDescription(match.identifier);
					QueryResult result = new QueryResult(queryOffset,queryOffset+queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),match.timeRatio,match.frequencyRatio);
					handler.handleQueryResult(result);
				}
			}
		} catch (UnsupportedAudioFileException e) {
			LOG.severe("Unsupported audio");
		}
	}
	

	@Override
	public boolean hasResource(String resource) {
		return NCteQMapDBStorage.getInstance().hasDescription(new File(resource).getName());
	}

	@Override
	public boolean isStorageAvailable() {
		return  NCteQMapDBStorage.getInstance()!=null;
	}

	@Override
	public void printStorageStatistics() {
		NCteQMapDBStorage storage = NCteQMapDBStorage.getInstance();
		System.out.println("Number of audio objects: " + storage.getNumberOfAudioObjects());
		System.out.println("Number of fingerprints: " + storage.getNumberOfFingerprints());
		System.out.println("Number of seconds: " + storage.getNumberOfSeconds());
		System.out.println("Number of fingerprints/second: " + storage.getNumberOfFingerprints()/storage.getNumberOfSeconds());
	}
	
	@Override
	public String resolve(String filename) {
		return "" + FileUtils.getIdentifier(filename);
	}

}
