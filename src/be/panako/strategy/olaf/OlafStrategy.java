package be.panako.strategy.olaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.FileUtils;
import be.panako.util.StopWatch;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class OlafStrategy extends Strategy {

	@Override
	public double store(String resource, String description) {
		OlafStorage db = OlafStorage.getInstance();
		
		List<OlafFingerprint> prints = toFingerprints(resource);
		
		int resourceID = FileUtils.getIdentifier(resource);
		//store
		for(OlafFingerprint print : prints) {
			long hash = print.fixedHash();			
			int printT1 = print.t1;
			db.addToStoreQueue(hash, resourceID, printT1);
		}		
		db.processStoreQueue();
		
		//store meta-data as well
		float duration = 0;
		if(prints.size() != 0) {
			duration = blocksToSeconds(prints.get(prints.size()-1).t3);
		}else {
			System.err.println("Warning: no prints extracted for " + resource);
		}
		int numberOfPrints = prints.size();
		
		db.storeMetadata((long) resourceID,resource,duration,numberOfPrints);
		
		return duration;
	}
	
	public List<OlafFingerprint> toFingerprints(String resource){
		return toFingerprints(resource,0,500_000) ;	
	}
	
	public List<OlafFingerprint> toFingerprints(String resource,double startTimeOffset,double numberOfSeconds){
		int samplerate, size, overlap;
		samplerate = 16000;
		size = 1024;
		overlap = size -128;
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap,startTimeOffset,numberOfSeconds);
		OlafEventPointProcessor eventPointProcessor = new OlafEventPointProcessor(size);
		d.addAudioProcessor(eventPointProcessor);
		d.run();
		
		return eventPointProcessor.getFingerprints() ;	
	}
	
	
	private float blocksToSeconds(int t) {		
		return t * (128/16000.0f);
	}

	private int numberOfMatches = 0;
	@Override
	public void query(String query, int maxNumberOfResults, Set<Integer> avoid, QueryResultHandler handler) {
		query(query,maxNumberOfResults,avoid,handler,0,50000);
	}
	
	public void query(String query, int maxNumberOfResults, Set<Integer> avoid, QueryResultHandler handler, double startTimeOffset,double numberOfSeconds ) {
		numberOfMatches = 0;
		
		final String queryDesc ;
		if(numberOfSeconds != 50000) {
			queryDesc = query + "-" + startTimeOffset + "_" + (startTimeOffset+numberOfSeconds);
		}else {
			queryDesc = query;
		}
		
		List<OlafFingerprint> prints = toFingerprints(query,startTimeOffset,numberOfSeconds);
		
		
		OlafStorage db = OlafStorage.getInstance();
		
		Map<Long,OlafFingerprint> printMap = new HashMap<>();
		
		//query
		for(OlafFingerprint print : prints) {
			long hash = print.fixedHash();
			db.addToQueryQueue(hash);
			printMap.put(hash, print);
		}
		
		//fingerprint hash to info
		Map<Long,long[]> matchAccumulator = new HashMap<>();
		
		StopWatch w = new StopWatch();
		db.processQueryQueue(matchAccumulator, 1, avoid);
		System.err.printf("Query for %d prints, %d matches in %s \n", printMap.size(),matchAccumulator.size(), w.formattedToString());
		
		 HashMap<Integer,List<OlafHit>> hitsPerIdentifer = new HashMap<>();
		 
		 matchAccumulator.forEach((fingerprintHash, data) -> {
			 //long matchingHash  = data[0];
			 int identifier = (int) data[1];
			 int matchTime = (int) data[2];
			 if(!hitsPerIdentifer.containsKey(identifier)){
					hitsPerIdentifer.put(identifier, new ArrayList<OlafHit>());
			 }
			 OlafHit hit = new OlafHit();
			 hit.identifier = identifier;
			 hit.matchTime = matchTime;
			 hit.queryTime = printMap.get(fingerprintHash).t1;
			 hitsPerIdentifer.get(identifier).add(hit);
		 });
		 
		 int min = 10;
		 
		 List<Integer> matchesToDelete = new ArrayList<>();
		 hitsPerIdentifer.forEach((identifier, hitlist) -> {
			 if(hitlist.size() < min) {
				 matchesToDelete.add(identifier);
			 }
		 });
		 
		 matchesToDelete.forEach( identifier ->{
			 hitsPerIdentifer.remove(identifier);
			 //System.out.println("Removed " + identifier);
		 });
		 
		 hitsPerIdentifer.forEach((identifier, hitlist) -> {
			// System.out.println("Matches " + identifier + " matches " + hitlist.size());
			 
			 //sort by query time
			 Collections.sort(hitlist, (Comparator<? super OlafHit>) (OlafHit a, OlafHit b) -> Integer.valueOf(a.queryTime).compareTo(Integer.valueOf(b.queryTime)));
			 
			 List<OlafHit> firstHits = new ArrayList<>(hitlist.subList(0, Math.max(min,hitlist.size()/5)));
			 List<OlafHit> lastHits = new ArrayList<>(hitlist.subList(hitlist.size()-Math.max(min,hitlist.size()/5), hitlist.size()));
			 
			 //sort by diff
			 Collections.sort(firstHits, (Comparator<? super OlafHit>) (OlafHit a, OlafHit b) -> Integer.valueOf(a.Δt()).compareTo(Integer.valueOf(b.Δt())));
			 Collections.sort(lastHits, (Comparator<? super OlafHit>) (OlafHit a, OlafHit b) -> Integer.valueOf(a.Δt()).compareTo(Integer.valueOf(b.Δt())));
			 
			 float y1 = firstHits.get(firstHits.size()/2).Δt();
			 float x1 = 0;
			 
			 for(int i = 0 ; i < firstHits.size() ; i++) {
				 OlafHit hit = firstHits.get(i);
				 int diff = hit.Δt();
				 if(diff == y1) {
					 x1 = hit.queryTime;
					 break;
				 }
			 }
			 
			 float y2 = lastHits.get(lastHits.size()/2).Δt();
			 float x2 = 0;
			 for(int i = lastHits.size() - 1 ; i >= 0 ; i--) {
				 OlafHit hit = lastHits.get(i);
				 int diff = hit.Δt();
				 if(diff == y2) {
					 x2 = hit.queryTime;
					 break;
				 }
			 }
			 
			 float slope = (y2-y1)/(x2-x1);
			 float offset = -x1 * slope + y1;
			 
			 List<OlafHit> filteredHits = new ArrayList<>();
			 
			 hitlist.forEach( hit ->{				 
				 float yActual = hit.Δt();
				 float x = hit.queryTime;
				 float yPredicted = slope * x + offset;
				 //should be within 15% of the expected range
				 boolean yInExpectedRange = Math.abs(yActual-yPredicted) <= 0.10 * yActual; 
				 
				 if(yInExpectedRange) {
					 filteredHits.add(hit);
				 }
			 });
			 
			 if(filteredHits.size() > 5) {
				 float minDuration = 0;
				 float queryTimeStart = blocksToSeconds(filteredHits.get(0).queryTime);
				 float queryTimeStop = blocksToSeconds(filteredHits.get(filteredHits.size()-1).queryTime);
				 float duration = queryTimeStop - queryTimeStart;
				 
				 if(duration >= minDuration) {
					 float deltaT = blocksToSeconds(filteredHits.get(0).Δt());
					 float timeFactor = 1-slope;
					 
					 if(timeFactor > 0.9 && timeFactor < 1.1) {
						 String description = "";
						 int score = filteredHits.size();
						 float frequencyFactor = 1.0f;
						 float matchTime = blocksToSeconds(filteredHits.get(0).matchTime);
						 
						 OlafResourceMetadata metadata = db.getMetadata((long) identifier);
						 description = metadata.path;
						 
						 handler.handleQueryResult(new QueryResult(queryDesc,deltaT, queryTimeStop, "" + identifier, description, score, matchTime, timeFactor, frequencyFactor));
						 numberOfMatches++;
					 }
				 }
			 }
		 });
		 
		 if (numberOfMatches==0) {
			 handler.handleEmptyResult(QueryResult.emptyQueryResult(queryDesc,0,0));
		 }
		
	}
	
	
	
	
	public  static class OlafHit {
		
		/**
		 * The match audio identifier
		 */
		public int identifier;
		
		/**
		 * Time in blocks in the original, matched audio.
		 */
		public int matchTime;
		
		/**
		 * Time in blocks in the query.
		 */
		public int queryTime;
		
		public int Δt() {
			return matchTime - queryTime;
		}
	}
		
	
	
	public static class OlafFingerprintQueryMatch{
		public int identifier;
		public double starttime;
		public int score;
		public int mostPopularOffset;
	}

	@Override
	public void monitor(String query, int maxNumberOfReqults, Set<Integer> avoid, QueryResultHandler handler) {
		int stepInSeconds = 5;
		int audioDuration = 15;
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, 8000, 2048, 0);
		d.run();
		double totalDuration = d.secondsProcessed();
		
		for(int t = 0 ; t + audioDuration < totalDuration; t+= stepInSeconds ) {			
			query(query,maxNumberOfReqults,avoid,handler,t,audioDuration);
		}
	}

	@Override
	public boolean hasResource(String resource) {
		int identifier = FileUtils.getIdentifier(resource);
		return OlafStorage.getInstance().getMetadata(identifier) != null;
	}

	@Override
	public boolean isStorageAvailable() {
		return true;
	}

	@Override
	public void printStorageStatistics() {
		OlafStorage.getInstance().entries(true);
	}

	@Override
	public String resolve(String filename) {
		return "" + FileUtils.getIdentifier(filename);
	}

}
