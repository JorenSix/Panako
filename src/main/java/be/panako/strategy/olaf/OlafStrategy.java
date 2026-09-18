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

package be.panako.strategy.olaf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.olaf.storage.*;
import be.panako.util.*;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.util.PitchConverter;

/**
 * The algorithm implemented here is inspired by the 'shazam' algorithm but does differ in a few crucial elements.
 *
 * A fingerprint consists of three spectral peaks
 * The matching step allows for moderate time stretching amounts
 *
 *
 */
public class OlafStrategy extends Strategy {
	private static final int MAX_TIME = 5_000_000;
	
	private final static Logger LOG = Logger.getLogger(OlafStrategy.class.getName());

	private OlafStorage db;

	/**
	 * Create an instance
	 */
	public OlafStrategy(){

	}

	private OlafStorage getStorage(){
		if (db ==null){
			OlafStorage db;
			if (Config.get(Key.OLAF_STORAGE).equalsIgnoreCase("LMDB")) {
				db = OlafStorageKV.getInstance();
			}else if (Config.get(Key.OLAF_STORAGE).equalsIgnoreCase("FILE")) {
				db = OlafStorageFile.getInstance();
			}else {
				db = OlafStorageMemory.getInstance();
			}

			if(Config.getBoolean(Key.OLAF_CACHE_TO_FILE) && db != OlafStorageFile.getInstance()) {
				LOG.info("Using "+ db.getClass().getSimpleName() + " storage with caching front.");
				db = new OlafCachingStorage(OlafStorageFile.getInstance(),db);
			}else {
				LOG.info("Using " + db.getClass().getSimpleName() + " as storage.");
			}
			this.db = db;
		}
		return db;
	}
	
	@Override
	public double store(String resource, String description) {

		OlafStorage db = getStorage();
		
		List<OlafFingerprint> prints = toFingerprints(resource);
		
		int resourceID = FileUtils.getIdentifier(resource);
		//store
		for(OlafFingerprint print : prints) {
			long hash = print.hash();			
			int printT1 = print.t1;
			db.addToStoreQueue(hash, resourceID, printT1);
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
		
		db.storeMetadata(resourceID,resource,duration,numberOfPrints);
		
		//storage is done: 
		//try to clear memory
		System.gc();
		
		return duration;
	}

	@Override
	public double delete(String resource) {
		OlafStorage db = getStorage();

		List<OlafFingerprint> prints = toFingerprints(resource);

		int resourceID = FileUtils.getIdentifier(resource);
		//delete
		for(OlafFingerprint print : prints) {
			long hash = print.hash();
			int printT1 = print.t1;
			db.addToDeleteQueue(hash, resourceID, printT1);
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

	/**
	 * For the resource with a certain path, either use a cached file with fingerprints or
	 * extract fingerprints.
	 * @param resource The path to the audio resource
	 * @return A list of fingerprints
	 */
	public List<OlafFingerprint> toFingerprints(String resource){
		return toFingerprints(resource,0,MAX_TIME);
	}

	private List<OlafFingerprint> toFingerprints(String resource,double startTimeOffset,double numberOfSeconds){
		if(Config.getBoolean(Key.OLAF_USE_CACHED_PRINTS)) {
			String folder = Config.get(Key.OLAF_CACHE_FOLDER);
			folder = FileUtils.expandHomeDir(folder);
			String tdbPath =  FileUtils.combine(folder,resolve(resource) + ".tdb");

			if(FileUtils.exists(tdbPath)) {
				List<OlafFingerprint> prints = new ArrayList<>();
				List<long[]> printData = readFingerprintFile(tdbPath);
				for(long[] data : printData) {
					long fingerprintHash = data[0];
					int t1 = (int) data[2];
					float t1InSeconds = blocksToSeconds(t1);

					//skip all fingerprints after stop time
					if(t1InSeconds > startTimeOffset + numberOfSeconds)
						break;
					//only add prints if they are after the start time offset
					if(t1InSeconds >= startTimeOffset)
						prints.add(new OlafFingerprint(fingerprintHash,t1));
				}
				LOG.info(String.format("Read %d cached fingerprints from file '%s' (start: %.3f sec, stop: %.3f sec) for '%s'", prints.size(),tdbPath,startTimeOffset,startTimeOffset+numberOfSeconds,resource));
				return prints;
			}else{
				LOG.info(String.format("Could not read cached fingerprints from file '%s' for '%s'",tdbPath,resource));
			}
		} //else no cached prints are found

		int samplerate, size, overlap;
		samplerate = Config.getInt(Key.OLAF_SAMPLE_RATE);
		size = Config.getInt(Key.OLAF_SIZE);
		overlap = size - Config.getInt(Key.OLAF_STEP_SIZE);
		
		AudioDispatcher d;
		
		if(numberOfSeconds==MAX_TIME)
			d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap,startTimeOffset);
		else
			d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap,startTimeOffset,numberOfSeconds);
		
		OlafEventPointProcessor eventPointProcessor = new OlafEventPointProcessor(size);
		d.addAudioProcessor(eventPointProcessor);
		d.run();
		
		return eventPointProcessor.getFingerprints();
	}

	private List<OlafEventPoint> toEventpoints(String resource){
		int samplerate, size, overlap;
		samplerate = Config.getInt(Key.OLAF_SAMPLE_RATE);
		size = Config.getInt(Key.OLAF_SIZE);
		overlap = size - Config.getInt(Key.OLAF_STEP_SIZE);

		AudioDispatcher d;
		d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap,0);
		OlafEventPointProcessor eventPointProcessor = new OlafEventPointProcessor(size);
		d.addAudioProcessor(eventPointProcessor);
		d.run();

		return eventPointProcessor.getEventPoints();
	}
	
	private float blocksToSeconds(int t) {		
		return t * (Config.getInt(Key.OLAF_STEP_SIZE)/(float) Config.getInt(Key.OLAF_SAMPLE_RATE));
	}

	private float binToHz(int f) {
		double sampleRate = Config.getFloat(Key.OLAF_SAMPLE_RATE);
		double fftSize = Config.getFloat(Key.OLAF_SIZE);
		double binSizeInHz = sampleRate / fftSize;
		double centerBinFrequencyInHz = f * binSizeInHz + binSizeInHz /2.0;
		return (float) centerBinFrequencyInHz;
	}

	private int mostCommonDeltaTforHitList(List<OlafMatch> hitList) {
		Map<Integer,Integer> countPerDiff = new HashMap<>();
		hitList.forEach((hit)->{
			int deltaT = hit.deltaT();
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


	private void query(String query, int maxNumberOfResults, Set<Integer> avoid, QueryResultHandler handler, double startTimeOffset,double numberOfSeconds ) {
		
		final String queryPath ;
		List<OlafFingerprint> prints;
		if(numberOfSeconds != MAX_TIME) {
			queryPath = query + "-" + startTimeOffset + "_" + (startTimeOffset+numberOfSeconds);
			prints = toFingerprints(query,startTimeOffset,numberOfSeconds);
		}else {
			queryPath = query;
			prints = toFingerprints(query);
		}
		
		final OlafStorage db = getStorage();;
		
		Map<Long,OlafFingerprint> printMap = new HashMap<>();
		
		//query
		for(OlafFingerprint print : prints) {
			long hash = print.hash();
			db.addToQueryQueue(hash);
			printMap.put(hash, print);
		}
		
		//fingerprint hash to info
		Map<Long,List<OlafHit>> matchAccumulator = new HashMap<>();
		
		StopWatch w = new StopWatch();
		int queryRange = Config.getInt(Key.OLAF_QUERY_RANGE); 
		db.processQueryQueue(matchAccumulator,queryRange , avoid);
		
		LOG.info(String.format("Query for %d prints, %d matches in %s \n", printMap.size(),matchAccumulator.size(), w.formattedToString()));
		
		 HashMap<Integer,List<OlafMatch>> hitsPerIdentifer = new HashMap<>();
		 
		 final List<QueryResult> queryResults = new ArrayList<>();
		 
		 matchAccumulator.forEach((fingerprintHash, dbHits) -> {
			 
			 dbHits.forEach((dbHit)->{
				//long matchingHash  = data[0];
				 int identifier = dbHit.resourceID;
				 int matchTime = dbHit.t;
				 if(!hitsPerIdentifer.containsKey(identifier)){
					hitsPerIdentifer.put(identifier, new ArrayList<OlafMatch>());
				 }
				 OlafMatch hit = new OlafMatch();
				 hit.identifier = identifier;
				 hit.matchTime = matchTime;
				 hit.originalHash = dbHit.originalHash;
				 hit.matchedNearHash = dbHit.matchedNearHash;
				 hit.queryTime = printMap.get(fingerprintHash).t1;
				 hitsPerIdentifer.get(identifier).add(hit);
			 });
		 });
		 
		 int minimumUnfilteredHits = Config.getInt(Key.OLAF_MIN_HITS_UNFILTERED);
		 int minimumFilteredHits = Config.getInt(Key.OLAF_MIN_HITS_FILTERED);
		 
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
			 Collections.sort(hitlist, (Comparator<? super OlafMatch>) (OlafMatch a, OlafMatch b) -> Integer.valueOf(a.queryTime).compareTo(Integer.valueOf(b.queryTime)));
			
			 //view the first and last hits (max 250)
			 int maxPartListSize = Config.getInt(Key.OLAF_HIT_PART_MAX_SIZE);
			 int partDivider = Config.getInt(Key.OLAF_HIT_PART_DIVIDER);
			 int partListLength = Math.min(maxPartListSize,Math.max(minimumUnfilteredHits,hitlist.size()/partDivider));

			 List<OlafMatch> firstHits = hitlist.subList(0, partListLength);
			 List<OlafMatch> lastHits  = hitlist.subList(hitlist.size()-partListLength, hitlist.size());
			 
			//find the first x1 where delta t is equals to the median delta t
			 float y1 = mostCommonDeltaTforHitList(firstHits);
			 float x1 = 0;
			 for(int i = 0 ; i < firstHits.size() ; i++) {
				 OlafMatch hit = firstHits.get(i);
				 int diff = hit.deltaT();
				 if(diff == y1) {
					 x1 = hit.queryTime;
					 break;
				 }
			 }

			//find the first x2 where delta t is equals to the median delta t
			 float y2 = mostCommonDeltaTforHitList(lastHits);
			 float x2 = 0;
			 for(int i = lastHits.size() - 1 ; i >= 0 ; i--) {
				 OlafMatch hit = lastHits.get(i);
				 int diff = hit.deltaT();
				 if(diff == y2) {
					 x2 = hit.queryTime;
					 break;
				 }
			 }
			 
			 float slope = (y2-y1)/(x2-x1);
			 float offset = -x1 * slope + y1;
			 float timeFactor = 1-slope;

			 //System.out.printf("slope %f  offset %f (blocks) time factor %f (percentage) hit list size %d , last hit list size %d, first hit list size %d, id %d\n",slope,offset,timeFactor, hitlist.size(), lastHits.size(), firstHits.size() , identifier);
			 
			 //threshold in time bins
			 double threshold = Config.getFloat(Key.OLAF_QUERY_RANGE);
			 
			 //only continue processing when time factor is reasonable
			 if(timeFactor > Config.getFloat(Key.OLAF_MIN_TIME_FACTOR) && timeFactor < Config.getFloat(Key.OLAF_MAX_TIME_FACTOR)) {
				 List<OlafMatch> filteredHits = new ArrayList<>();
				 
				 hitlist.forEach( hit ->{				 
					 float yActual = hit.deltaT();
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
					 float minDuration = Config.getFloat(Key.OLAF_MIN_MATCH_DURATION);
					 float queryStart = blocksToSeconds(filteredHits.get(0).queryTime);
					 float queryStop = blocksToSeconds(filteredHits.get(filteredHits.size()-1).queryTime);
					 float duration = queryStop - queryStart;
					 System.out.printf("Matches %d (id) Filtered hits: %d (#) query start %.2f (s) , query stop %.2f (s) \n",identifier, filteredHits.size(),queryStart,queryStop);

					 if(duration >= minDuration) {
						 int score = filteredHits.size();
						 float frequencyFactor = 1.0f;
						 
						 float refStart = blocksToSeconds(filteredHits.get(0).matchTime);
						 float refStop =  blocksToSeconds(filteredHits.get(filteredHits.size()-1).matchTime);
						 
						 //retrieve meta-data
						 OlafResourceMetadata metadata = db.getMetadata((long) identifier);
						 String refPath = "metadata unavailable!";
						 if(metadata != null )
							 refPath = metadata.path;
						 
						 //Construct a histogram with the number of matches for each second
						 //Ideally there is a more or less equal number of matches each second
						 // note that the last second might not be a full second
						 TreeMap<Integer,Integer> matchesPerSecondHistogram = new TreeMap<>();
						 for(OlafMatch hit : filteredHits) {
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
						 float percentOfSecondsWithMatches = 1 - (emptySeconds / numberOfMatchingSeconds);
						 
						 if(percentOfSecondsWithMatches >= Config.getFloat(Key.OLAF_MIN_SEC_WITH_MATCH)){
						 	QueryResult r = new QueryResult(queryPath,queryStart, queryStop, refPath, "" + identifier, refStart, refStop,  score, timeFactor, frequencyFactor,percentOfSecondsWithMatches);
						 	queryResults.add(r);
						 }
					 }
				 }
			 }
		 });

		 //fallback to simple histogram method
		if (queryResults.isEmpty() && Config.getBoolean(Key.OLAF_MATCH_FALLBACK_TO_HIST)) {
			int histogramBinSize = 5;
			hitsPerIdentifer.forEach((identifier, hitlist) -> {
				Map<Integer,Integer> countPerDiff = new HashMap<>();
				hitlist.forEach((hit)->{
					//Histogram per 5 time bins to allow some variation in tdiff
					int deltaT = hit.deltaT() / histogramBinSize;
					if(!countPerDiff.containsKey(deltaT)) countPerDiff.put(deltaT, 0);
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

				final int mostDeltaT = mostCommonDeltaT * histogramBinSize;
				List<OlafMatch> filteredHits = new ArrayList<>();
				if(maxCount > minimumUnfilteredHits){
					hitlist.forEach( hit ->{
						if( Math.abs(mostDeltaT - hit.deltaT() ) <= histogramBinSize)
							filteredHits.add(hit);
					});
				}



				if(filteredHits.size() > minimumFilteredHits) {
					float minDuration = Config.getFloat(Key.OLAF_MIN_MATCH_DURATION);
					float queryStart = blocksToSeconds(filteredHits.get(0).queryTime);
					float queryStop = blocksToSeconds(filteredHits.get(filteredHits.size() - 1).queryTime);
					float duration = queryStop - queryStart;
					System.out.printf("Matches %d (id) Filtered hits: %d (#) query start %.2f (s) , query stop %.2f (s) \n", identifier, filteredHits.size(), queryStart, queryStop);

					if (duration >= minDuration) {
						int score = filteredHits.size();
						float frequencyFactor = 1.0f;

						float refStart = blocksToSeconds(filteredHits.get(0).matchTime);
						float refStop = blocksToSeconds(filteredHits.get(filteredHits.size() - 1).matchTime);

						//retrieve meta-data
						OlafResourceMetadata metadata = db.getMetadata((long) identifier);
						String refPath = "metadata unavailable!";
						if (metadata != null)
							refPath = metadata.path;

						//Construct a histogram with the number of matches for each second
						//Ideally there is a more or less equal number of matches each second
						// note that the last second might not be a full second
						TreeMap<Integer, Integer> matchesPerSecondHistogram = new TreeMap<>();
						for (OlafMatch hit : filteredHits) {
							//if(hit.identifier!= FileUtils.getIdentifier(queryPath))
							//System.out.printf("%d %d %d %d %d\n", hit.identifier, hit.matchTime, hit.queryTime, hit.originalHash, hit.matchedNearHash);
							float offsetInSec = blocksToSeconds(hit.matchTime) - refStart;
							int secondBin = (int) offsetInSec;
							if (!matchesPerSecondHistogram.containsKey(secondBin))
								matchesPerSecondHistogram.put(secondBin, 0);
							matchesPerSecondHistogram.put(secondBin, matchesPerSecondHistogram.get(secondBin) + 1);
						}

						//number of seconds bins
						float numberOfMatchingSeconds = (float) Math.ceil(refStop - refStart);
						float emptySeconds = numberOfMatchingSeconds - matchesPerSecondHistogram.size();
						float percentOfSecondsWithMatches = 1 - (emptySeconds / numberOfMatchingSeconds);

						if (percentOfSecondsWithMatches >= Config.getFloat(Key.OLAF_MIN_SEC_WITH_MATCH) ) {
							QueryResult r = new QueryResult(queryPath, queryStart, queryStop, refPath, "" + identifier, refStart, refStop, score, 1.0f, frequencyFactor, percentOfSecondsWithMatches);
							queryResults.add(r);
						}
					}
				}
			});
		}
		 
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

	@Override
	public void monitor(String query, int maxNumberOfResults, Set<Integer> avoid, QueryResultHandler handler) {
		int overlapInSeconds = Config.getInt(Key.MONITOR_OVERLAP); // 5
		int stepSizeInSeconds = Config.getInt(Key.MONITOR_STEP_SIZE); //25

		// Get the total duration efficiently
		double totalDuration = AudioFileUtils.audioFileDurationInSeconds(new File(query));
		
		//Steps: 0-25s ; 20-45s ; 40-65s ...
		int actualStep = stepSizeInSeconds - overlapInSeconds;//20s
		for(int t = 0 ; t + stepSizeInSeconds < totalDuration; t += actualStep ) {			
			query(query,maxNumberOfResults,avoid,handler,t,stepSizeInSeconds);
		}
	}

	@Override
	public boolean hasResource(String resource) {
		int identifier = FileUtils.getIdentifier(resource);
		final OlafStorage db = getStorage();
		return db.getMetadata(identifier) != null;
	}

	@Override
	public boolean isStorageAvailable() {
		return true;
	}


	@Override
	public void printStorageStatistics() {
		final OlafStorage db = getStorage();
		db.printStatistics(true);
	}

	@Override
	public String resolve(String filename) {
		return "" + FileUtils.getIdentifier(filename);
	}
	
	private List<long[]> readFingerprintFile(String fingerprintFilePath) {
		List<long[]> prints = new ArrayList<>();
		try {
			OlafStorageFile fileDb = OlafStorageFile.getInstance();
			final File file = new File(fingerprintFilePath);
			FileReader fileReader = new FileReader(file);
			final BufferedReader reader = new BufferedReader(fileReader);
			String inputLine = reader.readLine();
			while (inputLine != null) {
				long[] data = fileDb.dataFromLine(inputLine);
				prints.add(data);
				inputLine = reader.readLine();
			}
			reader.close();
		} catch (final IOException i1) {
			System.err.println("Can't open file:" + fingerprintFilePath);
			i1.printStackTrace();
		}
		return prints;
	}

	/**
	 * Load cached fingerprints into the key value store
	 */
	public void load() {
		OlafStorage db = OlafStorageKV.getInstance();
		OlafStorage fileDb = OlafStorageFile.getInstance();
		
		String folder = Config.get(Key.OLAF_CACHE_FOLDER);
		folder = FileUtils.expandHomeDir(folder);
		
		List<String> tdbFiles =  FileUtils.glob(folder,".*.tdb", false);
		
		int index = 1;
		
		for(String fingerprintFilePath : tdbFiles) {
			
			int resourceIdentifier = Integer.valueOf(FileUtils.basename(fingerprintFilePath).replace(".tdb",""));
			
			if(null != db.getMetadata(resourceIdentifier)) {
				System.out.printf("%d/%d SKIPPED %s, db already contains resource %d\n",index,tdbFiles.size(),fingerprintFilePath,resourceIdentifier);
				continue;
			}
			
			List<long[]> fingerprints = readFingerprintFile(fingerprintFilePath);
			for(long[] fingerprintData : fingerprints) {
				db.addToStoreQueue(fingerprintData[0], (int) fingerprintData[1], (int) fingerprintData[2]);
			}
			
			String metaDataFilePath = FileUtils.combine(folder,String.format("%d_meta_data.txt", resourceIdentifier));
			if(FileUtils.exists(metaDataFilePath)) {
				db.processStoreQueue();
				
				OlafResourceMetadata metaData = fileDb.getMetadata(resourceIdentifier);
				db.storeMetadata(resourceIdentifier, metaData.path, (float) metaData.duration, metaData.numFingerprints);
				//FileUtils.rm(metaDataFilePath);
				//FileUtils.rm(fingerprintFilePath);
				System.out.printf("%d/%d Stored %d fingerprints and meta-data for resource %d \n",index,tdbFiles.size(),fingerprints.size(),resourceIdentifier);
			}else {
				db.clearStoreQueue();
				System.out.printf("%d/%d DID NOT STORE FINGERPRINTS: Could not find meta data file for %d, expected a file at: %s\n",index,tdbFiles.size(),resourceIdentifier,metaDataFilePath);
			}
			
			index++;
		}
	}
	
	private void addToMap(TreeMap<Integer,float[]> map,int t,int f,float m) {
		if(!map.containsKey(t)) {
			map.put(t, new float[Config.getInt(Key.OLAF_SIZE)/2]);
		}
		map.get(t)[f]=m;
	}

	public void print(String path, boolean sonicVisualizerOutput, boolean printOnlyEPs) {
		if(printOnlyEPs) {
			List<OlafEventPoint> eventPoints = toEventpoints(path);

			System.out.println("Time (step), Frequency (bin), Magnitude, Time (s), Frequency (Hz)");
			for(OlafEventPoint ep : eventPoints){
				System.out.printf("%d, %d, %.6f, %.6f, %.3f\n",ep.t, ep.f, ep.m,blocksToSeconds(ep.t),binToHz(ep.f));
			}
		} else if(sonicVisualizerOutput) {
			List<OlafFingerprint> prints = toFingerprints(path);
			TreeMap<Integer,float[]> timeIndexedSpectralPeaks = new TreeMap<>();
			for(OlafFingerprint print : prints) {
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
					sb.append(spectrum[i]).append(",");
				}
				sb.append("\n");
			}
			System.out.println(sb);
			
		}else {
			OlafStorageFile db = OlafStorageFile.getInstance();
			int resourceID = FileUtils.getIdentifier(path);
			List<OlafFingerprint> prints = toFingerprints(path);
			for(OlafFingerprint print : prints) {
				long hash = print.hash();			
				int printT1 = print.t1;
				db.addToStoreQueue(hash, resourceID, printT1);
			}
			String printString = db.storeQueueToString();
			System.out.print(printString);
		}
		
	}

	@Override
	public String toJson(String path) {
		List<OlafFingerprint> prints = toFingerprints(path);
		List<OlafEventPoint> eventPoints = toEventpoints(path);
		
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"path\": \"").append(path.replace("\\", "\\\\").replace("\"", "\\\"")).append("\",\n");
		json.append("  \"eventPoints\": [\n");
		
		boolean first = true;
		for(OlafEventPoint ep : eventPoints) {
			if (!first) {
				json.append(",\n");
			}
			first = false;
			
			json.append("    {\n");
			json.append("      \"t\": ").append(ep.t).append(",\n");
			json.append("      \"f\": ").append(ep.f).append(",\n");
			json.append("      \"m\": ").append(ep.m).append(",\n");
			json.append("      \"timeSeconds\": ").append(blocksToSeconds(ep.t)).append(",\n");
			json.append("      \"frequencyHz\": ").append(binToHz(ep.f)).append("\n");
			json.append("    }");
		}
		
		json.append("\n  ],\n");
		json.append("  \"fingerprints\": [\n");
		
		first = true;
		for(OlafFingerprint print : prints) {
			if (!first) {
				json.append(",\n");
			}
			first = false;
			
			json.append("    {\n");
			json.append("      \"t1\": ").append(print.t1).append(",\n");
			json.append("      \"f1\": ").append(print.f1).append(",\n");
			json.append("      \"m1\": ").append(print.m1).append(",\n");
			json.append("      \"t2\": ").append(print.t2).append(",\n");
			json.append("      \"f2\": ").append(print.f2).append(",\n");
			json.append("      \"m2\": ").append(print.m2).append(",\n");
			json.append("      \"m3\": ").append(print.m3).append(",\n");
			json.append("      \"hash\": ").append(print.hash()).append("\n");
			json.append("    }");
		}
		
		json.append("\n  ]\n");
		json.append("}\n");
		
		return json.toString();
	}

	@Override
	public void clear() {
		getStorage().clear();
	}

	@Override
	public String metadata(String path) {
		final OlafStorage db = getStorage();
		long identifier = FileUtils.getIdentifier(path);
		OlafResourceMetadata metaData = db.getMetadata(identifier);
		return String.format("%d ; %s ; %.3f (s) ; %d (#) ; %.3f (#/s)",metaData.identifier,metaData.path,metaData.duration,metaData.numFingerprints,metaData.printsPerSecond());
	}
}
