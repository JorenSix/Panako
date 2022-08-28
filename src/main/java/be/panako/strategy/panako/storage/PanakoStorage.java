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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The interface to a storage system for the Panako algorithm
 */
public interface PanakoStorage {


	/**
	 * Store meta data for a resource
	 * @param resourceID The internal identifier
	 * @param resourcePath the original path
	 * @param duration The duration in seconds
	 * @param fingerprints the amount of fingerprints extracted
	 */
	void storeMetadata(long resourceID, String resourcePath, float duration, int fingerprints);

	/**
	 * For efficiency reasons storing fingerprints is done in batches.
	 * With this method a single fingerprint is added to the batch. To actually store
	 * the fingerprints call processStoreQueue()
	 * @param fingerprintHash The hash of the fingerprint
	 * @param resourceIdentifier The internal identifier of the resource.
	 * @param t1 The time index at which the fingerprint was extracted
	 * @param f1 The frequency bin at which the fingerprint was extracted
	 */
	void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1, int f1);

	/**
	 * For efficiency reasons storing fingerprints is done in batches. This method is called to process and
	 * store the fingerprints in the queue.
	 */
	void processStoreQueue();

	/**
	 * Return the meta-data for a resource with a certain internal resource identifier.
	 * @param identifier The resource identifier.
	 * @return The meta-data for a resource
	 */
	PanakoResourceMetadata getMetadata(long identifier);

	/**
	 * Print statistics for the storage engine
	 * @param detailedStats Print in detail or not
	 */
	void printStatistics(boolean detailedStats);

	/**
	 * Delete the meta-data for a certain resource.
	 * @param resourceID The resource identifier to delete meta-data for.
	 */
	void deleteMetadata(long resourceID);


	/**
	 * Add a fingerprint hash to a queue to process in one go.
	 * For efficiency reasons.
	 * @param queryHash The fingerprint hash extracted from the query
	 */
	void addToQueryQueue(long queryHash);

	/**
	 * Query each fingerprint hash in the queue and add hits to the accumulator.
	 * @param matchAccumulator The list to add matches to.
	 * @param range The range determines how many hashes are allowed to differ
	 */
	void processQueryQueue(Map<Long,List<PanakoHit>> matchAccumulator,int range);

	/**
	 * Query each fingerprint hash in the queue and add hits to the accumulator.
	 * @param matchAccumulator The list to add matches to.
	 * @param range The range determines how many hashes are allowed to differ
	 * @param resourcesToAvoid The resource identifiers to avoid in the return set. This can be used for deduplication
	 *                         purposes: the resource itself should be ignored then.
	 */
	void processQueryQueue(Map<Long,List<PanakoHit>> matchAccumulator,int range,Set<Integer> resourcesToAvoid);

	/**
	 * Removes a fingerprint from the database. Similar to the store operation this is done in batches.
	 * @param fingerprintHash The hash to delete
	 * @param resourceIdentifier The resource id to remove
	 * @param t1 The associated time index
	 * @param f1 The associated frequency bin.
	 */
	void addToDeleteQueue(long fingerprintHash, int resourceIdentifier, int t1,int f1);

	/**
	 * Effectively delete all fingerprints in the queue.
	 */
	void processDeleteQueue();

	/**
	 * Clear the whole database!
	 */
	void clear();




}
