package be.panako.strategy.gaborator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.gaborator.GaboratorDBStorage.GaboratorDBHit;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.panako.util.StopWatch;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.util.PitchConverter;

public class GaboratorStrategy extends Strategy {
	private static final int MAX_TIME = 5_000_000;
	
	private final static Logger LOG = Logger.getLogger(GaboratorStrategy.class.getName());
	
	@Override
	public double store(String resource, String description) {

		GaboratorDBStorage db;
		
		
		db = GaboratorDBStorage.getInstance();
		
		
		List<GaboratorFingerprint> prints = toFingerprints(resource);
		
		int resourceID = FileUtils.getIdentifier(resource);
		//store
		for(GaboratorFingerprint print : prints) {
			long hash = print.hash();
			db.addToStoreQueue(hash, resourceID, print.t1,print.f1);
		}
		db.processStoreQueue();
		
		//store meta-data as well
		float duration = 0;
		if(prints.size() != 0) {
			duration = blocksToSeconds(prints.get(prints.size()-1).t3);
			LOG.info(String.format("Stored %d fingerprints for '%s', id: %d", prints.size() , resource ,resourceID));
		}else {
			LOG.warning("Warning: no prints extracted for " + resource);
			
		}
		int numberOfPrints = prints.size();
		
		db.storeMetadata((long) resourceID,resource,duration,numberOfPrints);
		
		//storage is done: 
		//try to clear memory
		System.gc();
		
		return duration;
	}
	
	public double delete(String resource, String description) {

		GaboratorDBStorage db = GaboratorDBStorage.getInstance();
		
		List<GaboratorFingerprint> prints = toFingerprints(resource);
		
		int resourceID = FileUtils.getIdentifier(resource);
		//delete
		for(GaboratorFingerprint print : prints) {
			long hash = print.hash();
			db.addToDeleteQueue(hash, resourceID, print.t1,print.f1);
		}
		db.processDeleteQueue();
		
		//delete meta-data as well
		float duration = 0;
		if(prints.size() != 0) {
			duration = blocksToSeconds(prints.get(prints.size()-1).t3);
		}else {
			LOG.warning("Warning: no prints extracted for " + resource);
		}
	
		
		db.deleteMetadata((long) resourceID);
		
		//storage is done: 
		//try to clear memory
		System.gc();
		
		return duration;
	}
	
	public List<GaboratorFingerprint> toFingerprints(String resource){
		
		
		return toFingerprints(resource,0,MAX_TIME);
	}
	
	public List<GaboratorFingerprint> toFingerprints(String resource,double startTimeOffset,double numberOfSeconds){
		int samplerate, size, overlap;
		samplerate = Config.getInt(Key.GABORATOR_SAMPLE_RATE);
		size = Config.getInt(Key.GABORATOR_AUDIO_BLOCK_SIZE);
		overlap = Config.getInt(Key.GABORATOR_AUDIO_BLOCK_OVERLAP);
		
		AudioDispatcher d;
		
		if(numberOfSeconds==MAX_TIME)
			d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap,startTimeOffset);
		else
			d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap,startTimeOffset,numberOfSeconds);
		
		GaboratorEventPointProcessor eventPointProcessor = new GaboratorEventPointProcessor(size);
		latency = eventPointProcessor.latency();
		d.addAudioProcessor(eventPointProcessor);
		d.run();
		
		return eventPointProcessor.getFingerprints();	
	}
	
	
	int latency;
	private float blocksToSeconds(int t) {
		float timeResolution = Config.getFloat(Key.GABORATOR_TRANSF_TIME_RESOLUTION);
		float sampleRate = Config.getFloat(Key.GABORATOR_SAMPLE_RATE);
		
		return t * (timeResolution/sampleRate) + latency/(float) sampleRate;
	}
	
	private float binToHz(int f) {
		float minFreq = Config.getFloat(Key.GABORATOR_TRANSF_MIN_FREQ);
		float binsPerOctave = Config.getFloat(Key.GABORATOR_TRANSF_BANDS_PER_OCTAVE);
		float centsPerBin = 1200.0f / binsPerOctave;
		
		float diffFromMinFreqInCents = f * centsPerBin;
		float minFreqInAbsCents = (float) PitchConverter.hertzToAbsoluteCent(minFreq);
		float binInAbsCents = minFreqInAbsCents + diffFromMinFreqInCents;
		
		return (float) PitchConverter.absoluteCentToHertz(binInAbsCents);
	}

	private int mostCommonDeltaTforHitList(List<GaboratorHit> hitList) {
		Map<Integer,Integer> countPerDiff = new HashMap<>();
		hitList.forEach((hit)->{
			int deltaT = hit.Δt();
			if(!countPerDiff.containsKey(deltaT))
				countPerDiff.put(deltaT, 0);
			countPerDiff.put(deltaT, countPerDiff.get(deltaT)+1);
		});
		
		int maxCount = 0;
		int mostCommonDeltaT = 0;
		for(Map.Entry<Integer,Integer> entry : countPerDiff.entrySet()) {
			int count = entry.getValue();
			if(count > maxCount) {
				maxCount = count;
				mostCommonDeltaT = entry.getKey();
			}
		}
		return mostCommonDeltaT;
	}
	@Override
	public void query(String query, int maxNumberOfResults, Set<Integer> avoid, QueryResultHandler handler) {
		query(query,maxNumberOfResults,avoid,handler,0,MAX_TIME);
	}
	
	public void query(String query, int maxNumberOfResults, Set<Integer> avoid, QueryResultHandler handler, double startTimeOffset,double numberOfSeconds ) {
		
		final String queryPath ;
		List<GaboratorFingerprint> prints;
		if(numberOfSeconds != MAX_TIME) {
			queryPath = query + "-" + startTimeOffset + "_" + (startTimeOffset+numberOfSeconds);
			prints = toFingerprints(query,startTimeOffset,numberOfSeconds);
		}else {
			queryPath = query;
			prints = toFingerprints(query);
		}
		
		GaboratorDBStorage db = GaboratorDBStorage.getInstance();
		
		Map<Long,GaboratorFingerprint> printMap = new HashMap<>();
		
		//query
		for(GaboratorFingerprint print : prints) {
			long hash = print.hash();
			db.addToQueryQueue(hash);
			printMap.put(hash, print);
		}
		
		//fingerprint hash to info
		Map<Long,List<GaboratorDBHit>> matchAccumulator = new HashMap<>();
		
		StopWatch w = new StopWatch();
		int queryRange = Config.getInt(Key.GABORATOR_QUERY_RANGE);
		db.processQueryQueue(matchAccumulator,queryRange , avoid);
		
		LOG.info(String.format("Query for %d prints, %d matches in %s \n", printMap.size(),matchAccumulator.size(), w.formattedToString()));
		
		 HashMap<Integer,List<GaboratorHit>> hitsPerIdentifer = new HashMap<>();
		 
		 final List<QueryResult> queryResults = new ArrayList<>();
		 
		 matchAccumulator.forEach((fingerprintHash, dbHits) -> {
			 
			 dbHits.forEach((dbHit)->{
				//long matchingHash  = data[0];
				 int identifier = dbHit.resourceID;
				 int matchTime = dbHit.t;
				 if(!hitsPerIdentifer.containsKey(identifier)){
					hitsPerIdentifer.put(identifier, new ArrayList<GaboratorHit>());
				 }
				 GaboratorHit hit = new GaboratorHit();
				 hit.identifier = identifier;
				 hit.matchTime = matchTime;
				 hit.originalHash = dbHit.originalHash;
				 hit.matchedNearHash = dbHit.matchedNearHash;
				 hit.queryTime = printMap.get(fingerprintHash).t1;
				 
				 hit.queryF1 = printMap.get(fingerprintHash).f1;
				 hit.matchF1 = dbHit.f;
				 
				 hitsPerIdentifer.get(identifier).add(hit);
			 });
		 });
		 
		 int minimumUnfilteredHits = Config.getInt(Key.GABORATOR_MIN_HITS_UNFILTERED);
		 int minimumFilteredHits = Config.getInt(Key.GABORATOR_MIN_HITS_FILTERED);
		 
		 List<Integer> matchesToDelete = new ArrayList<>();
		 hitsPerIdentifer.forEach((identifier, hitlist) -> {
			 if(hitlist.size() < minimumUnfilteredHits) {
				 matchesToDelete.add(identifier);
			 }
		 });
		 
		 matchesToDelete.forEach( identifier ->{
			 hitsPerIdentifer.remove(identifier);
			 //System.out.println("Removed " + identifier);
		 });
		 
		 hitsPerIdentifer.forEach((identifier, hitlist) -> {
			 //System.out.println("Matches " + identifier + " matches " + hitlist.size());
			 
			 //sort by query time
			 Collections.sort(hitlist, (Comparator<? super GaboratorHit>) (GaboratorHit a, GaboratorHit b) -> Integer.valueOf(a.queryTime).compareTo(Integer.valueOf(b.queryTime)));
			
			 //view the first and last hits (max 250)
			 int maxListSize = 250;
			 List<GaboratorHit> firstHits = hitlist.subList(0, Math.min(maxListSize,Math.max(minimumUnfilteredHits,hitlist.size()/5)));
			 List<GaboratorHit> lastHits  = hitlist.subList(hitlist.size()-Math.min(maxListSize, Math.max(minimumUnfilteredHits,hitlist.size()/5)), hitlist.size());
			 
			//find the first x1 where delta t is equals to the median delta t
			 float y1 = mostCommonDeltaTforHitList(firstHits);
			 float x1 = 0;
			 float frequencyFactor = 0;
			 for(int i = 0 ; i < firstHits.size() ; i++) {
				 GaboratorHit hit = firstHits.get(i);
				 int diff = hit.Δt();
				 if(diff == y1) {
					 x1 = hit.queryTime;
					 frequencyFactor = binToHz(hit.matchF1) / binToHz(hit.queryF1);
					 break;
				 }
			 }

			//find the first x2 where delta t is equals to the median delta t
			 float y2 = mostCommonDeltaTforHitList(lastHits);
			 float x2 = 0;
			 for(int i = lastHits.size() - 1 ; i >= 0 ; i--) {
				 GaboratorHit hit = lastHits.get(i);
				 int diff = hit.Δt();
				 if(diff == y2) {
					 x2 = hit.queryTime;
					 break;
				 }
			 }
			 	 
			 float slope = (y2-y1)/(x2-x1);
			 float offset = -x1 * slope + y1;
			 float timeFactor = 1.0f/(1-slope);
			 	 
			 //System.out.printf("slope %f offset %f  timefactor %f  freqfactor %f \n",slope,offset,timeFactor,frequencyFactor);
			 
			 //threshold in time bins
			 double threshold = Config.getFloat(Key.GABORATOR_QUERY_RANGE);
			 
			 //only continue processing when time factor is reasonable
			 if(timeFactor > Config.getFloat(Key.GABORATOR_MIN_TIME_FACTOR) && timeFactor < Config.getFloat(Key.GABORATOR_MAX_TIME_FACTOR) && 
					 frequencyFactor> Config.getFloat(Key.GABORATOR_MIN_FREQ_FACTOR) &&  timeFactor < Config.getFloat(Key.GABORATOR_MAX_FREQ_FACTOR)	 ) {
				 List<GaboratorHit> filteredHits = new ArrayList<>();
				 
				 hitlist.forEach( hit ->{				 
					 float yActual = hit.Δt();
					 float x = hit.queryTime;
					 float yPredicted = slope * x + offset;
					 
					 //should be within an expected range
					 boolean yInExpectedRange = Math.abs(yActual-yPredicted) <= threshold ; 
					 
					 //if(hit.identifier!= FileUtils.getIdentifier(queryPath))
						 //System.out.printf("pred: %f  actual: %f   dif abs: %f  threshold %f \n",yPredicted, yActual,Math.abs(yActual-yPredicted), threshold);
					 
					 if(yInExpectedRange) {
						 filteredHits.add(hit);
					 }
				 });
				 
				 //ignore resources with too few filtered hits remaining
				 if(filteredHits.size() > minimumFilteredHits) {
					 //System.out.println("Matches " + identifier + " matches filtered hits: " + filteredHits.size());
					 
					 float minDuration = 0;
					 float queryStart = blocksToSeconds(filteredHits.get(0).queryTime);
					 float queryStop = blocksToSeconds(filteredHits.get(filteredHits.size()-1).queryTime);
					 float duration = queryStop - queryStart;
					 
					 if(duration >= minDuration) {
						 int score = filteredHits.size();
						
						 
						 float refStart = blocksToSeconds(filteredHits.get(0).matchTime);
						 float refStop =  blocksToSeconds(filteredHits.get(filteredHits.size()-1).matchTime);
						 
						 //retrieve meta-data
						 GaboratorResourceMetadata metadata = db.getMetadata((long) identifier);
						 String refPath = "metadata unavailable!";
						 if(metadata != null )
							 refPath = metadata.path;
						 
						 //Construct a histogram with the number of matches for each second
						 //Ideally there is a more or less equal number of matches each second
						 // note that the last second might not be a full second
						 TreeMap<Integer,Integer> matchesPerSecondHistogram = new TreeMap<>();
						 for(GaboratorHit hit : filteredHits) {
							 //if(hit.identifier!= FileUtils.getIdentifier(queryPath))
								 //System.out.printf("%d %d %d %d %d\n", hit.identifier, hit.matchTime, hit.queryTime, hit.originalHash, hit.matchedNearHash);
							 float offsetInSec = blocksToSeconds(hit.matchTime) - refStart;
							 int secondBin = (int) offsetInSec;
							 if(!matchesPerSecondHistogram.containsKey(secondBin))
								 matchesPerSecondHistogram.put(secondBin, 0);
							 matchesPerSecondHistogram.put(secondBin, matchesPerSecondHistogram.get(secondBin)+1);
						 }
						
						 //number of seconds bins
						 float numberOfMatchingSeconds = (float) Math.ceil(refStop - refStart);
						 float emptySeconds = numberOfMatchingSeconds - matchesPerSecondHistogram.size();
						 float emptyRatio = emptySeconds / numberOfMatchingSeconds;
						 
						 QueryResult r = new QueryResult(queryPath,queryStart, queryStop, refPath, "" + identifier, refStart, refStop,  score, timeFactor, frequencyFactor,emptyRatio);
						 queryResults.add(r);
					 }
				 }
			 }			 
		 });
		 
		 if (queryResults.isEmpty()) {
			 handler.handleEmptyResult(QueryResult.emptyQueryResult(queryPath,0,0));
		 }else {
			 //sort results by score desc
			 queryResults.sort((Comparator<? super QueryResult>) (QueryResult a, QueryResult b) -> Integer.valueOf((int) b.score).compareTo(Integer.valueOf((int) a.score)));
			 //limit results to max number of results, if needed
			 List<QueryResult> finalResults = queryResults;
			 if(queryResults.size()>maxNumberOfResults) {
				 finalResults = queryResults.subList(0, maxNumberOfResults);
			 }
			 //handle the results in order (best score first)
			 for(QueryResult r : finalResults) {
				 handler.handleQueryResult(r);
			 }
		 }
	}
	
	
	public  static class GaboratorHit {
		
		public long matchedNearHash;

		public long originalHash;

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
		
		/**
		 * Frequency bin in the original, matched audio.
		 */
		public int matchF1;
		
		/**
		 * Frequency bin the query.
		 */
		public int queryF1;
		
		public int Δt() {
			return matchTime - queryTime;
		}
		
		public String toString() {
			return String.format("%d %d %d %d", identifier, matchTime, queryTime, Δt());
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
		int overlapInSeconds = Config.getInt(Key.MONITOR_OVERLAP); // 5
		int stepSizeInSeconds = Config.getInt(Key.MONITOR_STEP_SIZE); //25
		
		// Get the total duration
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, 8000, 2048, 0);
		d.run();
		double totalDuration = d.secondsProcessed();
		
		//Steps: 0-25s ; 20-45s ; 40-65s ...
		int actualStep = stepSizeInSeconds - overlapInSeconds;//20s
		for(int t = 0 ; t + stepSizeInSeconds < totalDuration; t += actualStep ) {			
			query(query,maxNumberOfReqults,avoid,handler,t,stepSizeInSeconds);
		}
	}

	@Override
	public boolean hasResource(String resource) {
		int identifier = FileUtils.getIdentifier(resource);
		GaboratorDBStorage db;
		db = GaboratorDBStorage.getInstance();
		
		return db.getMetadata(identifier) != null;
	}

	@Override
	public boolean isStorageAvailable() {
		return true;
	}

	@Override
	public void printStorageStatistics() {
		GaboratorDBStorage.getInstance().entries(true);
	}

	@Override
	public String resolve(String filename) {
		return "" + FileUtils.getIdentifier(filename);
	}
	
	private void addToMap(TreeMap<Integer,float[]> map,int t,int f,float m) {
		if(!map.containsKey(t)) {
			map.put(t, new float[Config.getInt(Key.OLAF_SIZE)/2]);
		}
		map.get(t)[f]=m;
	}

	public void print(String path, boolean sonicVisualizerOutput) {
		List<GaboratorFingerprint> prints = toFingerprints(path);

		TreeMap<Integer,float[]> timeIndexedSpectralPeaks = new TreeMap<>();
		for(GaboratorFingerprint print : prints) {
			addToMap(timeIndexedSpectralPeaks,print.t1,print.f1,print.m1);
			addToMap(timeIndexedSpectralPeaks,print.t2,print.f2,print.m2);
			addToMap(timeIndexedSpectralPeaks,print.t2,print.f2,print.m3);
		}
		float[] emptySpectrum = new float[Config.getInt(Key.OLAF_SIZE)/2];
		StringBuilder sb = new StringBuilder();
		for(int t = 0 ; t <= timeIndexedSpectralPeaks.lastKey();t++) {
			float[] spectrum = emptySpectrum;
			if(timeIndexedSpectralPeaks.containsKey(t))
				spectrum = timeIndexedSpectralPeaks.get(t);
			
			sb.append(blocksToSeconds(t)).append(",");
			for(int i = 0 ; i < spectrum.length ; i++) {
				//sb.append(spectrum[i]).append(",");
			}
			sb.append("\n");
		}
		System.out.println(sb.toString());		
	}
}
