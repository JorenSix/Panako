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

/**
 * A hit describes a match between a fingerprint and a near match in the reference database.
 *
 */
public class PanakoHit{
	/**
	 * The fingerprint hash that was used in the query
	 */
	public final long originalHash;

	/**
	 * The near fingerprint hash found in the reference database
	 */
	public final long matchedNearHash;

	/**
	 * The time index in the
	 */
	public final int t;

	/**
	 * The frequency index at the matching location
	 */
	public final int f;

	/**
	 * The internal resource identifier
	 */
	public final int resourceID;

	/**
	 * Create a new hit between an extracted hash and a matched
	 * near hash in the reference database
	 * @param originalHash The original extracted hash from the query
	 * @param matchedNearHash The matched hash in the reference database.
	 * @param t The time component of the fingerprint in the ref database.
	 * @param resourceID The resource identifier connected to the fingerprint in the ref database.
	 * @param f The frequency component of the fingerprint in the ref database.
	 */
	public PanakoHit(long originalHash, long matchedNearHash,long t, long resourceID, long f) {
		this.originalHash = originalHash;
		this.matchedNearHash = matchedNearHash;
		this.t=(int)t;
		this.f =(int)f;
		this.resourceID=(int)resourceID;
	}
	
}
