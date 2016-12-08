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



package be.panako.strategy.nfft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.LemireMinMaxFilter;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class NFFTEventPointProcessor implements AudioProcessor {

	private final FFT fft;
		
	/**
	 * 
	 * Use a 2D float array to prevent creation of new
	 * objects in the processing loop, at the expense of a bit of
	 * complexity
	 */
	private final float[][] magnitudes;
	/**
	 * The phase info of the current and previous frames.
	 */
	private final float[][] phases;
	
	/**
	 * A counter used in the 2D float arrays
	 */
	private int magnitudesIndex=0;
	
	//Each map maps a frame index to the data represented.
	private final Map<Integer,float[]> previousMagintudes;
	private final Map<Integer,float[]> previousPhase;
	private final Map<Integer,float[]> previousMinMagnitudes;
	private final Map<Integer,float[]> previousMaxMagnitudes;
	
	
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
	
	
	private final List<NFFTEventPoint> eventPoints = new ArrayList<>();
	private final List<NFFTFingerprint> fingerprints = new ArrayList<>();

	private int analysisFrameIndex = 0;
	
	private final LemireMinMaxFilter maxFilterVertical;
	private final LemireMinMaxFilter minFilterVertical;

	private final int maxFilterWindowSize;
	private final int minFilterWindowSize;
	private final int longestFilterWindowSize;
	
	private final float[] maxHorizontal;
	private final float[] minHorizontal;
	
	int maxFingerprintsPerEventPoint = Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT);
	
	private static final int defaultMaxFilterWindowSize = Config.getInt(Key.NFFT_MAX_FILTER_WINDOW_SIZE);
	private static final int defaultMinFilterWindowSize = Config.getInt(Key.NFFT_MIN_FILTER_WINDOW_SIZE);
	
	
	public NFFTEventPointProcessor(int size,int overlap,int sampleRate){
		this(size,overlap,sampleRate,defaultMaxFilterWindowSize,defaultMinFilterWindowSize);
	}
	
	private NFFTEventPointProcessor(int fftSize,int overlap,int sampleRate, int maxFilterWindowSize,int minFilterWindowSize){
		fft = new FFT(fftSize, new HammingWindow());
		
		this.maxFilterWindowSize = maxFilterWindowSize;
		this.minFilterWindowSize = minFilterWindowSize;
		longestFilterWindowSize = Math.max(maxFilterWindowSize, minFilterWindowSize);
		
		magnitudesIndex=0;
		magnitudes = new float[longestFilterWindowSize][fftSize/2];
		phases = new float[longestFilterWindowSize][fftSize/2];
		
		previousMagintudes = new HashMap<>();
		previousPhase = new HashMap<>();
		previousMaxMagnitudes = new HashMap<>();
		previousMinMagnitudes = new HashMap<>();
		
		maxFilterVertical = new LemireMinMaxFilter(maxFilterWindowSize, fftSize/2,true);
		minFilterVertical = new LemireMinMaxFilter(minFilterWindowSize, fftSize/2,true);
		
		maxHorizontal = new float[fftSize/2];
		minHorizontal = new float[fftSize/2];
		
		dt = (fftSize - overlap) / (double) sampleRate;
		cbin = (double) (dt * sampleRate / (double) fftSize);

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
		//It is not really needed, and skipped since it is very computationally expensive
		//log();
		
		//run a maximum filter on the frame
		maxFilterVertical.filter(magnitudes[magnitudesIndex]);
		previousMaxMagnitudes.put(analysisFrameIndex,maxFilterVertical.getMaxVal());
	
		//run a minimum filter on the frame
		minFilterVertical.filter(magnitudes[magnitudesIndex]);
		previousMinMagnitudes.put(analysisFrameIndex,minFilterVertical.getMinVal());
		
		//store the frame magnitudes
		previousMagintudes.put(analysisFrameIndex, magnitudes[magnitudesIndex]);
		//store the frame phase info
		previousPhase.put(analysisFrameIndex,phases[magnitudesIndex]);
		
		//find the horziontal minima and maxima
		if(previousMaxMagnitudes.size()==longestFilterWindowSize){
			horizontalFilter();
			//Remove analysis frames thashat are not needed any more:
			//previousMaxFrames.removeFirst();
			previousMaxMagnitudes.remove(analysisFrameIndex-longestFilterWindowSize+1);
			previousMinMagnitudes.remove(analysisFrameIndex-longestFilterWindowSize+1);
			previousMagintudes.remove(analysisFrameIndex-longestFilterWindowSize+1);
			previousPhase.remove(analysisFrameIndex-longestFilterWindowSize+1);
		}
				
		//magnitude index counter
		magnitudesIndex++;
		if(magnitudesIndex == magnitudes.length){
			magnitudesIndex=0;
		}
		
		//Increment analysis frame counter
		analysisFrameIndex++;
		
		return true;
	}
	
	public float[] getMagnitudes(){
		return magnitudes[magnitudesIndex];
	}
	

	private void horizontalFilter() {
		Arrays.fill(maxHorizontal, -1000000);
		Arrays.fill(minHorizontal, 10000000);
		
		// For the horizontal min and max filter we need a history of 
		// max(maxFilterSize/2,minFilterSize/2) frames and the same amount
		// of frames in the 'future' for the frame under analysis. 
		
		// The frame index of the frame under analysis:
		int frameUnderAnalysis = analysisFrameIndex - longestFilterWindowSize/2;
		
		// Run a horizontal min filter
		for(int i = frameUnderAnalysis - minFilterWindowSize/2; i < frameUnderAnalysis + minFilterWindowSize/2;i++){
			float[] minFrame = previousMinMagnitudes.get(i);
			for(int j = 0 ; j < minFrame.length ; j++){
				minHorizontal[j] = Math.min(minHorizontal[j], minFrame[j]);
			}
		}
		
		// Run a horizontal max filter
		for(int i = frameUnderAnalysis - maxFilterWindowSize/2; i < frameUnderAnalysis + maxFilterWindowSize/2;i++){
			float[] maxFrame = previousMaxMagnitudes.get(i);
			for(int j = 0 ; j < maxFrame.length ; j++){
				maxHorizontal[j] = Math.max(maxHorizontal[j], maxFrame[j]);
			}
		}
		
		float[] frameMagnitudes = previousMagintudes.get(frameUnderAnalysis);
		float[] currentPhase = previousPhase.get(frameUnderAnalysis);
				
				
		
		for(int i = 0 ; i<frameMagnitudes.length ; i++){
			float maxVal = maxHorizontal[i];
			float minVal = minHorizontal[i];
			float currentVal = frameMagnitudes[i];
			
			if(currentVal == maxVal && currentVal !=0 && minVal != 0 ){
				float[] previousPhaseData = previousPhase.get(frameUnderAnalysis-1);
				float frequencyEstimate = getFrequencyForBin(i, currentPhase,previousPhaseData);//in Hz
				eventPoints.add(new NFFTEventPoint(frameUnderAnalysis+longestFilterWindowSize, i, frequencyEstimate, currentVal,currentVal) );
				filterEventPoints();
			}
		}
	}
	
	private void filterEventPoints(){
		//do not 
		
		int history = 25;
		
		//filter excessive amount of event points in one frame 
		int maxEventPointsPerFrame = Config.getInt(Key.NFFT_EVENT_POINTS_MAX_PER_FFT_FRAME);
		HashMap<Integer,ArrayList<NFFTEventPoint>> counter =  new HashMap<Integer,ArrayList<NFFTEventPoint>>();
		for(int i = Math.max(eventPoints.size()-history, 0) ; i< eventPoints.size();i++){
			NFFTEventPoint ep  = eventPoints.get(i);
			if(!counter.containsKey(ep.t)){
				counter.put(ep.t, new ArrayList<NFFTEventPoint>());
			}
			counter.get(ep.t).add(ep);
		}
		for(Integer frameIndex : counter.keySet()){
			ArrayList<NFFTEventPoint> points = counter.get(frameIndex);
			if(points.size()>maxEventPointsPerFrame){
				Collections.sort(points,new Comparator<NFFTEventPoint>() {
					@Override
					public int compare(NFFTEventPoint o1, NFFTEventPoint o2) {
						return Double.compare(o2.contrast, o1.contrast);
					}
				});
				for(int i = maxEventPointsPerFrame ; i < points.size() ; i++ ){
					eventPoints.remove(points.get(i));
					//System.out.println("Removed event point at t " + points.get(i).t + " c " +  points.get(i).contrast);
				}
			}
		}
		
		int size = Config.getInt(Key.NFFT_SIZE);
		FFT fft = new FFT(size);
		float[] binStartingPointsInCents = new float[size];
		float[] binHeightsInCents = new float[size];
		for (int i = 1; i < size; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		
		
		//filter points that are too close
		double minDistance = Config.getFloat(Key.NFFT_EVENT_POINT_MIN_DISTANCE);
		float frameDurationInMS = Config.getInt(Key.NFFT_STEP_SIZE)/  ((float) Config.getInt(Key.NFFT_SAMPLE_RATE)) * 1000.f;
		List<NFFTEventPoint> pointsToDelete = new ArrayList<NFFTEventPoint>();
		for(int i = Math.max(eventPoints.size()-history, 0) ; i< eventPoints.size();i++){
			
			NFFTEventPoint first  = eventPoints.get(i);
			for(int j  = i+1  ; j< eventPoints.size();j++){
				NFFTEventPoint other  = eventPoints.get(j);
				int diffInMS = (int) (first.t * frameDurationInMS  - other.t * frameDurationInMS);
				int diffInCents = (int) ( (binStartingPointsInCents[first.f] + binHeightsInCents[first.f]/2.0f) - (binStartingPointsInCents[other.f] + binHeightsInCents[other.f]/2.0f));
				int distance = (int) Math.sqrt(diffInMS * diffInMS  +  diffInCents *  diffInCents);
				if(distance < minDistance){
					pointsToDelete.add(first.contrast > other.contrast ? other : first);
				}
			}
		}
		for(NFFTEventPoint pointToDelete : pointsToDelete){
			eventPoints.remove(pointToDelete);
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
	
	public List<NFFTFingerprint> getFingerprints(){
		return fingerprints;
	}
	

	private void packEventPointsIntoFingerprints(){
		int size = Config.getInt(Key.NFFT_SIZE);
		FFT fft = new FFT(size);
		float[] binStartingPointsInCents = new float[size];
		float[] binHeightsInCents = new float[size];
		for (int i = 1; i < size; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		
		float frameDurationInMS = Config.getInt(Key.NFFT_STEP_SIZE)/  ((float) Config.getInt(Key.NFFT_SAMPLE_RATE)) * 1000.f;
		
		int maxEventPointDeltaTInMs = 2000; //two seconds
		int maxEventPointDeltaFInCents = 1800; //1.5 octave		
		int minEventPointDeltaTInMs = 60;//milliseconds
		//Collections.shuffle(eventPoints);
		
		TreeMap<Float,NFFTFingerprint> printsOrderedByEnergy = new TreeMap<Float,NFFTFingerprint>();
					
		//int countPrint = 0;
		//Pack the event points into fingerprints
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			float f1 = binStartingPointsInCents[eventPoints.get(i).f];
			//int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			float maxfFirstLevel = f1 + maxEventPointDeltaFInCents;
			float minfFirstLevel = f1 - maxEventPointDeltaFInCents;
				
			for(int j = 0; j < eventPoints.size() ;j++){
				int t2 = eventPoints.get(j).t;
				float f2 = binStartingPointsInCents[eventPoints.get(j).f];
				if(t1 < t2 && f1 != f2 &&  Math.abs(t2-t1) * frameDurationInMS > minEventPointDeltaTInMs &&  Math.abs(t2-t1) * frameDurationInMS < maxEventPointDeltaTInMs && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					float energy = eventPoints.get(i).contrast + eventPoints.get(j).contrast;
					
					NFFTFingerprint fingerprint;
					fingerprint = new NFFTFingerprint(eventPoints.get(i),eventPoints.get(j));
					fingerprint.energy = energy;
					printsOrderedByEnergy.put(energy,fingerprint);
					//countPrint++;
				}
			}
		}
		
		//System.out.println(countPrint + " prints created, stored : " + printsOrderedByEnergy.size());
		//countPrint=0;
		int maxPrintsPerPoint = Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT);
		HashMap<NFFTEventPoint,Integer> printsPerPoint = new HashMap<NFFTEventPoint, Integer>();
		for(int i = 0; i < eventPoints.size();i++){
			printsPerPoint.put(eventPoints.get(i), 0);
		}
		for(Float key: printsOrderedByEnergy.descendingKeySet()){
			NFFTFingerprint print = printsOrderedByEnergy.get(key);
			if(printsPerPoint.get(print.p1)<maxPrintsPerPoint && printsPerPoint.get(print.p2)<maxPrintsPerPoint){
				printsPerPoint.put(print.p1,printsPerPoint.get(print.p1)+1);
				printsPerPoint.put(print.p2,printsPerPoint.get(print.p2)+1);
				fingerprints.add(print);
				//countPrint++;
			}
		}
		//System.out.println(countPrint + " prints created");
	}

	public List<NFFTEventPoint> getEventPoints() {
		return eventPoints;
	}

	public void reset() {
		eventPoints.clear();
		fingerprints.clear();
		analysisFrameIndex=0;
		magnitudesIndex=0;

		previousMagintudes.clear();
		previousPhase.clear();
		previousMaxMagnitudes.clear();
		previousMinMagnitudes.clear();
	}
	
}
