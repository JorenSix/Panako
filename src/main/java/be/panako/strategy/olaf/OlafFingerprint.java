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


package be.panako.strategy.olaf;

/**
 * A fingerprint connects three event points in a spectrogram. The points are defined
 * by a time and frequency pair, both encoded with an integer. The frequency is defined by
 * the bin index in the spectrogram. The time is defined as the index of the block processed.
 * 
 * @author Joren Six
 */
public class OlafFingerprint {

	/**
	 * time component of the event point 1
	 */
	public final int t1;
	/**
	 * frequency component of the event point 1
	 */
	public final int f1;
	/**
	 * magnitude component of the event point 1
	 */
	public final float m1;


	/**
	 * time component of the event point 2
	 */
	public final int t2;
	/**
	 * frequency component of the event point 2
	 */
	public final int f2;
	/**
	 * magnitude component of the event point 2
	 */
	public final float m2;


	/**
	 * time component of the event point 3
	 */
	public final int t3;
	/**
	 * frequency component of the event point 3
	 */
	public final int f3;
	/**
	 * magnitude component of the event point 3
	 */
	public final float m3;
	
	private long hash;

	/**
	 * Creates a new fingerprint based on three event points with each three components
	 * @param t1 time component of the event point 1
	 * @param f1 frequency component of the event point 1
	 * @param m1 time component of the event point 1
	 * @param t2 magnitude component of the event point 2
	 * @param f2 frequency component of the event point 2
	 * @param m2 magnitude component of the event point 2
	 * @param t3 time component of the event point 3
	 * @param f3 frequency component of the event point 3
	 * @param m3 magnitude component of the event point 3
	 */
	public OlafFingerprint(int t1,int f1,float m1,int t2,int f2,float m2,int t3,int f3,float m3){
		this.t1 = t1;
		this.f1 = f1;
		this.m1 = m1;
		
		this.t2 = t2;
		this.f2 = f2;
		this.m2 = m2;
		
		this.t3 = t3;
		this.f3 = f3;
		this.m3 = m3;
				
		assert t2 > t1;
		assert t3 > t2;
	}
	
	public OlafFingerprint(long hash,int t1){
		this.hash = hash;
		
		this.t1 = t1;
		this.f1 = -1;
		this.m1 = -1;
		
		this.t2 = -1;
		this.f2 = -1;
		this.m2 = -1;
		
		this.t3 = -1;
		this.f3 = -1;
		this.m3 = -1;
	}

	/**
	 * Creates a new fingerprint based on three event points
	 * @param e1 Event point 1
	 * @param e2 Event point 2
	 * @param e3 Event point 3
	 */
	public OlafFingerprint(OlafEventPoint e1, OlafEventPoint e2, OlafEventPoint e3){
		this(e1.t,e1.f,e1.m,  e2.t,e2.f,e2.m,  e3.t,e3.f,e3.m);
	}
	
	public long hash(){
		if(hash!=0)
			return hash;
		//else
		
		long f1LargerThanF2 = f2 > f3 ? 1 : 0;
		long f2LargerThanF3 = f2 > f3 ? 1 : 0;
		long f3LargerThanF1 = f3 > f1 ? 1 : 0;

		long m1LargerThanm2 = m1 > m2 ? 1 : 0;
		long m2LargerThanm3 = m2 > m3 ? 1 : 0;
		long m3LargerThanm1 = m3 > m1 ? 1 : 0;

		long dt1t2LargerThant3t2 = (t2 - t1) > (t3 - t2) ? 1 : 0;
		long df1f2LargerThanf3f2 = Math.abs(f2 - f1) > Math.abs(f3 - f2) ? 1 : 0;

		//9 bits f in range( 0 - 512) to 8 bits
		long f1Range = (f1 >> 1);
		
		//7 bits (0-128) -> 5 bits
		long df2f1 = (Math.abs(f2 - f1) >> 2);
		long df3f2 = (Math.abs(f3 - f2) >> 2);
		
		//6 bits max
		long diffT = t3 - t1;
	    
		//combine the hash components into a single 64 bit integer
		hash = 
				((diffT                &  ((1<<6)  -1)   ) << 0 ) +
		        ((f1LargerThanF2       &  ((1<<1 ) -1)   ) << 6 ) +
		        ((f2LargerThanF3       &  ((1<<1 ) -1)   ) << 7 ) +
		        ((f3LargerThanF1       &  ((1<<1 ) -1)   ) << 8 ) +
		        ((m1LargerThanm2       &  ((1<<1 ) -1)   ) << 9 ) +
		        ((m2LargerThanm3       &  ((1<<1 ) -1)   ) << 10) +
		        ((m3LargerThanm1       &  ((1<<1 ) -1)   ) << 11) +
		        ((dt1t2LargerThant3t2  &  ((1<<1 ) -1)   ) << 12) +
		        ((df1f2LargerThanf3f2  &  ((1<<1 ) -1)   ) << 13) +
		        ((f1Range              &  ((1<<8 ) -1)   ) << 14) +
		        ((df2f1                &  ((1<<6 ) -1)   ) << 22) +
		        ((df3f2                &  ((long) (1<<6 ) -1)   ) << 28) ;
		
		return hash;
	}
	
	@Override
	public String toString(){
		return String.format("(%d,%d),(%d,%d),(%d,%d),%d",t1,f1,t2,f2,t3,f3,hash());
	}
	
	public boolean equals(Object other){
	    if (other == null){
	    	return false;
	    }
	    if (other == this){
	    	return true;
	    }
	    if (!(other instanceof OlafFingerprint)){
	    	return false;
	    }
	    OlafFingerprint otherFingerprint = (OlafFingerprint) other;
	    boolean sameHash = otherFingerprint.hash() == this.hash();
	    //if closer than 100 analysis frames (of e.g. 32ms), than hash is deemed the same).
	    boolean closeInTime = Math.abs(otherFingerprint.t1 - this.t1) < 100;
	    return sameHash && closeInTime;
	}
	
	
	/*
	 * This is not completely consistent with the expected hash code / equals
	 * behavior: It is very well possible that two hashes collide, while
	 * the fingerprints are not equal to each other. Implementing hash code makes
	 * sure no identical fingerprints are added, but also that no collisions are
	 * allowed. Take care when using sets.
	 */
	public int hashCode(){
		//This is not completely consistent with the expected hash code / equals behavior:
		//It is very well possible that two hashes collide, while the fingerprints are not equal to each other.
		//Implementing hash code makes sure no identical fingerprints are added, but also that no collisions are
		//allowed. Take care when using sets. 
		return (int) hash();
	}
}
