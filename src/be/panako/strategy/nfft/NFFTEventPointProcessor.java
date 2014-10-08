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


package be.panako.strategy.nfft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import be.panako.util.LemireMinMaxFilter;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HannWindow;

public class NFFTEventPointProcessor implements AudioProcessor {

	private final FFT fft;
	
	//Use a 2D float array to prevent creation of new
	//objects in the processing loop, at the expense of a bit of
	//complexity
	private float[][] magnitudes;
	private int magnitudesIndex=0;
	
	private final ArrayDeque<float[]> previousFrames;
	private final ArrayDeque<float[]> previousMinFrames;
	private final ArrayDeque<float[]> previousMaxFrames;
	
	private final List<NFFTEventPoint> eventPoints = new ArrayList<>();
	private final Set<NFFTFingerprint> fingerprints = new HashSet<>();

	private int t = 0;
	
	private final LemireMinMaxFilter maxFilterVertical;
	private final LemireMinMaxFilter minFilterVertical;

	private final int maxFilterWindowSize;
	private final int minFilterWindowSize;
	
	private final float[] maxHorizontal;
	private final float[] minHorizontal;
	
	
	public NFFTEventPointProcessor(int size){
		this(size,15,3);
	}
	
	public NFFTEventPointProcessor(int size, int maxFilterWindowSize,int minFilterWindowSize){
		fft = new FFT(size, new HannWindow());
		
		magnitudesIndex=0;
		magnitudes = new float[maxFilterWindowSize/2 + minFilterWindowSize/2][size/2];
		
		previousFrames = new ArrayDeque<>();
		previousMaxFrames = new ArrayDeque<>();
		previousMinFrames = new ArrayDeque<>();
		
		maxFilterVertical = new LemireMinMaxFilter(maxFilterWindowSize, size/2,true);
		minFilterVertical = new LemireMinMaxFilter(minFilterWindowSize, size/2,true);
		
		maxHorizontal = new float[size/2];
		minHorizontal = new float[size/2];
		
		this.maxFilterWindowSize = maxFilterWindowSize;
		this.minFilterWindowSize = minFilterWindowSize;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		//clone since the buffer is reused to slide
		float[] buffer = audioEvent.getFloatBuffer().clone();
		
		//calculate the fft
		fft.forwardTransform(buffer);
		
		//store the magnitudes (moduli) in magnitudes
		fft.modulus(buffer, magnitudes[magnitudesIndex]);
		
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
					eventPoints.add(new NFFTEventPoint(timeInFrames, i, currentVal,minVal/maxVal) );
				}
			}
					
		}
	}
	
	

	@Override
	public void processingFinished() {
		packEventPointsIntoFingerprints();
	}
	
	public Set<NFFTFingerprint> getFingerprints(){
		return fingerprints;
	}
	

	
	private void packEventPointsIntoFingerprints(){
		int maxEventPointDeltaTInSteps = 120; //about two seconds
		int maxEventPointDeltaFInBins = 19; // 256 is the complete spectrum
		
		int maxFingerprintsPerEventPoint = 2;
		
		int minTimeDifference = 19;//time steps about 200ms
		//Pack the event points into fingerprints
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			int f1 = eventPoints.get(i).f;
			int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			int maxfFirstLevel = f1 + maxEventPointDeltaFInBins;
			int minfFirstLevel = f1 - maxEventPointDeltaFInBins;
			
			//A list of fingerprints Per Event Point, ordered by energy of the combined event points
			TreeMap<Float,NFFTFingerprint> fingerprintsPerEventPoint = new TreeMap<Float,NFFTFingerprint>();
			
			for(int j = i + 1; j < eventPoints.size()  && eventPoints.get(j).t < maxtFirstLevel;j++){
				int t2 = eventPoints.get(j).t;
				int f2 = eventPoints.get(j).f;
				if(t1 != t2 && f1 != f2 && t2 > t1 + minTimeDifference && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					float energy = eventPoints.get(i).contrast + eventPoints.get(j).contrast;
					NFFTFingerprint fingerprint = new NFFTFingerprint(t1, f1, t2, f2);
					fingerprint.energy = energy;
					fingerprintsPerEventPoint.put(energy,fingerprint);					
				}
			}

			if(fingerprintsPerEventPoint.size() >= maxFingerprintsPerEventPoint ){
				for(int s = 0 ; s < maxFingerprintsPerEventPoint ; s++){
					Entry<Float, NFFTFingerprint> e = fingerprintsPerEventPoint.lastEntry();
					fingerprints.add(e.getValue());
					fingerprintsPerEventPoint.remove(e.getKey());
				}
			}else{
				fingerprints.addAll(fingerprintsPerEventPoint.values());	
			}
		}
	}

	public List<NFFTEventPoint> getEventPoints() {
		return eventPoints;
	}
	
}
