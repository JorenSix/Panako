package be.panako.strategy.olaf.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OlafStorageMemory implements OlafStorage {

	/**
	 * The single instance of the storage.
	 */
	private static OlafStorageMemory instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static OlafStorageMemory getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new OlafStorageMemory();
				}
			}
		}
		return instance;
	}
	
	
	private final TreeMap<Long, List<int[]>> fingerprints;
	private final HashMap<Long, OlafResourceMetadata> resourceMap;
	
	final Map<Long,List<Long>> queryQueue;
	
	public OlafStorageMemory() {
		fingerprints = new TreeMap<>();
		resourceMap = new HashMap<>();
		queryQueue = new HashMap<Long,List<Long>>();
	}
	
	@Override
	public void storeMetadata(long resourceID, String resourcePath, float duration, int fingerprints) {
		OlafResourceMetadata r = new OlafResourceMetadata();
		r.duration = duration;
		r.numFingerprints = fingerprints;
		r.path = resourcePath;
		r.identifier = (int) resourceID;
		
		resourceMap.put(resourceID, r);
	}
	
	@Override
	public OlafResourceMetadata getMetadata(long identifier) {
		return resourceMap.get(identifier);
	}
	

	@Override
	public void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1) {
		int[] val = {resourceIdentifier,t1};
		if(!fingerprints.containsKey(fingerprintHash)) {
			List<int[]> list = new ArrayList<int[]>();
			fingerprints.put(fingerprintHash,list);
		}
		fingerprints.get(fingerprintHash).add(val);
	}

	@Override
	public void processStoreQueue() {
		//NOOP
	}
	
	public void addToQueryQueue(long queryHash) {
		long threadID = Thread.currentThread().getId();
		if(!queryQueue.containsKey(threadID))
			queryQueue.put(threadID, new ArrayList<Long>());
		queryQueue.get(threadID).add(queryHash);
	}

	public void processQueryQueue(Map<Long,List<OlafHit>> matchAccumulator,int range) {
		processQueryQueue(matchAccumulator, range, new HashSet<Integer>());
	}
	
	public void processQueryQueue(Map<Long,List<OlafHit>> matchAccumulator,int range,Set<Integer> resourcesToAvoid) {
		if (queryQueue.isEmpty())
			return;
		
		long threadID = Thread.currentThread().getId();
		if(!queryQueue.containsKey(threadID))
			return;
		
		List<Long> queue = queryQueue.get(threadID);
		
		if (queue.isEmpty())
			return;
		
		for (long originalKey : queue) {
			long startKey = originalKey - range;
			long stopKey = originalKey + range;
			for (long key = startKey; key <= stopKey; key++) {
				List<int[]> results = fingerprints.get(key);
				if (results != null) {
					for (int[] result : results) {
						if (!matchAccumulator.containsKey(originalKey))
							matchAccumulator.put(originalKey, new ArrayList<OlafHit>());
						long fingerprintHash = key;
						long resourceID = result[0];
						long t = result[1];
						if(!resourcesToAvoid.contains((int) resourceID))
						matchAccumulator.get(originalKey)
								.add(new OlafHit(originalKey, fingerprintHash, t, resourceID));
					}
				}
			}
		}
		queue.clear();
	}
}
