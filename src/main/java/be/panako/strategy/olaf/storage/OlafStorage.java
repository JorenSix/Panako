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


package be.panako.strategy.olaf.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The interface to a storage system.
 */
public interface OlafStorage {

	/**
	 * Store meta-data for a resource
	 * @param resourceID The identifier of the resource
	 * @param resourcePath The path of the resource
	 * @param duration The duration in seconds
	 * @param numberOfFingerprints The number of fingerprints extracted
	 */
	void storeMetadata(long resourceID, String resourcePath, float duration, int numberOfFingerprints);

	/**
	 * Storing fingerprint hashes goes in batches,
	 * @param fingerprintHash The fingerprint hash
	 * @param resourceIdentifier The internal identifier of the resource
	 * @param t1 The time associated with the fingerprint
	 */
	void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1);

	/**
	 * Actually store the queued fingerprint hashes
	 */
	void processStoreQueue();

	/**
	 * Clears the currently pending fingerprints to store
	 */
	void clearStoreQueue();

	/**
	 * Print the storage statistics.
	 * @param printDetailedStats print additional details or not
	 */
	void printStatistics(boolean printDetailedStats);


	/**
	 * Return meta-data for an identifier
	 * @param identifier The internal identifier.
	 * @return The associated meta-data or null.
	 */
	OlafResourceMetadata getMetadata(long identifier);

	/**
	 * Query operations are done in batches this method adds a fingerprint hash to the query queue
	 * @param queryHash The hash to add to the queue
	 */
	void addToQueryQueue(long queryHash);

	/**
	 * Actually query the database for the queued fingerprint hashes.
	 * @param matchAccumulator Add the matches to this list
	 * @param range The range determines how much the reference hashes might differ from the query hash
	 * @param resourcesToAvoid For deduplication it might be of interest to filter out some resources.
	 */
	void processQueryQueue(Map<Long,List<OlafHit>> matchAccumulator,int range,Set<Integer> resourcesToAvoid);


	/**
	 * Adds a fingerprint hash to the delete queue.
	 * @param fingerprintHash The hash to delete
	 * @param resourceIdentifier The associated resource identifier
	 * @param t1 The associated time index.
	 */
	void addToDeleteQueue(long fingerprintHash, int resourceIdentifier, int t1);

	/**
	 * Actually delete all queued hashes.
	 */
	void processDeleteQueue();

	/**
	 * Delete the meta-data associated with this resource identifier
	 * @param resourceID The internal resource identifier.
	 */
	void deleteMetadata(long resourceID);

	/**
	 * Trie to delete everything from the database
	 */
	void clear();


}
