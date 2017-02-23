/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2015 - Joren Six / IPEM                             *
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




package be.panako.strategy.qifft;


import be.tarsos.dsp.util.PitchConverter;


/**
 * A fingerprint connects two event points in a spectrogram. The points are defined
 * by a time and frequency pair, both encoded with an integer. The frequency is defined by
 * the bin index in the spectrogram. The time is defined as the index of the block processed.
 * 
 * @author Joren Six
 */
public class QIFFTFingerprint {
	
	public final double t1;
	public final double f1Estimate;
	
	public final double t2;
	public final double f2Estimate;
	
	public QIFFTEventPoint p1,p2;
	
	
	/*private final double nyquistFrequencyInCents = PitchConverter.hertzToAbsoluteCent(Config.getInt(Key.NFFT_SAMPLE_RATE)/2.0);
	private final double minimumFrequencyInCents = PitchConverter.hertzToAbsoluteCent(20);
	*/
	
	public double energy;

	
	public QIFFTFingerprint(float t1,float f1Estimate,float t2,float f2Estimate){
		this.t1 = t1;
		
		this.t2 = t2;
	
		this.f1Estimate = f1Estimate;
		this.f2Estimate = f2Estimate;
		
		assert t2 > t1;
	}	
	
	public QIFFTFingerprint(QIFFTEventPoint l1, QIFFTEventPoint l2){
		this(l1.t,l1.getFrequencyInCents(),l2.t,l2.getFrequencyInCents());
		p1 = l1;
		p2 = l2;
	}
	
	/**
	 * Calculate a hash representing this fingerprint.
	 * 
	 * @return a hash representing this fingerprint.
	 */
	public int hash(){
		final int hash;
		
		//11 bits for the exact location of the frequency component
		int f =  ((int) Math.round((f1Estimate/15.0))) & ((1<<11)-1);
		//10 bits for the frequency delta (not fully used?)
		//delta f should be correct up to 5 cents
		int deltaF = (int) Math.round(Math.abs(f2Estimate - f1Estimate)/10.0f);
		deltaF = deltaF & ((1<<10)-1);
		//6 bits for the time difference
		int deltaT = ((int) Math.round(Math.abs(timeDelta()*32)/24)) & ((1<<7)-1);
		//In total the hash contains 8 + 8 + 6 bits == 22 bits (about 4 million values)
		int binHash = (f<<17) + (deltaF<<7) + deltaT;
		if(f1Estimate>f2Estimate){
			hash = binHash *-1;
		}else{
			hash = binHash;
		}		
		return hash;
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
		return String.format("%d,%.3f,%d,%.3f,%d",t1,f1Estimate,t2,f2Estimate,hash());
	}	
	
	public boolean equals(Object other){
	    if (other == null){
	    	return false;
	    }
	    if (other == this){
	    	return true;
	    }
	    if (!(other instanceof QIFFTFingerprint)){
	    	return false;
	    }
	    QIFFTFingerprint otherFingerprint = (QIFFTFingerprint) other;
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
	public double timeDelta() {
		return t2 - t1;
	}
}
