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



package be.panako.strategy.cteq;

/**
 * A fingerprint connects three event points in a spectrogram. The points are defined
 * by a time and frequency pair, both encoded with an integer. The frequency is defined by
 * the bin index in the spectrogram. The time is defined as the index of the block processed.
 * 
 * @author Joren Six
 */
public class CteQFingerprint {
	
	public final int t1;
	public final int f1;
	
	public final int t2;
	public final int f2;
	
	public final int t3;
	public final int f3;
	
	private final CteQHashFunction hashFunction;

	
	public CteQFingerprint(int t1,int f1,int t2,int f2,int t3,int f3){
		this.t1 = t1;
		this.f1 = f1;
		
		this.t2 = t2;
		this.f2 = f2;
		
		this.t3 = t3;
		this.f3 = f3;
		
		this.hashFunction = new CteQHashFunction();
		
		assert t2 > t1;
		assert t3 > t2;
	}	
	
	public CteQFingerprint(CteQEventPoint l1, CteQEventPoint l2, CteQEventPoint l3){
		this(l1.t,l1.f,l2.t,l2.f,l3.t,l3.f);
	}	
	
	
	
	/**
	 * Calculate a hash representing this fingerprint.
	 * 
	 * @return a hash representing this fingerprint.
	 */
	public int hash(){
		return hashFunction.calculateHash(this);
	}
	

	/**
	 * @param hash the hash to reverse
	 * @return an array of integers with [f1,df1,df2,tr]. The approximate frequency location, delta f2-f1, delta f2-f3, time ratio
	 */
	public static int[] reverseHash(int hash){
		return null;//return  Hash.getConfiguredHashFunction().reverseHash(hash);
	}
	
	public String toString(){
		return String.format("%d,%d,%d,%d,%d,%d,%d",t1,f1,t2,f2,t3,f3,hash());
	}
	
	public boolean equals(Object other){
	    if (other == null){
	    	return false;
	    }
	    if (other == this){
	    	return true;
	    }
	    if (!(other instanceof CteQFingerprint)){
	    	return false;
	    }
	    CteQFingerprint otherFingerprint = (CteQFingerprint) other;
	    boolean sameHash = otherFingerprint.hash() == this.hash();
	    //if closer than 100 analysis frames (of e.g. 32ms), than hash is deemed the same).
	    boolean closeInTime = Math.abs(otherFingerprint.t1 - this.t1) < 100;
	    return sameHash && closeInTime;
	}
	
	
	/*
	 * This is not completely consistent with the expected hash code / equals
	 * behavior: It is very well possible that that two hashes collide, while
	 * the fingerprints are not equal to each other. Implementing hash code makes
	 * sure no identical fingerprints are added, but also that no collisions are
	 * allowed. Take care when using sets.
	 */
	public int hashCode(){
		//This is not completely consistent with the expected hash code / equals behavior:
		//It is very well possible that that two hashes collide, while the fingerprints are not equal to each other.
		//Implementing hash code makes sure no identical fingerprints are added, but also that no collisions are
		//allowed. Take care when using sets. 
		return hash();
	}

	/**
	 * The time delta between the first and last event is max 2.4 seconds. In analysis frames of 1024 samples at 44100Hz, this is
	 * 2.4 * 44100/1024 = 104 max (1.2 seconds is 52 per steps). 
	 * 
	 * [0.96,1.44] to [41.3,62.01]
	 * @return The difference between t1 and t3, in analysis frames.
	 */
	public int timeDelta() {
		return t3 - t1;
	}


	public String explainHash() {
		return hashFunction.explain(this);
	}
}
