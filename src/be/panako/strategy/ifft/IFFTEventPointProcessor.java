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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.panako.util.LemireMinMaxFilter;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class IFFTEventPointProcessor implements AudioProcessor {

	private final FFT fft;
	
	//Use a 2D float array to prevent creation of new
	//objects in the processing loop, at the expense of a bit of
	//complexity
	private float[][] magnitudes;
	/**
	 * The phase info of the current frame.
	 */
	private final float[][] phases;
	
	private int magnitudesIndex=0;
	
	private final ArrayDeque<float[]> previousFrames;
	private final ArrayDeque<float[]> previousPhase;
	private final ArrayDeque<float[]> previousMinFrames;
	private final ArrayDeque<float[]> previousMaxFrames;
	
	/**
	 * The sample rate of the signal.
	 */
	private final int sampleRate;
	
	
	/**
	 * Cached calculations for the frequency calculation
	 */
	private final double dt;
	private final double cbin;
	private final double inv_2pi;
	private final double inv_deltat;
	private final double inv_2pideltat;
	
	
	private final List<IFFTEventPoint> eventPoints = new ArrayList<IFFTEventPoint>();
	private final List<IFFTFingerprint> fingerprints = new ArrayList<IFFTFingerprint>();

	private int t = 0;
	
	private final LemireMinMaxFilter maxFilterVertical;
	private final LemireMinMaxFilter minFilterVertical;

	private final int maxFilterWindowSize;
	private final int minFilterWindowSize;
	
	private final float[] maxHorizontal;
	private final float[] minHorizontal;
	
	
	public IFFTEventPointProcessor(int size,int overlap,int sampleRate){
		this(size,overlap,sampleRate,15,3);
	}
	
	public IFFTEventPointProcessor(int size,int overlap,int sampleRate, int maxFilterWindowSize,int minFilterWindowSize){
		fft = new FFT(size, new HammingWindow());
		
		magnitudesIndex=0;
		magnitudes = new float[maxFilterWindowSize/2 + minFilterWindowSize/2][size/2];
		phases = new float[maxFilterWindowSize/2 + minFilterWindowSize/2][size/2];
		
		previousFrames = new ArrayDeque<>();
		previousPhase = new ArrayDeque<>();
		previousMaxFrames = new ArrayDeque<>();
		previousMinFrames = new ArrayDeque<>();
		
		maxFilterVertical = new LemireMinMaxFilter(maxFilterWindowSize, size/2,true);
		minFilterVertical = new LemireMinMaxFilter(minFilterWindowSize, size/2,true);
		
		maxHorizontal = new float[size/2];
		minHorizontal = new float[size/2];
		
		this.maxFilterWindowSize = maxFilterWindowSize;
		this.minFilterWindowSize = minFilterWindowSize;
		
		dt = (size - overlap) / (double) sampleRate;
		cbin = (double) (dt * sampleRate / (double) size);

		inv_2pi = (double) (1.0 / (2.0 * Math.PI));
		inv_deltat = (double) (1.0 / dt);
		inv_2pideltat = (double) (inv_deltat * inv_2pi);

		this.sampleRate = sampleRate;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		//clone since the buffer is reused to slide
		float[] buffer = audioEvent.getFloatBuffer().clone();
		
		//calculate the fft
		fft.forwardTransform(buffer);
		
		//store the magnitudes (moduli) in magnitudes
		fft.powerAndPhaseFromFFT(buffer, magnitudes[magnitudesIndex],phases[magnitudesIndex]);
		
		//calculate the natural logarithm
		//log();
		
		//run a maximum filter on the frame
		maxFilterVertical.filter(magnitudes[magnitudesIndex]);
		previousMaxFrames.addLast(maxFilterVertical.getMaxVal());
	
		//run a minimum filter on the frame
		minFilterVertical.filter(magnitudes[magnitudesIndex]);
		previousMinFrames.addLast(minFilterVertical.getMinVal());
		
		//store the frame magnitudes
		previousFrames.addLast(magnitudes[magnitudesIndex]);
		//store the frame phase info
		previousPhase.addLast(phases[magnitudesIndex]);
		
		//find the horziontal minima and maxima
		if(previousMaxFrames.size()==maxFilterWindowSize){
			horizontalFilter();
			previousMaxFrames.removeFirst();
		}
		
		//this makes sure that the first frame in previousMinFrames aligns with the center of 
		//previousmaxframes
		if(previousMinFrames.size() == maxFilterWindowSize/2 + minFilterWindowSize/2 + 1 ){
			previousMinFrames.removeFirst();
		}
		
		//this makes sure that the first frame in previousframes alignes with the center of 
		//previousmaxframes
		if(previousFrames.size() == maxFilterWindowSize/2 + minFilterWindowSize/2  ){
			previousFrames.removeFirst();
			previousPhase.removeFirst();
		}
		
		//magnitude index counter
		magnitudesIndex++;
		if(magnitudesIndex == magnitudes.length){
			magnitudesIndex=0;
		}
		
		//frame counter
		t++;
		return true;
	}
	
	public float[] getMagnitudes(){
		return magnitudes[magnitudesIndex];
	}
	

	private void horizontalFilter() {
		Arrays.fill(maxHorizontal, -10000);
		Arrays.fill(minHorizontal, 10000000);
		
		Iterator<float[]> prevMinFramesIterator = previousMinFrames.iterator();
		
		int i = 0;
		while(prevMinFramesIterator.hasNext() && i < minFilterWindowSize){
			float[] minFrame = prevMinFramesIterator.next();
			for(int j = 0 ; j < minFrame.length ; j++){
				minHorizontal[j] = Math.min(minHorizontal[j], minFrame[j]);
			}
			i++;
		}
		
		Iterator<float[]> prevMaxFramesIterator = previousMaxFrames.iterator();
		while(prevMaxFramesIterator.hasNext()){
			float[] maxFrame = prevMaxFramesIterator.next();
			for(int j = 0 ; j < maxFrame.length ; j++){
				maxHorizontal[j] = Math.max(maxHorizontal[j], maxFrame[j]);
			}
		}
		
		
		float[] frame = previousFrames.getFirst();
		
		Iterator<float[]> it = previousPhase.iterator();
		float[] currentPhase = it.next();
		float[] previousPhase = null;
		if(it.hasNext()){
			previousPhase = it.next();
		}
		
		float frameMaxVal=0;
		int timeInFrames = t-maxFilterWindowSize/2;
		
		//An event point is only valid if the ratio between min and max is larger than 20%
		//This eliminates points where the minimum is close to silence.
		float minRatioThreshold = 0.20f;
		//An event point is only valid if the ratio between min and max is smaller than 90%
		//This eliminates points in a region of equal energy (no contrast between min and max).
		float maxRatioThreshold = 0.90f;
		//An event point is only valid if it contains at least 10% 
		//of the maximum energy bin in the frame.
		//This eliminates low energy points.
		float minEnergyForPoint = 0.1f;
		
		for(i = 0 ; i<frame.length ; i++){
			float maxVal = maxHorizontal[i];
			float minVal = minHorizontal[i];
			float currentVal = frame[i];
			frameMaxVal = Math.max(frameMaxVal, maxVal);
			
			if(currentVal == maxVal && currentVal !=0 && minVal != 0){
				//only calculate log values when needed, to compare minimum and max
				float maxValLog = (float) Math.log1p(maxHorizontal[i]);
				float minValLog = (float) Math.log1p(minHorizontal[i]);
				float currentValLog = (float) Math.log1p(frame[i]);
				float framMaxValLog = (float) Math.log1p(frameMaxVal);
				float ratio = minValLog/maxValLog;
				
				if(currentValLog > minEnergyForPoint * framMaxValLog &&
						ratio > minRatioThreshold  && 
						ratio < maxRatioThreshold){
					//now calculate detailed frequency information using fft:
					float frequencyEstimate = getFrequencyForBin(i, currentPhase,previousPhase);//in Hz
					//estimate should be larger than zero!
					if(frequencyEstimate > 0){
						eventPoints.add(new IFFTEventPoint(timeInFrames, i, frequencyEstimate, currentVal,minVal/maxVal) );
					}
				}
			}
					
		}
	}
	
	/**
	 * Calculates a frequency for a bin using phase info, if available.
	 * @param binIndex The FFT bin index.
	 * @return a frequency, in Hz, calculated using available phase info.
	 */
	private float getFrequencyForBin(int binIndex, float[] currentPhase,
			float[] previousPhase2) {
		final float frequencyInHertz;
		// use the phase delta information to get a more precise
		// frequency estimate
		// if the phase of the previous frame is available.
		// See
		// * Moore 1976
		// "The use of phase vocoder in computer music applications"
		// * Sethares et al. 2009 - Spectral Tools for Dynamic
		// Tonality and Audio Morphing
		// * Laroche and Dolson 1999
		if (previousPhase2!=null) {
			float phaseDelta =  previousPhase2[binIndex] - currentPhase[binIndex];
			long k = Math.round(cbin * binIndex - inv_2pi * phaseDelta);
			frequencyInHertz = (float) (inv_2pideltat * phaseDelta  + inv_deltat * k);
			//System.out.println(frequencyInHertz);
		} else {
			frequencyInHertz = (float) fft.binToHz(binIndex, sampleRate);
		}
		return frequencyInHertz;
	}
	

	@Override
	public void processingFinished() {
		packEventPointsIntoFingerprints();
	}
	
	public List<IFFTFingerprint> getFingerprints(){
		return fingerprints;
	}
	

	
	private void packEventPointsIntoFingerprints(){
		int maxEventPointDeltaTInSteps = 30; //about 30*32ms, 1 seconds (so 2 sec in total)
		int maxEventPointDeltaFInBins = 19; // 256 is the complete spectrum
		
		int maxFingerprintsPerEventPoint = 2;
		
		int minTimeDifference = 8;//time steps about 200ms
		//Pack 3 event points into a fingerprint
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			int f1 = eventPoints.get(i).f;
			int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			int maxfFirstLevel = f1 + maxEventPointDeltaFInBins;
			int minfFirstLevel = f1 - maxEventPointDeltaFInBins;
			
			//A list of fingerprints Per Event Point, ordered by energy of the combined event points
			TreeMap<Float,IFFTFingerprint> fingerprintsPerEventPoint = new TreeMap<Float,IFFTFingerprint>();
			
			for(int j = i + 1; j < eventPoints.size()  && eventPoints.get(j).t < maxtFirstLevel;j++){
				int t2 = eventPoints.get(j).t;
				int f2 = eventPoints.get(j).f;
				if(t1 != t2 && f1 != f2 && t2 > t1 + minTimeDifference && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					int maxtSecondLevel = t2 + maxEventPointDeltaTInSteps;
					int maxfSecondLevel = f2 + maxEventPointDeltaFInBins;
					int minfSecondLevel = f2 - maxEventPointDeltaFInBins;					
					for(int k = j + 1; k < eventPoints.size()  && eventPoints.get(k).t < maxtSecondLevel;k++){
						int t3 = eventPoints.get(k).t;
						int f3 = eventPoints.get(k).f;
						if(t2 != t3 && f2 != f3 && t3 > t2 + minTimeDifference && f3 > minfSecondLevel && f3 < maxfSecondLevel){
							float energy = eventPoints.get(i).contrast + eventPoints.get(j).contrast + eventPoints.get(k).contrast;
							float fe1 = eventPoints.get(i).frequencyEstimate;
							float fe2 = eventPoints.get(j).frequencyEstimate;
							float fe3 = eventPoints.get(k).frequencyEstimate;
							
							//do not accept three more or less equal frequencies, at least one should be 300 cents from another
							
							double fe1Cent = PitchConverter.hertzToAbsoluteCent(fe1);
							double fe2Cent = PitchConverter.hertzToAbsoluteCent(fe2);
							double fe3Cent = PitchConverter.hertzToAbsoluteCent(fe3);
							
							double maxDeltaFInCents = Math.max(Math.abs(fe1Cent-fe2Cent), Math.abs(fe2Cent-fe3Cent));
							maxDeltaFInCents = Math.max(Math.abs(fe1Cent-fe3Cent), maxDeltaFInCents);
							
							if(maxDeltaFInCents>350){
								IFFTFingerprint fingerprint = new IFFTFingerprint(t1,fe1, t2, fe2,t3,fe3);
								fingerprint.energy = energy;
								fingerprintsPerEventPoint.put(energy,fingerprint);
							}
						}
					}						
				}
			}

			if(fingerprintsPerEventPoint.size() >= maxFingerprintsPerEventPoint ){
				for(int s = 0 ; s < maxFingerprintsPerEventPoint ; s++){
					Entry<Float, IFFTFingerprint> e = fingerprintsPerEventPoint.lastEntry();
					fingerprints.add(e.getValue());
					fingerprintsPerEventPoint.remove(e.getKey());
				}
			}else{
				fingerprints.addAll(fingerprintsPerEventPoint.values());	
			}
		}
	}

	public List<IFFTEventPoint> getEventPoints() {
		return eventPoints;
	}
	
}
