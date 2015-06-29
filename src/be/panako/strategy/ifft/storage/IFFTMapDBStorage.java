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



package be.panako.strategy.ifft.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.logging.Logger;

import org.mapdb.Atomic;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple3;

import be.panako.cli.Panako;
import be.panako.strategy.ifft.IFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.panako.util.StopWatch;

public class IFFTMapDBStorage {
	private final static Logger LOG = Logger.getLogger(IFFTMapDBStorage.class.getName());

	/**
	 * The single instance of the storage.
	 */
	private static IFFTMapDBStorage instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static IFFTMapDBStorage getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new IFFTMapDBStorage();
				}
			}
		}
		return instance;
	}
	
	

	private final ConcurrentNavigableMap<Integer, String> audioNameStore;

	private final NavigableSet<Fun.Tuple3<Integer, Integer, Integer>> fftFingerprintStore;

	private final Atomic.Long secondsCounter;

	private final DB db;


	public IFFTMapDBStorage() {
		final String audioStore = "audio_store";
		final String fftStore = "fft_store";
		
		File dbFile = new File(Config.get(Key.NFFT_MAPDB_DATABASE));
		
		// If the current application writes to the storage of if a new database has to be created
		// create the stores.
		if (Panako.getCurrentApplication().writesToStorage() && !dbFile.exists()) {
			// Check for and create a lock.
			checkAndCreateLock(dbFile);
			db = DBMaker.newFileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.make();
			
			// The meta-data store.
			audioNameStore = db.createTreeMap(audioStore)
			.counterEnable() // enable size counter
			.makeOrGet();
			
			// The fingerprint store.
			fftFingerprintStore = db.createTreeSet(fftStore)
					.counterEnable() // enable size counter
					.serializer(BTreeKeySerializer.TUPLE3)
					.makeOrGet();
			
			// Create or get an atomic long
			secondsCounter = db.getAtomicLong("seconds_counter");
		} else if(Panako.getCurrentApplication().needsStorage()){
			// read only
			db = DBMaker.newFileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.readOnly() // make the database read only
					.make();

			audioNameStore = db.getTreeMap(audioStore);
			fftFingerprintStore = db.getTreeSet(fftStore);
			secondsCounter = db.getAtomicLong("seconds_counter");
		}else{
			// no database is needed
			secondsCounter = null;
			audioNameStore = null;
			fftFingerprintStore = null;
			db = null;
		}
	}

	private void checkAndCreateLock(File dbFile) {
		// Multiple processes should not write to the same database
		// Check for a lock and quit if there is one.
		if (FileUtils.isFileLocked(dbFile.getAbsolutePath())) {
			String message = "The database is locked.\nMultiple processes should not write to the same database at the same time.\n"
					+ "If no other processes use the database, remove '"
					+ FileUtils.getLockFileName(dbFile.getAbsolutePath())
					+ "' manually.";
			System.out.println(message);
			System.err.println(message);
			LOG.severe(message);
			throw new RuntimeException(message);
		}

		// Create a lock, quit if there is a problem creating the lock.
		if (!FileUtils.createLock(dbFile.getAbsolutePath())) {
			String message = "Could not create a lock file for the database. \n"
					+ "Please make sure that '"
					+ FileUtils.getLockFileName(dbFile.getAbsolutePath())
					+ "' is writable.";
			System.out.println(message);
			System.err.println(message);
			LOG.severe(message);
			throw new RuntimeException(message);
		}
	}

	public void addAudio(int identifier, String description) {
		audioNameStore.put(identifier, description);
	}


	public void audioObjectAdded(int numberOfSeconds) {
		secondsCounter.addAndGet(numberOfSeconds);
		db.commit();
	}

	public int getNumberOfFingerprints() {
		return fftFingerprintStore.size();
	}
	
	public void checkNumberOfFingerprints() {

    	Iterator<Tuple3<Integer,Integer,Integer>> it = fftFingerprintStore.iterator();
    	int counter = 0 ; 
    	while(it.hasNext()){
    		counter++;
    		it.next();
    	}
    	assert counter == getNumberOfFingerprints();
	}


	public String getAudioDescription(int identifier) {
		return audioNameStore.get(identifier);
	}

	public int getNumberOfAudioObjects() {
		return audioNameStore.size();
	}

	public double getNumberOfSeconds() {
		return secondsCounter.get();
	}

	public boolean hasDescription(String description) {
		int indentifier = FileUtils.getIdentifier(description);
		return description.equals(getAudioDescription(indentifier));
	}

	public float addFingerprint(int identifier, int time, int landmarkHash) {
		fftFingerprintStore.add(Fun.t3(landmarkHash, time, identifier));
		return 0.0f;
	}
	
	
public List<IFFTFingerprintQueryMatch> getMatches(List<IFFTFingerprint> fingerprints, int size) {
	
		StopWatch w = new StopWatch();
		Set<IFFTFingerprintHit> allHits = new HashSet<IFFTFingerprintHit>();	
	    for(IFFTFingerprint fingerprint: fingerprints){
	    	int hash = fingerprint.hash();
	    	Tuple3<Integer,Integer,Integer> fromElement = Fun.t3(hash, null, null);
	    	Tuple3<Integer,Integer,Integer> toElement = Fun.t3(hash,Integer.MAX_VALUE, Integer.MAX_VALUE);
	    	
	    	Iterator<Tuple3<Integer,Integer,Integer>> it = fftFingerprintStore.subSet(fromElement, toElement).iterator();
	    	while(it.hasNext()){
	    		Tuple3<Integer,Integer,Integer> hit = it.next();
	    		IFFTFingerprintHit lh = new IFFTFingerprintHit();
				int queryTime = fingerprint.t1;//queryTimeForHash.get(landmarkHash);
				lh.matchTime = hit.b;
				lh.identifier = hit.c;
				lh.timeDifference = lh.matchTime - queryTime;
				lh.queryTime = queryTime;
				allHits.add(lh);
	    	}
	    }
	    LOG.info(String.format("MapDB answerd to query of %d hashes in %s and found %d hits.", fingerprints.size(),w.formattedToString(),allHits.size()));
		
	   
	    HashMap<Integer,List<IFFTFingerprintHit>> hitsPerIdentifer = new HashMap<Integer, List<IFFTFingerprintHit>>();
		for(IFFTFingerprintHit hit : allHits){
			if(!hitsPerIdentifer.containsKey(hit.identifier)){
				hitsPerIdentifer.put(hit.identifier, new ArrayList<IFFTFingerprintHit>());
			}
			List<IFFTFingerprintHit> hitsForIdentifier = hitsPerIdentifer.get(hit.identifier);
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
			for(IFFTFingerprintHit hit : hitsPerIdentifer.get(identifier)){
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
				HashMap<Integer,IFFTFingerprintHit> hitsWithBestOffset = new HashMap<Integer,IFFTFingerprintHit>();
				for(IFFTFingerprintHit hit : hitsPerIdentifer.get(identifier)){
					if(hit.timeDifference == offsetPerIdentifier.get(identifier)){
						hitsWithBestOffset.put(hit.queryTime,hit);
					}
				}
				List<IFFTFingerprintHit> hitsToSortyByQueryTime = new ArrayList<IFFTFingerprintHit>(hitsWithBestOffset.values());
				List<IFFTFingerprintHit> hitsToSortyByReferenceTime = new ArrayList<IFFTFingerprintHit>(hitsWithBestOffset.values());
				Collections.sort(hitsToSortyByQueryTime,new Comparator<IFFTFingerprintHit>() {
					@Override
					public int compare(IFFTFingerprintHit o1,
							IFFTFingerprintHit o2) {
						return Integer.valueOf(o1.queryTime).compareTo(o2.queryTime);
					}
				});
				Collections.sort(hitsToSortyByReferenceTime,new Comparator<IFFTFingerprintHit>() {
					@Override
					public int compare(IFFTFingerprintHit o1,
							IFFTFingerprintHit o2) {
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
		
		
		List<IFFTFingerprintQueryMatch> matches = new ArrayList<IFFTFingerprintQueryMatch>();
		for(Integer alignedOffsets : scoreOderedPerIdentifier.keySet()){
			List<Integer> identifiers = scoreOderedPerIdentifier.get(alignedOffsets);
			for(Integer identifier : identifiers){
				IFFTFingerprintQueryMatch match = new IFFTFingerprintQueryMatch();
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
