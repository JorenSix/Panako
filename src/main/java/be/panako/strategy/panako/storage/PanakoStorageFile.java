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


package be.panako.strategy.panako.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;

/**
 * Stores fingerprints in flat files.
 */
public class PanakoStorageFile implements PanakoStorage {
	private final static Logger LOG = Logger.getLogger(PanakoStorageFile.class.getName());
	
	/**
	 * The single instance of the storage.
	 */
	private static PanakoStorageFile instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static final Object mutex = new Object();

	/**
	 * Uses a singleton pattern.
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static PanakoStorageFile getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new PanakoStorageFile();
				}
			}
		}
		return instance;
	}
	
	
	final Map<Long,List<long[]>> storeQueue;
	final File storeDir;

	/**
	 * Create a new file storage
	 */
	public PanakoStorageFile() {
		String folder = Config.get(Key.PANAKO_CACHE_FOLDER);
		folder = FileUtils.expandHomeDir(folder);
		if(!new File(folder).exists()) {
			FileUtils.mkdirs(folder);
		}
		if(!new File(folder).exists()) {
			throw new RuntimeException("Could not create CACHE folder: " + folder);
		}
		
		storeDir = new File(folder);
		
		storeQueue = new HashMap<Long,List<long[]>>();
	}
	
	
	
	public void storeMetadata(long resourceID,String resourcePath,float duration, int fingerprints) {
		String path = FileUtils.combine(storeDir.getAbsolutePath(),resourceID + "_meta_data.txt");		
		StringBuilder sb = new StringBuilder();		
		sb.append(resourceID).append("\n").append(duration).append("\n").append(fingerprints).append("\n").append(resourcePath).append("\n");	
		FileUtils.writeFile(sb.toString(), path);
		LOG.info(String.format("Stored metadata file '%s'.",path));
	}
	
	@Override
	public PanakoResourceMetadata getMetadata(long identifier) {
		String path = FileUtils.combine(storeDir.getAbsolutePath(),identifier + "_meta_data.txt");
		if(!FileUtils.exists(path)) {
			return null;
		}
		String[] metaDataItems = FileUtils.readFile(path).split("\n");
		
		PanakoResourceMetadata metaData = new PanakoResourceMetadata();
		metaData.duration= Float.valueOf(metaDataItems[1]);
		metaData.numFingerprints= Integer.valueOf(metaDataItems[2]);
		metaData.path= metaDataItems[3];
		
		return metaData;
	}

	@Override
	public void printStatistics(boolean detailedStats) {

	}

	@Override
	public void deleteMetadata(long resourceID) {
		String path = FileUtils.combine(storeDir.getAbsolutePath(),resourceID + "_meta_data.txt");
		FileUtils.rm(path);
		LOG.info(String.format("Deleted metadata file '%s'.",path));
	}


	public void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1,int f1) {
		long[] data = {fingerprintHash,resourceIdentifier,t1,f1};
		long threadID = Thread.currentThread().getId();
		if(!storeQueue.containsKey(threadID))
			storeQueue.put(threadID, new ArrayList<long[]>());
		storeQueue.get(threadID).add(data);
	}

	/**
	 * Takes a list of fingerprint data items and turns it into a string
	 * @param queue The queue with fingerprint data
	 * @return A formatted string used in the file
	 */
	public String storeQueueToString(List<long[]> queue) {		
		//sort by hash asc
		//storeQueue.sort((a, b) -> Long.valueOf(a[0]).compareTo(b[0]));
		StringBuilder sb = new StringBuilder();
		for(long[] data : queue) {
			for(long element : data) {
				sb.append(element).append(" ");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}

	
	public void processStoreQueue() {
		if(storeQueue.isEmpty()) return;
		
		long threadID = Thread.currentThread().getId();
		
		if(!storeQueue.containsKey(threadID)) return;
		
		List<long[]> queue = storeQueue.get(threadID);
		
		if (queue.isEmpty()) return;
		
		int resourceIdentifier = (int) queue.get(0)[1];
		
		// Clears the store queue
		String fingerprintsAsString = storeQueueToString(queue);
		String path = FileUtils.combine(storeDir.getAbsolutePath(),resourceIdentifier + ".tdb");
		FileUtils.writeFile(fingerprintsAsString, path);

		LOG.info(String.format("Stored %d fingerprints in file %s",queue.size(),path));

		// Clears the store queue
		queue.clear();
	}

	/**
	 * Parse a line from a file with fingerprints.
	 * @param line A line from a fingerprint file with "fingerprintHash resourceIdentifier t1 f1" format.
	 * @return The line parsed as a long array.
	 */
	public long[] dataFromLine(String line) {
		String[] data = line.split(" ");
		long[] dataArray = {Long.valueOf(data[0]),Long.valueOf(data[1]),Long.valueOf(data[2]),Long.valueOf(data[3])};
		return dataArray;
	}

	@Override
	public void addToQueryQueue(long queryHash) {

	}
	
	@Override
	public void processQueryQueue(Map<Long, List<PanakoHit>> matchAccumulator, int range) {
	}

	@Override
	public void processQueryQueue(Map<Long, List<PanakoHit>> matchAccumulator, int range,
			Set<Integer> resourcesToAvoid) {

	}

	@Override
	public void addToDeleteQueue(long fingerprintHash, int resourceIdentifier, int t1, int f1) {

	}

	@Override
	public void processDeleteQueue() {

	}

	public void clear() {
		FileUtils.rm(storeDir.getAbsolutePath());

		if(!FileUtils.exists(storeDir.getAbsolutePath()))
			return;
		File[] filesToDelete = storeDir.listFiles();
		for(File f : filesToDelete) {
			FileUtils.rm(f.getAbsolutePath());
		}
		String message = String.format("Removed %d files from file storage",filesToDelete.length);
		LOG.info(message);
		System.out.println(message);
	}
}

