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

import be.panako.util.Config;
import be.panako.util.Key;

/**
 * Describes a fingerprint hit in the store.
 * @author Joren Six
 */
public class CteQFingerprintHit{
	
	/**
	 * The audio identifier
	 */
	public int identifier;
	
	/**
	 * Time in analysis blocks in the original, matched audio.
	 */
	public int matchTime;
	
	/**
	 * The frequency bin index of the matching audio.
	 */
	public int matchFrequency;
	
	/**
	 * The delta, expressed in analysis frames, between the first and third event point.
	 */
	public int matchTimeDelta;
	
	//////////////////////////Query fields////////////////////////
	
	/**
	 * The frequency bin index of the query.
	 */
	public int queryFrequency;
	
	/**
	 * Time in analysis blocks in the query.
	 */
	public int queryTime;
	
	/**
	 * The delta, expressed in analysis frames, between the first and third event point.
	 */
	public int queryTimeDelta;

	/**
	 * The fingerprint hash
	 */
	public int hash;

	
	//////////////////////////Calculated fields////////////////////////
	
	/**
	 * The ratio between the time delta's. It should show 
	 * how much tempo of the song was changed, using a percentage. 
	 * The resolution is 2%. E.g. 110% tells you that query was 10% faster 
	 * than the reference track.
	 * @return The time ratio.
	 */
	public float timeRatio(){
		return matchTimeDelta / (float) queryTimeDelta * 100.0f ;
	}
	
	private static double ln2DividedBy1200 = Math.log(2)/Math.log(Math.E)/1200.0;
	
	/**
	 * The ratio between the reference and query frequency.
	 * @return A ratio that tells how much the query frequency is scaled compared to the reference. E.g. 118% translates to +300 cents.
	 */
	public int frequencyRatio(){
		int frequencyDelta = queryFrequency - matchFrequency;
		float frequencyDeltaInCents = 1200 / Config.getFloat(Key.CTEQ_BINS_PER_OCTAVE) * frequencyDelta;
		//e ^ (c ln(2)/1200)  converts a value c to a ratio
		return Math.round((float) Math.pow(Math.E, frequencyDeltaInCents * ln2DividedBy1200) * 100);
	}
	
	public int timeDifference(){
		//int offset = Math.abs(matchTime - queryTime);
		//return (int) Math.round(offset * timeRatio()/100.0f /8.0f ) * 8;
		return Math.round(matchTime - queryTime * timeRatio()/100.0f);
	}
	
	public int roughTimeDifference(){
		float errorFactorDivider = 3.0f;
		return  Math.round(timeDifference()/errorFactorDivider);
	}
	
	public int frequencyDifferenceInCents(){
		int frequencyDelta = queryFrequency - matchFrequency;
		//assuming 36 bins per octave!
		return Math.round(1200 / Config.getFloat(Key.CTEQ_BINS_PER_OCTAVE) * frequencyDelta);
	}
	
	public double detailedTimeRatio(){
		return matchTimeDelta / (double) queryTimeDelta * 100;
	}
	
	public double detailedFrequencyRatio(){
		int frequencyDelta = queryFrequency - matchFrequency;
		//assuming 36 bins per octave!
		float frequencyDeltaInCents = 1200 / Config.getFloat(Key.CTEQ_BINS_PER_OCTAVE) * frequencyDelta;
		//e ^ (c ln(2)/1200)  converts a value c to a ratio
		return Math.pow(Math.E, frequencyDeltaInCents * ln2DividedBy1200) * 100;
	}
	
	@Override
	public boolean equals(Object other){
		if(other == null){
			return false;
		}
		if(!(other instanceof CteQFingerprintHit)){
			return false;
		}
		return ((CteQFingerprintHit) other).hashCode() == hashCode();
	}
	@Override
	public int hashCode(){
		return 	(hash | this.identifier | this.matchTime | this.matchFrequency | this.matchTimeDelta);
	}
}
