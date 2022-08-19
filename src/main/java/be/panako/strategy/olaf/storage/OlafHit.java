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

/**
 * A data class containing a store hit of a hash with a near hash in the key value store.
 */
public class OlafHit{
	/**
	 * The hash of the extracted fingerprint
	 */
	public final long originalHash;

	/**
	 * The hash of the matched fingerprint in the store
	 */
	public final long matchedNearHash;

	/**
	 * The time when the fingerprint was present in the indexed audio
	 */
	public final int t;

	/**
	 * The internal identifier of the indexed audio
	 */
	public final int resourceID;

	/**
	 * Creates a new hit of an extracted fingerprint with a fingerprint in the key-value store.
	 *
	 * @param originalHash The hash of the extracted fingerprint
	 * @param matchedNearHash The hash of the matched fingerprint in the store
	 * @param t The time when the fingerprint was present in the indexed audio
	 * @param resourceID The internal identifier of the indexed audio
	 */
	public OlafHit(long originalHash, long matchedNearHash,long t, long resourceID) {
		this.originalHash = originalHash;
		this.matchedNearHash = matchedNearHash;
		this.t=(int)t;
		this.resourceID=(int)resourceID;
	}
	
}
