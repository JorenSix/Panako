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



package be.panako.strategy.fft.storage;


/**
 * Describes a landmark hit in the store.
 * @author Joren Six
 */
public class FFTFingerprintHit {
	
	/**
	 * The audio identifier
	 */
	public int identifier;
	
	/**
	 * Time in blocks in the original, matched audio.
	 */
	public int matchTime;
	
	/**
	 * Difference in time between the original, matched audio and the query. Expressed in blocks.
	 */
	public int timeDifference;	
	
	/**
	 * Time in blocks in the query.
	 */
	public int queryTime;
	
	
	
	public boolean equals(Object other){
	    if (other == null){
	    	return false;
	    }
	    if (other == this){
	    	return true;
	    }
	    if (!(other instanceof FFTFingerprintHit)){
	    	return false;
	    }
	    FFTFingerprintHit otherFingerprintHit = (FFTFingerprintHit) other;
	    return otherFingerprintHit.hashCode() == this.hashCode();
	}
	
	
	/*
	 * It is the same fingerprint hit if query time, match time and identifier are the same.
	 */
	/*public int hashCode(){
		return (identifier | matchTime << 20 |  queryTime << 24 );
	}
	*/
	
}
