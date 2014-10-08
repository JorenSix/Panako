/***************************************************************************
*                                                                          *                     
* Panako - acoustic fingerprinting                                         *   
* Copyright (C) 2014 - Joren Six / IPEM                                    *   
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


package be.panako.strategy.cteq.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.logging.Logger;

import org.mapdb.Atomic;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple4;

import be.panako.cli.Panako;
import be.panako.strategy.cteq.CteQFingerprint;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.panako.util.StopWatch;

public class CteQMapDBStorage {
	private final static Logger LOG = Logger.getLogger(CteQMapDBStorage.class
			.getName());

	/**
	 * The single instance of the storage.
	 */
	private static CteQMapDBStorage instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static CteQMapDBStorage getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new CteQMapDBStorage();
				}
			}
		}
		return instance;
	}

	private final ConcurrentNavigableMap<Integer, String> audioNameStore;

	private final NavigableSet<Fun.Tuple4<Integer, Integer, Integer, Integer>> cteqFingerprintStore;

	private final Atomic.Long secondsCounter;

	private final DB db;

	private int minimumMatchesThreshold;
	private int minimumAlignedMatchesThreshold;

	public CteQMapDBStorage() {
		File dbFile = null;
		minimumMatchesThreshold = Config.getInt(Key.CTEQ_MINIMUM_MATCHES_THRESHOLD);
		minimumAlignedMatchesThreshold = Config.getInt(Key.CTEQ_MINIMUM_ALIGNED_MATCHES_THRESHOLD);
		
		String mapDBDatabase = Config.get(Key.CTEQ_MAPDB_DATABASE);
		dbFile = new File(mapDBDatabase);

		if (Panako.getCurrentApplication().writesToStorage()) {
			// check for and create a lock.
			checkAndCreateLock(dbFile);
			db = DBMaker.newFileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.make();
		} else {
			// read only
			db = DBMaker.newFileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.readOnly() // make the database read only
					.make();
		}

		secondsCounter = db.getAtomicLong("seconds_counter");

		String audioStore = "audio_store";

		audioNameStore = db.createTreeMap(audioStore)
				.counterEnable() // enable size counter
				.makeOrGet();

		String cteqStore = "cteq_store";
		cteqFingerprintStore = db.createTreeSet(cteqStore)
				.counterEnable() // enable size counter
				.serializer(BTreeKeySerializer.TUPLE4)
				.makeOrGet();
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

	private Comparator<Integer> reverseIntegerOrder = new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o2.compareTo(o1);
		}
	};

	public void audioObjectAdded(int landmarks, int numberOfSeconds) {
		secondsCounter.addAndGet(numberOfSeconds);
		db.commit();
	}

	public int getNumberOfFingerprints() {
		return cteqFingerprintStore.size();

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

	public float addFingerprint(int identifier, int time, int landmarkHash,
			int timeDelta, int frequency) {
		int timeDeltaAndFrequency = timeDelta + (frequency << 16);
		cteqFingerprintStore.add(Fun.t4(landmarkHash, time, identifier,
				timeDeltaAndFrequency));
		return 0.0f;
	}

	public List<CteQFingerprintQueryMatch> getFingerprintMatches(
			List<CteQFingerprint> fingerprints, int size) {

		if (fingerprints.isEmpty()) {
			return new ArrayList<CteQFingerprintQueryMatch>();
		}

		List<Integer> hashes = new ArrayList<Integer>();
		Map<Integer, Integer[]> queryInfoForHash = new HashMap<Integer, Integer[]>();
		// make it a set, so if for some reason fingerprints with the same
		// identifier, frequency, time components are in the database, they are
		// only counted once.
		Set<CteQFingerprintHit> allHits = new HashSet<CteQFingerprintHit>();

		for (CteQFingerprint fingerprint : fingerprints) {
			int fingerprintHash = fingerprint.hash();
			Integer[] queryInfo = { fingerprint.t1, fingerprint.timeDelta(),
					fingerprint.f1 };
			hashes.add(fingerprintHash);
			queryInfoForHash.put(fingerprintHash, queryInfo);
		}

		StopWatch stopWatch = new StopWatch();

		for (CteQFingerprint fingerprint : fingerprints) {
			int hash = fingerprint.hash();
			Tuple4<Integer, Integer, Integer, Integer> fromElement = Fun.t4(
					hash, null, null, null);
			Tuple4<Integer, Integer, Integer, Integer> toElement = Fun.t4(hash,
					Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			Iterator<Tuple4<Integer, Integer, Integer, Integer>> it = cteqFingerprintStore
					.subSet(fromElement, toElement).iterator();
			while (it.hasNext()) {
				Tuple4<Integer, Integer, Integer, Integer> hit = it.next();
				int fingerprintHash = hash;
				Integer[] queryInfo = queryInfoForHash.get(fingerprintHash);
				int queryTime = queryInfo[0];
				int queryTimeDelta = queryInfo[1];
				int queryFrequency = queryInfo[2];
				CteQFingerprintHit lh = new CteQFingerprintHit();

				lh.hash = fingerprintHash;

				lh.matchTime = hit.b;
				lh.identifier = hit.c;
				// Time delta and frequency are packed into one integer.
				// Each have 16 bits at their disposal, so split the
				// values.
				int combinedTimeDeltaAndFrequency = hit.d;
				int reconstructedFrequency = (combinedTimeDeltaAndFrequency >> 16);
				int reconstructedTimeDelta = combinedTimeDeltaAndFrequency
						- (reconstructedFrequency << 16);
				lh.matchTimeDelta = reconstructedTimeDelta;
				lh.matchFrequency = reconstructedFrequency;

				lh.queryTime = queryTime;
				lh.queryFrequency = queryFrequency;
				lh.queryTimeDelta = queryTimeDelta;

				allHits.add(lh);
			}
		}

		long nanosPassed = stopWatch.nanoTicksPassed();

		// First count the number of matching fingerprints.
		HashMap<Integer, Integer> hitCountPerIdentifer = new HashMap<Integer, Integer>();
		for (CteQFingerprintHit hit : allHits) {
			if (!hitCountPerIdentifer.containsKey(hit.identifier)) {
				hitCountPerIdentifer.put(hit.identifier, 0);
			}
			hitCountPerIdentifer.put(hit.identifier,
					hitCountPerIdentifer.get(hit.identifier) + 1);
		}

		// This step removes random chance hash hits.
		// The minimumMatchesThreshold is a configurable threshold,
		// If it is too low, a lot of time will be spent verifying
		// random chance hits, too high and actual matches might be missed.

		// Now store the ones with min x matches (a lot less, in good
		// conditions)
		HashMap<Integer, List<CteQFingerprintHit>> hitsPerIdentifer = new HashMap<Integer, List<CteQFingerprintHit>>();
		for (CteQFingerprintHit hit : allHits) {
			if (hitCountPerIdentifer.get(hit.identifier) > minimumMatchesThreshold) {
				// create a new list if not present
				if (!hitsPerIdentifer.containsKey(hit.identifier)) {
					hitsPerIdentifer.put(hit.identifier,
							new ArrayList<CteQFingerprintHit>());
				}
				// store the match
				List<CteQFingerprintHit> hitsForIdentifier = hitsPerIdentifer
						.get(hit.identifier);
				hitsForIdentifier.add(hit);
			}
		}

		// Holds the maximum number of aligned offsets per identifier
		// The key is the number of aligned offsets. The list contains a list of
		// identifiers.
		// The list will most of the time only contain one entry.
		// The most common offset will be at the top of the list (reversed key
		// order).
		TreeMap<Integer, List<Integer>> scorePerIdentifier = new TreeMap<Integer, List<Integer>>(
				reverseIntegerOrder);

		// A map that contains the most popular offset per identifier
		HashMap<Integer, Integer> timeRatioPerIdentifier = new HashMap<Integer, Integer>();
		// A map that contains the most popular offset per identifier
		HashMap<Integer, Integer> frequencyRatioPerIdentifier = new HashMap<Integer, Integer>();

		// iterate every list per identifier and count the most popular offsets
		for (Integer identifier : hitsPerIdentifer.keySet()) {
			// use this hash table to count the most popular offsets
			HashMap<Integer, List<CteQFingerprintHit>> popularTimeRatioPerIdentifier = new HashMap<Integer, List<CteQFingerprintHit>>();
			// the final score for the identifier
			int maxAlignedTimeRatios = 0;
			int maxAlignedFrequencyRatios = 0;
			// count the aligned time ratio's for each fingerprint hit, remove
			// impossible ratio's
			for (CteQFingerprintHit hit : hitsPerIdentifer.get(identifier)) {
				int hitTimeRatio = (int) hit.timeRatio();
				int hitFrequencyRatio = hit.frequencyRatio();

				// only support reasonable time differences: +- 30%
				boolean reasonableTimeRatio = hitTimeRatio < 130
						&& hitTimeRatio > 70;
				// only support reasonable frequency differences: +- 30% (450
				// cents)
				boolean reasonableFrequencyRatio = hitFrequencyRatio < 130
						&& hitFrequencyRatio > 70;

				if (reasonableTimeRatio && reasonableFrequencyRatio) {
					int offset = hit.roughTimeDifference();
					if (!popularTimeRatioPerIdentifier.containsKey(offset)) {
						popularTimeRatioPerIdentifier.put(offset,
								new ArrayList<CteQFingerprintHit>());
					}
					popularTimeRatioPerIdentifier.get(offset).add(hit);
					int numberOfAlignedOffsets = popularTimeRatioPerIdentifier
							.get(offset).size();
					if (numberOfAlignedOffsets > maxAlignedTimeRatios) {
						maxAlignedTimeRatios = numberOfAlignedOffsets;
						timeRatioPerIdentifier.put(identifier, offset);
					}
				}
			}

			// Threshold on aligned ratios. Ignores identifiers with less than 3
			// aligned fingerprints
			if (maxAlignedTimeRatios > minimumAlignedMatchesThreshold) {

				HashMap<Integer, List<CteQFingerprintHit>> popularFrequencyDifferencePerIdentifier = new HashMap<Integer, List<CteQFingerprintHit>>();

				// do the same thing for the frequency ratio's: if this is a
				// real hit those should match too.
				for (CteQFingerprintHit hit : hitsPerIdentifer.get(identifier)) {
					int hitTimeRatio = (int) hit.timeRatio();
					int hitFrequencyRatio = hit.frequencyRatio();

					// only support reasonable time differences: +- 30%
					boolean reasonableTimeRatio = hitTimeRatio < 130
							&& hitTimeRatio > 70;
					// only support reasonable frequency differences: +- 30%
					// (450 cents)
					boolean reasonableFrequencyRatio = hitFrequencyRatio < 130
							&& hitFrequencyRatio > 70;

					if (reasonableTimeRatio && reasonableFrequencyRatio) {
						int offset = hit.frequencyDifferenceInCents() / 5;
						if (!popularFrequencyDifferencePerIdentifier
								.containsKey(offset)) {
							popularFrequencyDifferencePerIdentifier.put(offset,
									new ArrayList<CteQFingerprintHit>());
						}
						popularFrequencyDifferencePerIdentifier.get(offset)
								.add(hit);
						int numberOfAlignedOffsets = popularFrequencyDifferencePerIdentifier
								.get(offset).size();
						if (numberOfAlignedOffsets > maxAlignedFrequencyRatios) {
							maxAlignedFrequencyRatios = numberOfAlignedOffsets;
							frequencyRatioPerIdentifier.put(identifier, offset);
						}
					}
				}

				int timeRatio = timeRatioPerIdentifier.get(identifier);
				// keeps only the aligned hits!
				List<CteQFingerprintHit> alignedHits = popularTimeRatioPerIdentifier
						.get(timeRatio);
				hitsPerIdentifer.put(identifier, alignedHits);

				int score = Math.min(1, maxAlignedFrequencyRatios)
						* maxAlignedTimeRatios;
				if (!scorePerIdentifier.containsKey(score)) {
					scorePerIdentifier.put(score, new ArrayList<Integer>());
				}
				scorePerIdentifier.get(score).add(identifier);
			}
		}

		List<CteQFingerprintQueryMatch> matches = new ArrayList<CteQFingerprintQueryMatch>();
		for (Integer alignedOffsets : scorePerIdentifier.keySet()) {
			List<Integer> identifiers = scorePerIdentifier.get(alignedOffsets);
			for (Integer identifier : identifiers) {

				double timeRatio = 0;
				double frequencyRatio = 0;
				for (CteQFingerprintHit hit : hitsPerIdentifer.get(identifier)) {
					timeRatio += hit.detailedTimeRatio();
					frequencyRatio += hit.detailedFrequencyRatio();
				}
				timeRatio = timeRatio
						/ (float) hitsPerIdentifer.get(identifier).size();
				frequencyRatio = frequencyRatio
						/ (float) hitsPerIdentifer.get(identifier).size();

				CteQFingerprintQueryMatch match = new CteQFingerprintQueryMatch();
				match.identifier = identifier;
				match.score = alignedOffsets;
				// rough time difference factor
				match.mostPopularOffset = (int) (timeRatioPerIdentifier
						.get(identifier) * 3.0);
				match.timeRatio = timeRatio;
				match.frequencyRatio = frequencyRatio;
				if (matches.size() < size) {
					matches.add(match);
				}
			}
		}
		// between query result and now (after processing result)
		long msPassed = (stopWatch.nanoTicksPassed() - nanosPassed) / 1000000;
		LOG.info("Finished query in " + stopWatch.formattedToString()
				+ " found " + allHits.size() + " hits from "
				+ fingerprints.size() + " landmarks. Resulting in "
				+ matches.size() + " aligned matches. Processing in "
				+ msPassed + "ms");
		return matches;
	}

	public double[] getCollisionInfo() {
		String strategy = Config.get(Key.STRATEGY).trim().toUpperCase();
		double[] info = { 0, 0 };
		double mean = 0.0;
		if (strategy.equals("CTEQ")) {
			int maxCount = 0;
			int hashCount = 1;
			int recordCount = 0;
			int recordCountForIf = 0;// to avoid modulo
			Iterator<Tuple4<Integer, Integer, Integer, Integer>> it = cteqFingerprintStore
					.iterator();
			if (it.hasNext()) {
				int prevHash = it.next().a;
				recordCount++;
				recordCountForIf++;
				int currentHashCount = 1;
				while (it.hasNext()) {
					int currentHash = it.next().a;
					recordCount++;
					recordCountForIf++;
					if (recordCountForIf == 1000000) {
						LOG.info(String
								.format("Iterated  %d of %d records or %.2f%%, found %d hashes",
										recordCount,
										cteqFingerprintStore.size(),
										recordCount
												/ (float) cteqFingerprintStore
														.size() * 100,
										hashCount));
						recordCountForIf = 0;
					}
					if (currentHash == prevHash) {
						currentHashCount++;
					} else {
						maxCount = Math.max(currentHashCount, maxCount);
						prevHash = currentHash;
						mean = mean + (currentHashCount - mean)
								/ (double) hashCount;
						hashCount++;
						currentHashCount = 1;
					}

				}
				info[1] = maxCount;
				info[0] = Math.round(mean * 100) / 100.0;
			}
		} else {

		}
		return info;
	}

	public void polluteDB(final int hashes) {

		Iterator<Tuple4<Integer, Integer, Integer, Integer>> source = new Iterator<Tuple4<Integer, Integer, Integer, Integer>>() {

			long counter = 0;
			int hash = hashes * 4;
			Random rnd = new Random();

			@Override
			public boolean hasNext() {
				return counter < hashes;
			}

			@Override
			public Tuple4<Integer, Integer, Integer, Integer> next() {
				counter++;
				hash = hash - 1 - rnd.nextInt(4);
				return new Fun.Tuple4<>(hash, rnd.nextInt(), rnd.nextInt(),
						rnd.nextInt());
			}

			@Override
			public void remove() {
			}
		};

		/*
		 * source = Pump.sort(source, true, 100000,
		 * Collections.reverseOrder(BTreeMap.COMPARABLE_COMPARATOR), //reverse
		 * order comparator db.getDefaultSerializer() );
		 */

		String cteqStore = "cteq_store";

		if (db.exists(cteqStore)) {
			db.delete(cteqStore);
		}

		db.createTreeSet(cteqStore).counterEnable()
				// enable size counter
				.serializer(BTreeKeySerializer.TUPLE4).pumpSource(source)
				.make();

		db.close();
	}
}
