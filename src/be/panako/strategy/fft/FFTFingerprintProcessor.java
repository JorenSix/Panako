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



package be.panako.strategy.fft;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;


/**
 * Processes audio to store landmarks in an in memory hash, or to persistent storage.
 * 
 * @author Joren Six
 */
public class FFTFingerprintProcessor implements AudioProcessor {	
	/**
	 * Target number of hashes per second of audio
	 */
	private final int numberOfFingerprintsPerSecond;
	
	private final int divideSteps;
	
	private int timeOffset; 
	
	private double lastTimeStamp;
	
	
	/**
	 * A set of unique fingerprints. Ordered by time, last fingerprint first.
	 */
	private final TreeSet<FFTFingerprint> fingerprintTree;

	public int samplesProcessed;
	

	public FFTFingerprintProcessor(int numberOfFingerprintsPerSecond,int divideSteps){
		samplesProcessed = 0;
		this.numberOfFingerprintsPerSecond = numberOfFingerprintsPerSecond;
		this.divideSteps = 1;
		//order by time
		fingerprintTree = new TreeSet<FFTFingerprint>(new Comparator<FFTFingerprint>(){
			@Override
			public int compare(FFTFingerprint o1, FFTFingerprint o2) {
				return Integer.valueOf(o2.t1).compareTo(o1.t1);
			}});
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float sampleRate = audioEvent.getSampleRate();
		int fftHopSizeInSamples = Config.getInt(Key.FFT_STEP_SIZE);
		timeOffset = samplesProcessed / fftHopSizeInSamples;
		samplesProcessed += audioEvent.getBufferSize();
		
		float[] buffer = audioEvent.getFloatBuffer();
		Set<FFTFingerprint> fingerprints = FFTFingerprintExtractorNaive.calculateLandmarks(buffer, sampleRate, numberOfFingerprintsPerSecond,timeOffset);
		fingerprintTree.addAll(fingerprints);
		
		if(divideSteps > 1){
			//divide audio block of 32ms in x steps (for better recall)
			int step = fftHopSizeInSamples / divideSteps;
			for(int i = 1; i < divideSteps ; i++){
				buffer = Arrays.copyOfRange(buffer,step,buffer.length);
				fingerprints = FFTFingerprintExtractorNaive.calculateLandmarks(buffer, sampleRate, numberOfFingerprintsPerSecond,timeOffset);
				fingerprintTree.addAll(fingerprints);
			}
		}
		lastTimeStamp = audioEvent.getEndTimeStamp();
		return true;
	}
	
	@Override
	public void processingFinished() {
	}
	
	public Set<FFTFingerprint> getFingerprints(){
		return fingerprintTree;
	}

	public void clear() {
		fingerprintTree.clear();
	}
	
	public double getTimeStamp(){
		return lastTimeStamp;
	}
}
