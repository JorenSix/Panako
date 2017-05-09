package be.panako.strategy.nfft.storage.redisson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.strategy.nfft.storage.NFFTFingerprintHit;
import be.panako.strategy.nfft.storage.NFFTFingerprintQueryMatch;
import be.panako.strategy.nfft.storage.Storage;
import be.panako.util.FileUtils;
import be.panako.util.StopWatch;

public class NFFTRedisStorage implements Storage {
	
	private final static Logger LOG = Logger.getLogger(NFFTRedisStorage.class.getName());
	
	/**
	 * The single instance of the storage.
	 */
	private static Storage instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static Storage getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new NFFTRedisStorage();
				}
			}
		}
		return instance;
	}
	
	private final RMap<Integer, List<Integer>> fingerprintMap;
	private final RMap<Integer, String> metaDataMap;
	private final RAtomicLong secondsStored;
	//private final Random rnd; 
	
	public NFFTRedisStorage() {
		Config config = new Config();
		config.useSingleServer().setAddress("127.0.0.1:6379");
		//config.useSingleServer().setAddress("157.193.92.74:6379");

		final RedissonClient redisson = Redisson.create(config);
		fingerprintMap = redisson.getMap("integerMap");
		metaDataMap = redisson.getMap("descriptionMap");
		
		secondsStored = redisson.getAtomicLong("secondsStoredAtomicLong");
				
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
			@Override
			public void run() {
				redisson.shutdown();
			}}));
		
		//rnd = new Random();
	}
	
	public void clearDatabase(){
		metaDataMap.clear();
		fingerprintMap.clear();
		secondsStored.set(0);
	}

	@Override
	public void addAudio(int identifier, String description) {
		metaDataMap.put(identifier, description);
	}

	@Override
	public void audioObjectAdded(int numberOfSeconds) {
		secondsStored.addAndGet(numberOfSeconds);
	}

	@Override
	public int getNumberOfFingerprints() {
		return fingerprintMap.size();
	}

	@Override
	public String getAudioDescription(int identifier) {
		return metaDataMap.get(identifier);
	}

	@Override
	public int getNumberOfAudioObjects() {
		return metaDataMap.size();
	}

	@Override
	public double getNumberOfSeconds() {
		return secondsStored.get();
	}

	@Override
	public boolean hasDescription(String description) {
		int indentifier = FileUtils.getIdentifier(description);
		return description.equals(getAudioDescription(indentifier));
	}

	@Override
	public float addFingerprint(int identifier, int time, int landmarkHash) {
		List<Integer> list;
		if(fingerprintMap.containsKey(landmarkHash)){
			list = fingerprintMap.get(landmarkHash);
		}else{
			list = new ArrayList<Integer>();
		}
		//store identifier first, then time
		list.add(identifier);
		list.add(time);
		if(list.size()>1500){
			//an even index between 0 and 1498
			//remove? item?
		}
		fingerprintMap.put(landmarkHash,list);
		return 0;
	}

	@Override
	public List<NFFTFingerprintQueryMatch> getMatches(
			List<NFFTFingerprint> fingerprints, int size) {
		StopWatch w = new StopWatch();
		Set<NFFTFingerprintHit> allHits = new HashSet<NFFTFingerprintHit>();	
		try{
		    for(NFFTFingerprint fingerprint: fingerprints){
		    	int hash = fingerprint.hash();
		    	List<Integer> data = fingerprintMap.get(hash);
		    	for(int i = 0 ; i < data.size() -1 ; i+=2){
		    		NFFTFingerprintHit lh = new NFFTFingerprintHit();
					int queryTime = fingerprint.t1;//queryTimeForHash.get(landmarkHash);
					lh.identifier = data.get(i);
					lh.matchTime = data.get(i+1);
					lh.timeDifference = lh.matchTime - queryTime;
					lh.queryTime = queryTime;
					allHits.add(lh);
		    	}
		    }
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	
	    LOG.info(String.format("Redis answered to query of %d hashes in %s and found %d hits.", fingerprints.size(),w.formattedToString(),allHits.size()));
		
	   
	    HashMap<Integer,List<NFFTFingerprintHit>> hitsPerIdentifer = new HashMap<Integer, List<NFFTFingerprintHit>>();
		for(NFFTFingerprintHit hit : allHits){
			if(!hitsPerIdentifer.containsKey(hit.identifier)){
				hitsPerIdentifer.put(hit.identifier, new ArrayList<NFFTFingerprintHit>());
			}
			List<NFFTFingerprintHit> hitsForIdentifier = hitsPerIdentifer.get(hit.identifier);
			hitsForIdentifier.add(hit);
		}
		
		//This could be done in an SQL where clause also (with e.g. a group by identifier /having count(identifier) >= 5 clause)
		//removes random chance hash hits.
		int minMatchingLandmarksThreshold = 3;
		for(Integer identifier: new HashSet<Integer>(hitsPerIdentifer.keySet())){
			if(hitsPerIdentifer.get(identifier).size() < minMatchingLandmarksThreshold){
				hitsPerIdentifer.remove(identifier);
			}
		}
		
		//Holds the maximum number of aligned offsets per identifier
		//The key is the number of aligned offsets. The list contains a list of identifiers. 
		//The list will most of the time only contain one entry.
		//The most common offset will be at the top of the list (reversed integer order).
		TreeMap<Integer,List<Integer>> scorePerIdentifier = new TreeMap<Integer,List<Integer>>(reverseIntegerOrder);
		//A map that contains the most popular offset per identifier
		HashMap<Integer,Integer> offsetPerIdentifier = new HashMap<Integer,Integer>();

		//iterate every list per identifier and count the most popular offsets
		for(Integer identifier: hitsPerIdentifer.keySet()){
			//use this hash table to count the most popular offsets 
			HashMap<Integer,Integer> popularOffsetsPerIdentifier = new HashMap<Integer, Integer>();
			//the final score for the identifier
			int maxAlignedOffsets = 0;
			
			//add the offsets for each landmark hit 
			for(NFFTFingerprintHit hit : hitsPerIdentifer.get(identifier)){
				if(!popularOffsetsPerIdentifier.containsKey(hit.timeDifference)){
					popularOffsetsPerIdentifier.put(hit.timeDifference, 0);	
				}
				int numberOfAlignedOffsets = 1 + popularOffsetsPerIdentifier.get(hit.timeDifference);
				popularOffsetsPerIdentifier.put(hit.timeDifference,numberOfAlignedOffsets);
				if(numberOfAlignedOffsets > maxAlignedOffsets){
					maxAlignedOffsets = numberOfAlignedOffsets;
					offsetPerIdentifier.put(identifier, hit.timeDifference);
				}
			}
			//Threshold on aligned offsets. Ignores identifiers with less than 3 aligned offsets
			if(maxAlignedOffsets > 4){
				if(!scorePerIdentifier.containsKey(maxAlignedOffsets)){
					scorePerIdentifier.put(maxAlignedOffsets, new ArrayList<Integer>());
				}
				scorePerIdentifier.get(maxAlignedOffsets).add(identifier);						
			}
		}
		
		
		//Holds the maximum number of aligned and ordered offsets per identifier
			//The key is the number of aligned offsets. The list contains a list of identifiers. 
			//The list will most of the time only contain one entry.
			//The most common offset will be at the top of the list (reversed integer order).
			TreeMap<Integer,List<Integer>> scoreOderedPerIdentifier = new TreeMap<Integer,List<Integer>>(reverseIntegerOrder);
				
		//check if the order in the query is the same as the order in the reference audio
		for(Integer alignedOffsets : scorePerIdentifier.keySet()){
			List<Integer> identifiers = scorePerIdentifier.get(alignedOffsets);
			for(Integer identifier : identifiers){
				//by making it a set only unique times are left
				HashMap<Integer,NFFTFingerprintHit> hitsWithBestOffset = new HashMap<Integer,NFFTFingerprintHit>();
				for(NFFTFingerprintHit hit : hitsPerIdentifer.get(identifier)){
					if(hit.timeDifference == offsetPerIdentifier.get(identifier)){
						hitsWithBestOffset.put(hit.queryTime,hit);
					}
				}
				List<NFFTFingerprintHit> hitsToSortyByQueryTime = new ArrayList<NFFTFingerprintHit>(hitsWithBestOffset.values());
				List<NFFTFingerprintHit> hitsToSortyByReferenceTime = new ArrayList<NFFTFingerprintHit>(hitsWithBestOffset.values());
				Collections.sort(hitsToSortyByQueryTime,new Comparator<NFFTFingerprintHit>() {
					@Override
					public int compare(NFFTFingerprintHit o1,
							NFFTFingerprintHit o2) {
						return Integer.valueOf(o1.queryTime).compareTo(o2.queryTime);
					}
				});
				Collections.sort(hitsToSortyByReferenceTime,new Comparator<NFFTFingerprintHit>() {
					@Override
					public int compare(NFFTFingerprintHit o1,
							NFFTFingerprintHit o2) {
						return Integer.valueOf(o1.matchTime).compareTo(o2.matchTime);
					}
				});
				
				int countInOrderAlignedHits = 0;
				for(int i = 0 ; i < hitsToSortyByQueryTime.size() ; i++){
					if(hitsToSortyByQueryTime.get(i).equals(hitsToSortyByReferenceTime.get(i))){
						countInOrderAlignedHits++;
					}
				}
				if(countInOrderAlignedHits>4){
					if(!scoreOderedPerIdentifier.containsKey(countInOrderAlignedHits)){
						scoreOderedPerIdentifier.put(countInOrderAlignedHits, new ArrayList<Integer>());
					}
					scoreOderedPerIdentifier.get(countInOrderAlignedHits).add(identifier);	
				}
			}
		}
		
		
		List<NFFTFingerprintQueryMatch> matches = new ArrayList<NFFTFingerprintQueryMatch>();
		for(Integer alignedOffsets : scoreOderedPerIdentifier.keySet()){
			List<Integer> identifiers = scoreOderedPerIdentifier.get(alignedOffsets);
			for(Integer identifier : identifiers){
				NFFTFingerprintQueryMatch match = new NFFTFingerprintQueryMatch();
				match.identifier = identifier;
				match.score = alignedOffsets;
				match.mostPopularOffset = offsetPerIdentifier.get(identifier);
				if(matches.size() < size){
					matches.add(match);
				}
			}
			if(matches.size() >= size){
				break;
			}
		}
		return matches;
	}
	
	private Comparator<Integer> reverseIntegerOrder = new Comparator<Integer>(){
		@Override
		public int compare(Integer o1, Integer o2) {
			return o2.compareTo(o1);
		}
	};

}
