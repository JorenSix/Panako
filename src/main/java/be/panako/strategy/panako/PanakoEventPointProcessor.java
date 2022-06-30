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



package be.panako.strategy.panako;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.LemireMinMaxFilter;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.ugent.jgaborator.JGaborator;

public class PanakoEventPointProcessor implements AudioProcessor {

	private final JGaborator gaborator;
		
	/**
	 * 
	 * Use a 2D float array to prevent creation of new
	 * objects in the processing loop, at the expense of a bit of
	 * complexity
	 */
	private final float[][] magnitudes;
	private final float[][] maxMagnitudes;
	
	/**
	 * A counter used in the 2D float arrays
	 */
	private int magnitudesIndex=0;
	
	//Each map maps a frame index to the data represented.
	private final Map<Integer,float[]> previousMaxMagnitudes;
	private final Map<Integer,float[]> previousMagnitudes;
	
	private final List<PanakoEventPoint> eventPoints = new ArrayList<>();
	private final List<PanakoFingerprint> fingerprints = new ArrayList<>();

	private int analysisFrameIndex = 0;
	
	private final LemireMinMaxFilter maxFilterVertical;

	private final int maxFilterWindowSizeFrequency = Config.getInt(Key.PANAKO_FREQ_MAX_FILTER_SIZE);
	private final int maxFilterWindowSizeTime = Config.getInt(Key.PANAKO_TIME_MAX_FILTER_SIZE);
	
	private final float[] maxHorizontal;
	
	//private final int maxFingerprintsPerEventPoint = 10;
	
	public PanakoEventPointProcessor(final int fftSize){
		
		int stepSize = Config.getInt(Key.PANAKO_AUDIO_BLOCK_SIZE);
		int sampleRate = Config.getInt(Key.PANAKO_SAMPLE_RATE);
		int minFrequency = Config.getInt(Key.PANAKO_TRANSF_MIN_FREQ); 
		int maxFrequency = Config.getInt(Key.PANAKO_TRANSF_MAX_FREQ); 
		int bandsPerOctave =Config.getInt(Key.PANAKO_TRANSF_BANDS_PER_OCTAVE) ; // with 6 octaves this means that band index fits in 512, or 9 bits 
		int refFrequency = Config.getInt(Key.PANAKO_TRANSF_REF_FREQ); //center 440Hz
		int resolution = Config.getInt(Key.PANAKO_TRANSF_TIME_RESOLUTION);//in audio samples, 8 ms
		
		gaborator = new JGaborator(stepSize, sampleRate, bandsPerOctave, minFrequency, maxFrequency, refFrequency, resolution);
		
		magnitudesIndex=0;
		magnitudes = new float[maxFilterWindowSizeTime][fftSize/2];
		maxMagnitudes = new float[maxFilterWindowSizeTime][fftSize/2];

		previousMaxMagnitudes = new HashMap<>();
		previousMagnitudes = new HashMap<>();

		maxFilterVertical = new LemireMinMaxFilter(maxFilterWindowSizeFrequency, fftSize/2,true);
		
		maxHorizontal = new float[fftSize/2];
	}
	
	void naive_max_filter(float[] data, float[]  max, int  half_filter_size , boolean clamp){

		for(int i = 0 ; i < data.length ; i++){
			int startIndex = Math.max(i - half_filter_size,0);
			int  stopIndex = Math.min(data.length-1, i + half_filter_size);
			float maxValue = -1000000;
			for(int j = startIndex ; j <= stopIndex; j++){
				if(maxValue < data[j]){
	                maxValue = data[j];
	            }
			}
	        max[i] = maxValue;
		}
	   
	}
	
	@Override	
	public boolean process(AudioEvent audioEvent) {
		
		gaborator.process(audioEvent);
		
		return true;
	}
	
	public float[] getMagnitudes(){
		return magnitudes[magnitudesIndex];
	}
	
	private void horizontalFilter(int j) {
 		Arrays.fill(maxHorizontal, -1000);
		
		// The frame index of the frame under analysis:
 		int centerFrameIndex = analysisFrameIndex - maxFilterWindowSizeTime /2;
		int startFrameIndex =  centerFrameIndex - maxFilterWindowSizeTime/2;
		int stopFrameIndex =  centerFrameIndex + maxFilterWindowSizeTime/2;
		
		// Run a horizontal max filter
		for(int i = startFrameIndex ; i < stopFrameIndex ; i++){
			float[] maxFrame = previousMaxMagnitudes.get(i);
			maxHorizontal[j] = Math.max(maxHorizontal[j], maxFrame[j]);
		}
	}
	
	
	@Override
	public void processingFinished() {
		gaborator.processingFinished();
		
		//calculate the fft
		List<float[]> allMagnitudes = gaborator.getCoefficents();
		
		for(float[] currentMagnitudes : allMagnitudes) {
			
			magnitudes[magnitudesIndex]=currentMagnitudes;
			
			//store the frame magnitudes
			previousMagnitudes.put(analysisFrameIndex, magnitudes[magnitudesIndex]);
			
			//run a max filter over frequency bins
			maxFilterVertical.maxFilter(magnitudes[magnitudesIndex],maxMagnitudes[magnitudesIndex]);
			//store the max filtered frequency bins
			previousMaxMagnitudes.put(analysisFrameIndex,maxMagnitudes[magnitudesIndex]);
			
			//find the horziontal maxima
			if(previousMaxMagnitudes.size()==maxFilterWindowSizeTime){
				
				int t = analysisFrameIndex - maxFilterWindowSizeTime /2;
				
				float[] maxFrame = previousMaxMagnitudes.get(t);
				float[] frameMagnitudes = previousMagnitudes.get(t);
				
				for(int f = 2 ; f < frameMagnitudes.length - 1 ; f++){
					float maxVal = maxFrame[f];
					float currentVal = frameMagnitudes[f];
					
					if(maxVal == currentVal) {
						horizontalFilter(f);
						maxVal = maxHorizontal[f];
						if(currentVal == maxVal && currentVal !=0 ){
							
							float[] prevFrameMagnitudes = previousMagnitudes.get(t-1);
							float[] nextFrameMagnitudes = previousMagnitudes.get(t+1);
							
							//add the magnitude of surrounding bins for magnitude estimates more robust against discretization effects 
							float totalMagnitude = frameMagnitudes[f] + prevFrameMagnitudes[f] + nextFrameMagnitudes[f]
									+ frameMagnitudes[f+1] + prevFrameMagnitudes[f+1] + nextFrameMagnitudes[f+1]
									+ frameMagnitudes[f-1] + prevFrameMagnitudes[f-1] + nextFrameMagnitudes[f-1];
							
							eventPoints.add(new PanakoEventPoint(t, f,totalMagnitude));
						}
					}
				}
				
				//Remove analysis frames that are not needed any more:
				previousMaxMagnitudes.remove(analysisFrameIndex-maxFilterWindowSizeTime+1);
				previousMagnitudes.remove(analysisFrameIndex-maxFilterWindowSizeTime+1);
			}
					
			//magnitude index counter
			magnitudesIndex++;
			if(magnitudesIndex == magnitudes.length){
				magnitudesIndex=0;
			}
			
			//Increment analysis frame counter
			analysisFrameIndex++;
		}
		
		packEventPointsIntoFingerprints();
	}
	
	public List<PanakoFingerprint> getFingerprints(){
		return fingerprints;
	}

	public List<PanakoEventPoint> getEventPoints() {
		return eventPoints;
	}
	
	private void packEventPointsIntoFingerprints(){
		
		int minFreqDistance = Config.getInt(Key.PANAKO_FP_MIN_FREQ_DIST);
		int maxFreqDistance = Config.getInt(Key.PANAKO_FP_MAX_FREQ_DIST);
		
		int minTimeDistance = Config.getInt(Key.PANAKO_FP_MIN_TIME_DIST);
		int maxTimeDistance = Config.getInt(Key.PANAKO_FP_MAX_TIME_DIST);
		
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			int f1 = eventPoints.get(i).f;
				
			for(int j = i + 1; j < eventPoints.size() ;j++){
				int t2 = eventPoints.get(j).t;
				int f2 = eventPoints.get(j).f;
				
				int fDiff = Math.abs(f1 - f2);
				int tDiff = t2-t1;
				
				if(tDiff > maxTimeDistance) break;
				if(tDiff < minTimeDistance) continue;
				
				if(fDiff < minFreqDistance) continue;
				if(fDiff > maxFreqDistance ) continue;
				
				for(int k = j + 1 ; k <eventPoints.size(); k++) {
					int t3 = eventPoints.get(k).t;
					int f3 = eventPoints.get(k).f;
					
					fDiff = Math.abs(f2 - f3);
					tDiff = t3-t2;
					
					if(tDiff > maxTimeDistance) break;
					if(tDiff < minTimeDistance) continue;
					
					if(fDiff < minFreqDistance) continue;
					if(fDiff > maxFreqDistance ) continue;
					
					PanakoFingerprint fingerprint;
					fingerprint = new PanakoFingerprint(eventPoints.get(i),eventPoints.get(j),eventPoints.get(k));
					fingerprints.add(fingerprint);
				}
			}
		}
	}

	public void reset() {
		eventPoints.clear();
		fingerprints.clear();
		analysisFrameIndex=0;
		magnitudesIndex=0;
		previousMagnitudes.clear();
		previousMaxMagnitudes.clear();
	}

	public int latency() {
		return gaborator.getLatency();
	}
	
}
