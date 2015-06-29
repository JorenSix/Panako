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



package be.panako.strategy.ifft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.panako.cli.Panako;
import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.ifft.storage.IFFTFingerprintQueryMatch;
import be.panako.strategy.ifft.storage.IFFTMapDBStorage;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class IFFTStrategy extends Strategy {
	
	private final static Logger LOG = Logger.getLogger(IFFTStrategy.class.getName());
	
	private final IFFTMapDBStorage storage;
	
	public IFFTStrategy(){
		storage = IFFTMapDBStorage.getInstance();
	}
	
	@Override
	public double store(String resource, String description) {
		int samplerate = Config.getInt(Key.IFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.IFFT_SIZE);
		int overlap = size - Config.getInt(Key.IFFT_STEP_SIZE);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap);
		final IFFTEventPointProcessor minMaxProcessor = new IFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		Set<IFFTFingerprint> fingerprints = new HashSet<IFFTFingerprint>(minMaxProcessor.getFingerprints());
		
		int identifier = FileUtils.getIdentifier(resource);
		
		for(IFFTFingerprint fingerprint: fingerprints){
			storage.addFingerprint(identifier, fingerprint.t1, fingerprint.hash());
		}
		
		// Store the meta data.
		storage.addAudio(identifier, description);
		
		// Commit the changes to store the fingerprints
		double durationInSeconds = d.secondsProcessed();
		storage.audioObjectAdded((int) Math.round(durationInSeconds));
		
		LOG.info(String.format("Stored %d fingerprints bundeled from %d event points for %s.",fingerprints.size(),minMaxProcessor.getEventPoints().size(),resource));
		return durationInSeconds;
	}

	@Override
	public void query(String query, int maxNumberOfResults,
			QueryResultHandler handler) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		final IFFTEventPointProcessor minMaxProcessor = new IFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		List<IFFTFingerprint> fingerprints = new ArrayList<IFFTFingerprint>(minMaxProcessor.getFingerprints());
		
		final List<IFFTFingerprintQueryMatch> queryMatches = new ArrayList<IFFTFingerprintQueryMatch>();
		
		queryMatches.addAll(storage.getMatches(fingerprints, maxNumberOfResults));
		
		double queryDuration = d.secondsProcessed();
		
		if(queryMatches.isEmpty()){
			QueryResult result = QueryResult.emptyQueryResult(0,queryDuration);
			handler.handleEmptyResult(result);
		}else{
			for(IFFTFingerprintQueryMatch match : queryMatches){
				String description = storage.getAudioDescription(match.identifier);
				handler.handleQueryResult(new QueryResult(0,queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
			}
		}
	}

	@Override
	public void monitor(String query,final  int maxNumberOfResults,
			final QueryResultHandler handler) {
		
		int samplerate = Config.getInt(Key.IFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.MONITOR_STEP_SIZE) * samplerate;
		int overlap = Config.getInt(Key.MONITOR_OVERLAP) * samplerate;
		AudioDispatcher d ;
		if (query.equals(Panako.DEFAULT_MICROPHONE)){
			try {
				d = AudioDispatcherFactory.fromDefaultMicrophone(samplerate,size, overlap);
			} catch (LineUnavailableException e) {
				LOG.warning("Could not connect to default microphone!" + e.getMessage());
				e.printStackTrace();
				d = null;
			}
		}else{
			d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		}
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				double timeStamp = audioEvent.getTimeStamp() - Config.getInt(Key.MONITOR_OVERLAP);
				processMonitorQuery(audioEvent.getFloatBuffer().clone(), maxNumberOfResults, handler,timeStamp);
				return true;
			}
			
			@Override
			public void processingFinished() {
			}
		});
		d.run();
	}
	
	private void processMonitorQuery(float[] audioBuffer,int maxNumberOfResults,
			QueryResultHandler handler,double queryOffset){
		int samplerate = Config.getInt(Key.IFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.IFFT_SIZE);
		int overlap = size - Config.getInt(Key.IFFT_STEP_SIZE);
		
		AudioDispatcher d;
		try {
			d = AudioDispatcherFactory.fromFloatArray(audioBuffer, samplerate, size, overlap);
			final IFFTEventPointProcessor minMaxProcessor = new IFFTEventPointProcessor(size,overlap,samplerate);
			d.addAudioProcessor(minMaxProcessor);
			d.run();
			List<IFFTFingerprint> fingerprints = new ArrayList<IFFTFingerprint>(minMaxProcessor.getFingerprints());
			
			final List<IFFTFingerprintQueryMatch> queryMatches = new ArrayList<IFFTFingerprintQueryMatch>();
			
			queryMatches.addAll(storage.getMatches(fingerprints, maxNumberOfResults));
			
			double queryDuration = d.secondsProcessed();
			
			if(queryMatches.isEmpty()){
				QueryResult result = QueryResult.emptyQueryResult(queryOffset,queryOffset+queryDuration);
				handler.handleEmptyResult(result);
			}else{
				for(IFFTFingerprintQueryMatch match : queryMatches){
					String description = storage.getAudioDescription(match.identifier);
					handler.handleQueryResult(new QueryResult(queryOffset,queryOffset+queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
				}
			}
			
		} catch (UnsupportedAudioFileException e) {
			LOG.severe("Unsupported audio");
		}
		
	}

	@Override
	public boolean hasResource(String resource) {
		return storage.hasDescription(new File(resource).getName());
	}

	@Override
	public boolean isStorageAvailable() {
		return IFFTMapDBStorage.getInstance() != null;
	}
	

	@Override
	public void printStorageStatistics() {
		System.out.println("Number of audio objects: " + storage.getNumberOfAudioObjects());
		System.out.println("Number of fingerprints: " + storage.getNumberOfFingerprints());
		System.out.println("Number of seconds: " + storage.getNumberOfSeconds());
		System.out.println("Number of fingerprints/second: " + storage.getNumberOfFingerprints()/storage.getNumberOfSeconds());
	}

	
}
