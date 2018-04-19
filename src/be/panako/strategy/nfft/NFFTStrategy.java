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




package be.panako.strategy.nfft;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;



import be.panako.cli.Panako;
import be.panako.http.PanakoWebserviceClient;
import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.SerializedFingerprintsHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.storage.NFFTFingerprintHit;
import be.panako.strategy.nfft.storage.NFFTFingerprintQueryMatch;
import be.panako.strategy.nfft.storage.NFFTMapDBStorage;
import be.panako.strategy.nfft.storage.Storage;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class NFFTStrategy extends Strategy {
	
	private final static Logger LOG = Logger.getLogger(NFFTStrategy.class.getName());
	
	private final Storage storage;
	
	public NFFTStrategy(){
		storage = NFFTMapDBStorage.getInstance();
	}
	
	@Override
	public double store(String resource, String description) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap);
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		Set<NFFTFingerprint> fingerprints = new HashSet<NFFTFingerprint>(minMaxProcessor.getFingerprints());
		
		int identifier = FileUtils.getIdentifier(resource);
		
		
		for(NFFTFingerprint fingerprint: fingerprints){
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
	
	public String getAudioDescription(int identifier){
		return storage.getAudioDescription(identifier);
	}
	
	public List<NFFTFingerprint> extractFingerprintsFromQuery(String query){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		List<NFFTFingerprint> fingerprints = new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
		return fingerprints;
	}

	@Override
	public void query(String query, Set<Integer> avoid,
                      QueryResultHandler handler) {
		
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		List<NFFTFingerprint> fingerprints = new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
		
		final List<NFFTFingerprintQueryMatch> queryMatches = new ArrayList<NFFTFingerprintQueryMatch>();
		
		queryMatches.addAll(storage.getMatches(fingerprints));
		
		
		double queryDuration = d.secondsProcessed();
		
		if(queryMatches.isEmpty()){
			QueryResult result = QueryResult.emptyQueryResult(0,queryDuration);
			handler.handleEmptyResult(result);
		}else{
			for(NFFTFingerprintQueryMatch match : queryMatches){
				String description = storage.getAudioDescription(match.identifier);
				handler.handleQueryResult(new QueryResult(0,queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
			}
		}
	}
	
	public void monitor(String query,final SerializedFingerprintsHandler handler){
		
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
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
				processMonitorQueryToSerializeFingerprints(audioEvent.getFloatBuffer().clone(), handler,timeStamp);
				return true;
			}
			
			@Override
			public void processingFinished() {
			}
		});
		d.run();
	}
	
	private void processMonitorQueryToSerializeFingerprints(float[] audioBuffer,SerializedFingerprintsHandler handler,double queryOffset){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		AudioDispatcher d;
		try {
			d = AudioDispatcherFactory.fromFloatArray(audioBuffer, samplerate, size, overlap);
			final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
			d.addAudioProcessor(minMaxProcessor);
			d.run();
			double queryDuration = d.secondsProcessed();
			List<NFFTFingerprint> fingerprints = new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
			handler.handleSerializedFingerprints(PanakoWebserviceClient.serializeFingerprintsToJson(fingerprints),queryDuration,queryOffset);
		} catch (UnsupportedAudioFileException e) {
			LOG.severe("Unsupported audio");
		}
	}
	
	public void matchSerializedFingerprints(String serizalizedFingerprints,final  int maxNumberOfResults,
			final QueryResultHandler handler,double queryDuration,double queryOffset){
		
		List<NFFTFingerprint> fingerprints = PanakoWebserviceClient.deserializeFingerprintsFromJson(serizalizedFingerprints);
		final List<NFFTFingerprintQueryMatch> queryMatches = new ArrayList<NFFTFingerprintQueryMatch>();
		
		queryMatches.addAll(storage.getMatches(fingerprints));
		if(queryMatches.isEmpty()){
			QueryResult result = QueryResult.emptyQueryResult(queryOffset,queryOffset+queryDuration);
			handler.handleEmptyResult(result);
		}else{
			for(NFFTFingerprintQueryMatch match : queryMatches){
				String description = storage.getAudioDescription(match.identifier);
				handler.handleQueryResult(new QueryResult(queryOffset,queryOffset+queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
			}
		}
	}
	

	@Override
	public void monitor(String query, Set<Integer> avoid,
                        final QueryResultHandler handler) {
		
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
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
				processMonitorQuery(audioEvent.getFloatBuffer().clone(), handler,timeStamp,avoid);
				return true;
			}
			
			@Override
			public void processingFinished() {
			}
		});
		d.run();
	}
	
	
	

	
	private void processMonitorQuery(float[] audioBuffer,
                                     QueryResultHandler handler, double queryOffset, Set<Integer> avoid){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		AudioDispatcher d;
		try {
			d = AudioDispatcherFactory.fromFloatArray(audioBuffer, samplerate, size, overlap);
			final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
			d.addAudioProcessor(minMaxProcessor);
			d.run();
			List<NFFTFingerprint> fingerprints = new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
			
			final List<NFFTFingerprintQueryMatch> queryMatches = new ArrayList<NFFTFingerprintQueryMatch>();
			
			queryMatches.addAll(storage.getMatches(fingerprints));
			
			double queryDuration = d.secondsProcessed();
			
			if(queryMatches.isEmpty()){
				QueryResult result = QueryResult.emptyQueryResult(queryOffset,queryOffset+queryDuration);
				handler.handleEmptyResult(result);
			}else{
				for(NFFTFingerprintQueryMatch match : queryMatches){
					//avoid the results in the avoid hash set
					if(!avoid.contains(match.identifier)){
						String description = storage.getAudioDescription(match.identifier);
						handler.handleQueryResult(new QueryResult(queryOffset,queryOffset+queryDuration,String.valueOf(match.identifier), description, match.score, match.getStartTime(),100.0,100.0));
					}
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
		return NFFTMapDBStorage.getInstance() != null;
	}
	
	public void compareFingerprints(File reference,File other) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(reference.getAbsolutePath(), samplerate, size, overlap);
		NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		List<NFFTFingerprint> fingerprintsReference = minMaxProcessor.getFingerprints();
		
		final List<NFFTFingerprint> fingerprintsQuery;		
		if(reference.getAbsolutePath().equals(other.getAbsolutePath())){
			fingerprintsQuery=fingerprintsReference;
		}else{
			d = AudioDispatcherFactory.fromPipe(other.getAbsolutePath(), samplerate, size, overlap);
			minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
			d.addAudioProcessor(minMaxProcessor);
			d.run();
			fingerprintsQuery = minMaxProcessor.getFingerprints();
		}
		
		
		TreeMap<Integer,List<NFFTFingerprint>> queryPrintsOrderedByTime;
		queryPrintsOrderedByTime = new TreeMap<>();
		for(NFFTFingerprint fingerprint: fingerprintsQuery){
			int milliseconds = fingerprint.t1 * samplerate / Config.getInt(Key.NFFT_STEP_SIZE);
			if( ! queryPrintsOrderedByTime.containsKey(milliseconds)){
				queryPrintsOrderedByTime.put(milliseconds,new ArrayList<NFFTFingerprint>());
			}
			queryPrintsOrderedByTime.get(milliseconds).add(fingerprint);
		}
		
		TreeMap<Integer,List<NFFTFingerprint>> referencePrintsOrderedByTime;
		referencePrintsOrderedByTime = new TreeMap<>();
		for(NFFTFingerprint fingerprint: fingerprintsReference){
			int milliseconds = fingerprint.t1 * samplerate / Config.getInt(Key.NFFT_STEP_SIZE);
			if( ! referencePrintsOrderedByTime.containsKey(milliseconds)){
				referencePrintsOrderedByTime.put(milliseconds,new ArrayList<NFFTFingerprint>());
			}
			referencePrintsOrderedByTime.get(milliseconds).add(fingerprint);
		}		
		
		int windowInMilliseconds=5000;
		int maxMilliseconds = 30000 * 1000;//max 30k seconds
		int prevMilliSeconds = -999;
		for(Map.Entry<Integer,List<NFFTFingerprint>> entry : queryPrintsOrderedByTime.entrySet()){
			int currentMilliseconds = entry.getKey();
			
			SortedMap<Integer,List<NFFTFingerprint>> searchSpace = referencePrintsOrderedByTime.subMap(currentMilliseconds, maxMilliseconds);
			//SortedMap<Integer,List<NFFTFingerprint>> searchSpace = referencePrintsOrderedByTime.subMap(0, maxMilliseconds);
			SortedMap<Integer,List<NFFTFingerprint>> query = queryPrintsOrderedByTime.subMap(currentMilliseconds, currentMilliseconds+windowInMilliseconds);
			Set<Integer> matchingMilliseconds = search(searchSpace,query);
			if(!matchingMilliseconds.isEmpty() && currentMilliseconds!=prevMilliSeconds){
				prevMilliSeconds = currentMilliseconds;
				System.out.print(currentMilliseconds + ",");
				ArrayList<Integer> matchingMS = new ArrayList<Integer>(matchingMilliseconds);
				Collections.sort(matchingMS);
				for(Integer matchingMilli : matchingMilliseconds){
					System.out.print(matchingMilli + ",");
				}
				System.out.println();
			}else{
				System.out.println(currentMilliseconds + "," + currentMilliseconds);
			}
		}
		
		LOG.info(String.format("%d fingerprints printed on stdout from %d event points for %s.",fingerprintsReference.size(),minMaxProcessor.getEventPoints().size(),reference.getAbsolutePath()));
		
		
	}
	
	private Set<Integer> search(SortedMap<Integer, List<NFFTFingerprint>> searchSpace,
			SortedMap<Integer, List<NFFTFingerprint>> query) {
		
		Set<Integer> millisecondMatches = new HashSet<Integer>();
		//create a database
		HashMap<Integer,List<NFFTFingerprint>> database = new HashMap<>();
		for(Map.Entry<Integer,List<NFFTFingerprint>> entry : searchSpace.entrySet()){
			for(NFFTFingerprint print : entry.getValue()){
				int hash = print.hash();
				if(!database.containsKey(hash)){
					database.put(hash, new ArrayList<NFFTFingerprint>());
				}
				database.get(hash).add(print);
			}
		}
		
		//match the query with the database, keep the matches
		List<NFFTFingerprintHit> matches = new ArrayList<>();
		for(Map.Entry<Integer,List<NFFTFingerprint>> entry : query.entrySet()){
			for(NFFTFingerprint queryPrint : entry.getValue()){
				int hash = queryPrint.hash();
				if(database.containsKey(hash)){
					List<NFFTFingerprint> dbPrints = database.get(hash);
					for(NFFTFingerprint dbPrint : dbPrints ){
						NFFTFingerprintHit hit = new NFFTFingerprintHit();
						hit.matchTime = dbPrint.t1;
						hit.queryTime = queryPrint.t1;
						hit.timeDifference = hit.matchTime - hit.queryTime;
						matches.add(hit);
					}
					
				}//else ignore the fingerprint
			}
		}
		
		//use this hash table to count the most popular offsets 
		HashMap<Integer,Integer> popularOffsets = new HashMap<Integer, Integer>();
		
		//add the offsets for each landmark hit 
		for(NFFTFingerprintHit hit : matches){
			if(!popularOffsets.containsKey(hit.timeDifference)){
				popularOffsets.put(hit.timeDifference, 0);	
			}
			int numberOfAlignedOffsets = 1 + popularOffsets.get(hit.timeDifference);
			popularOffsets.put(hit.timeDifference,numberOfAlignedOffsets);
		}
		
		float toMSFactor = (Config.getInt(Key.NFFT_STEP_SIZE) * 1000) / (float) Config.getInt(Key.NFFT_SAMPLE_RATE);
		for(NFFTFingerprintHit hit : matches){
			if(popularOffsets.containsKey(hit.timeDifference)){
				int alignedOffsets = popularOffsets.get(hit.timeDifference);
				if(alignedOffsets >= 5){
					popularOffsets.remove(hit.timeDifference);
					//System.out.println((hit.queryTime * 128000) / 8000 + " matches " +( hit.matchTime  * 128000) / 8000 + " aligned offsets:" + alignedOffsets);
					int startQueryMilliseconds = query.firstKey();
					int matchMilliseconds = startQueryMilliseconds + (int) (hit.timeDifference * toMSFactor);
					millisecondMatches.add(matchMilliseconds);
				}
			}
		}
		
		return millisecondMatches;
		
	}

	public NFFTStreamSync sync(String reference,String other){
		NFFTStreamSync syncObject = new NFFTStreamSync(reference, other);
		syncObject.synchronize();
		return syncObject;
	}

	@Override
	public void printStorageStatistics() {
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
