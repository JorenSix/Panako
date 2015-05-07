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



package be.panako.strategy.ifft;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.util.PitchConverter;




/**
 * A fingerprint connects two event points in a spectrogram. The points are defined
 * by a time and frequency pair, both encoded with an integer. The frequency is defined by
 * the bin index in the spectrogram. The time is defined as the index of the block processed.
 * 
 * @author Joren Six
 */
public class IFFTFingerprint {
	
	public final int t1;
	public final int t1InMilliSeconds;
	public final float f1;
	public final int f1InCents;
	
	public final int t2;
	public final int t2InMilliSeconds;
	public final float f2;
	public final int f2InCents;
	
	public final int t3;
	public final int t3InMilliSeconds;
	public final float f3;
	public final int f3InCents;
	
	public double energy;

	private static final float sampleRate = Config.getInt(Key.IFFT_SAMPLE_RATE);
	private static final float stepSize = Config.getInt(Key.IFFT_STEP_SIZE);
	private static final float stepSizeInMs = stepSize/sampleRate * 1000f;
	
	public IFFTFingerprint(int t1,float f1,int t2,float f2,int t3, float f3){
		this.t1 = t1;
		this.f1 = f1;
		
		this.t2 = t2;
		this.f2 = f2;
		
		this.t3 = t3;
		this.f3 = f3;
		
		t1InMilliSeconds = (int) Math.round(t1 * stepSizeInMs);
		t2InMilliSeconds = (int) Math.round(t2 * stepSizeInMs);
		t3InMilliSeconds = (int) Math.round(t3 * stepSizeInMs);
		
		//store to the nearest cent value.
		f1InCents = (int) Math.round(PitchConverter.hertzToAbsoluteCent(f1));
		f2InCents = (int) Math.round(PitchConverter.hertzToAbsoluteCent(f2));
		f3InCents = (int) Math.round(PitchConverter.hertzToAbsoluteCent(f3));
		
		assert t2 > t1;
		assert t3 > t2;
	}	
	
	public IFFTFingerprint(IFFTEventPoint l1, IFFTEventPoint l2,IFFTEventPoint l3){
		this(l1.t,l1.frequencyEstimate,l2.t,l2.frequencyEstimate,l3.t,l3.frequencyEstimate);
	}
	
	/**
	 * Calculate a hash representing this fingerprint.
	 * 
	 * @return a hash representing this fingerprint.
	 */
	public int hash(){
		
				
		// Defines in which octave f1 can be found
		int f1Octave = Math.round(f1InCents/1125f)  % (1<<3);//[0-7], 3bits
		// Defines in which octave f1 can be found
		int f3Octave = Math.round(f3InCents/1213f)  % (1<<3);//[0-7], 3bits
		
		// The delta components are preserved when pitch shifting.
		// Here we assume that the estimation, shifts remain accurate to up to a 10th of a semitone (10 cents).
		int deltaF1 = Math.abs(Math.round((f2InCents/9f - f1InCents/9f))) % (1<<8);//[0-255], 8bits 
		int deltaF2 = Math.abs(Math.round((f3InCents/9f - f1InCents/9f))) % (1<<8);//[0-255], 8bits
		
				
		//Adds one bit, info about which time delta is the biggest
		int firstTimeDeltalargest = (t2-t1) > (t3-t2) ? 1 : 0;//[0-1]1bit
		
		int timeRatio = Math.round(ratio()*16) % (1<<5);//[0-15], 4bits
		
		//Adds two bits bit, info about which frequency is the highest
		int firstFrequencyHigherThanSecond = (f1InCents) > (f2InCents) ? 1 : 0;//[0-1]1bit
		int SecondFrequencyHigherThanThird = (f2InCents) > (f3InCents) ? 1 : 0;//[0-1]1bit
		
		// Adds info about the size of t3-t1
		// Time can deviate about 10%, the delta between t3 and t2
		// must be scaled
		// [68ms,2800ms] = [2-80]
		// [76,3080] (+10%) = [2,88]
		// [57,2520] (-10%] = [2,72]
		// to make [2,88] = [2,72] divide by 20 and round:
		// [0,round(4.4)] = [0,round(3.6)]
		int timeDeltaBin = Math.round((t3InMilliSeconds-t1InMilliSeconds)/600.0f) % (1<<2);//2bits
		
		//Hash entropy: 3+3+8+8+1+1+1+2+4 = 30bits
		
		
		int hash = 0;
		hash += f1Octave						* 1  	 ;//3bits		
		hash += f3Octave        				* (1<<3) ;//3bits (3)
		hash += deltaF1      					* (1<<6) ;//8bits (3+3)
		hash += deltaF2      					* (1<<14);//8bits (3+3+8)
		hash += firstTimeDeltalargest    		* (1<<22);//1bits (3+3+8+8+1)
		hash += firstFrequencyHigherThanSecond 	* (1<<23);//1bits (3+3+8+8+1+1)
		hash += SecondFrequencyHigherThanThird	* (1<<24);//1bits (3+3+8+8+1+1+1)
		hash += timeDeltaBin 					* (1<<26);//2bits (3+3+8+8+1+1+1)
		hash += timeRatio 						* (1<<27);//4bits (3+3+8+8+1+1+1+2)
		
		return hash;
	}
	
	public float ratio(){
		return (t2InMilliSeconds - t1InMilliSeconds)/(float)(t3InMilliSeconds - t1InMilliSeconds);
	}
	

	/**
	 * @param hash the hash to reverse
	 * @return an array of integers with [f1,df,dt].
	 */
	public static int[] reverseHash(int hash){
		int[] values = new int[3];
		int f1 = hash>>14;
		int df = (hash - f1 * (1<<14)) / (1<<6);
		int dt = (hash - f1 * (1<<14) - df * (1<<6));
		values[0]=f1;
		values[1]=df;
		values[2]=dt;
		return values;
	}
	
	public String toString(){
		return String.format("%d,%d,%d,%d,%d",t1,f1,t2,f2,hash());
	}
	
	public boolean equals(Object other){
	    if (other == null){
	    	return false;
	    }
	    if (other == this){
	    	return true;
	    }
	    if (!(other instanceof IFFTFingerprint)){
	    	return false;
	    }
	    IFFTFingerprint otherFingerprint = (IFFTFingerprint) other;
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
	 * The time delta between the first and last event.
	 * 
	 * @return The difference between t1 and t2, in analysis frames.
	 */
	public int timeDelta() {
		return t2 - t1;
	}
}
