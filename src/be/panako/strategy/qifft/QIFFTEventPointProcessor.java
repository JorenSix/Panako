/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
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

import java.util.ArrayList;
import java.util.Arrays;
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

public class QIFFTEventPointProcessor implements AudioProcessor {

	private final FFT fft;
		
	/**
	 * 
	 * Use a 2D float array to prevent creation of new
	 * objects in the processing loop, at the expense of a bit of
	 * complexity
	 */
	private final float[][] magnitudes;
	
	
	/**
	 * A counter used in the 2D float arrays
	 */
	private int magnitudesIndex=0;
	
	//Each map maps a frame index to the data represented.
	private final Map<Integer,float[]> previousMagintudes;
	private final Map<Integer,float[]> previousMinMagnitudes;
	private final Map<Integer,float[]> previousMaxMagnitudes;
	
	/**
	 * The sample rate of the signal.
	 */
	private final int sampleRate;
	
	private final List<QIFFTEventPoint> eventPoints = new ArrayList<>();
	private final List<QIFFTFingerprint> fingerprints = new ArrayList<>();

	private int analysisFrameIndex = 0;
	
	private final LemireMinMaxFilter maxFilterVertical;
	private final LemireMinMaxFilter minFilterVertical;
	
	private final float[] zeroPaddedData;
	private final int zeropaddingFactor;

	private final int maxFilterWindowSize;
	private final int minFilterWindowSize;
	private final int longestFilterWindowSize;
	
	private final float[] maxHorizontal;
	private final float[] minHorizontal;
	
	int maxFingerprintsPerEventPoint = Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT);
	
	private static final int defaultMaxFilterWindowSize = Config.getInt(Key.NFFT_MAX_FILTER_WINDOW_SIZE);
	private static final int defaultMinFilterWindowSize = Config.getInt(Key.NFFT_MIN_FILTER_WINDOW_SIZE);
	private static final float defaultSampleRate = Config.getInt(Key.NFFT_SAMPLE_RATE);
	
	
	public QIFFTEventPointProcessor(int size,int overlap,int sampleRate,int zeropaddingFactor){
		this(size,overlap,sampleRate,defaultMaxFilterWindowSize,defaultMinFilterWindowSize,zeropaddingFactor);
	}
	
	private QIFFTEventPointProcessor(int fftSize,int overlap,int sampleRate, int maxFilterWindowSize,int minFilterWindowSize,int zeropaddingFactor){
		fft = new FFT(fftSize*zeropaddingFactor, new HammingWindow());
		
		this.maxFilterWindowSize = maxFilterWindowSize;
		this.minFilterWindowSize = minFilterWindowSize;
		longestFilterWindowSize = Math.max(maxFilterWindowSize, minFilterWindowSize);
		
		magnitudesIndex=0;
		magnitudes = new float[longestFilterWindowSize][(fftSize * zeropaddingFactor) /2];
			
		zeroPaddedData = new float[zeropaddingFactor*fftSize];
		
		this.zeropaddingFactor = zeropaddingFactor;
		
		previousMagintudes = new HashMap<>();
		previousMaxMagnitudes = new HashMap<>();
		previousMinMagnitudes = new HashMap<>();
		
		maxFilterVertical = new LemireMinMaxFilter(maxFilterWindowSize * 5, (fftSize * zeropaddingFactor) /2,true);
		minFilterVertical = new LemireMinMaxFilter(minFilterWindowSize * 5, (fftSize * zeropaddingFactor) /2,true);
		
		maxHorizontal = new float[(fftSize * zeropaddingFactor) /2];
		minHorizontal = new float[(fftSize * zeropaddingFactor) /2];

		this.sampleRate = sampleRate;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		//clone since the buffer is reused to slide
		float[] buffer = audioEvent.getFloatBuffer();
		float[] zeroPaddedData = new float[zeropaddingFactor*512];
		int offset = (buffer.length * zeropaddingFactor - buffer.length)/2;
		for(int i = offset ; i < offset + buffer.length ;i ++){
			zeroPaddedData[i] = buffer[i-offset];
		}
		
		//calculate the fft
		fft.forwardTransform(zeroPaddedData);
		
		//store the magnitudes (moduli) in magnitudes
		fft.modulus(zeroPaddedData, magnitudes[magnitudesIndex]);
				
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
		
		//find the horziontal minima and maxima
		if(previousMaxMagnitudes.size()==longestFilterWindowSize){
			horizontalFilter();
			//Remove analysis frames that are not needed any more:
			//previousMaxFrames.removeFirst();
			previousMaxMagnitudes.remove(analysisFrameIndex-longestFilterWindowSize+1);
			previousMinMagnitudes.remove(analysisFrameIndex-longestFilterWindowSize+1);
			previousMagintudes.remove(analysisFrameIndex-longestFilterWindowSize+1);
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
		
		for(int i = 0 ; i<frameMagnitudes.length ; i++){
			float maxVal = maxHorizontal[i];
			float minVal = minHorizontal[i];
			float currentVal = frameMagnitudes[i];
			
			if(currentVal == maxVal && currentVal !=0 && minVal != 0 && currentVal > 0.01 ){
				float frequencyEstimate = getFrequencyForBin(i,frameMagnitudes);//in Hz
				
				float mp1 =  previousMagintudes.get(frameUnderAnalysis+1)[i];
				float m = previousMagintudes.get(frameUnderAnalysis)[i];
				float mm1 =  previousMagintudes.get(frameUnderAnalysis-1)[i];
				
				float p = (mp1 - mm1)/(2*(2*m - mp1 - mm1));
				float time = frameUnderAnalysis+longestFilterWindowSize + p;
				
				if(frequencyEstimate < sampleRate /2)
				
				//System.out.println(String.format("%.4f %.4f %.4f %.4f", mm1,m,mp1,time));
				eventPoints.add(new QIFFTEventPoint(time, frequencyEstimate, currentVal,currentVal) );
			}
		}
	}
	
	
	/**
	 * Calculates a frequency for a bin using quadratic interpolation, if possible.
	 * @param binIndex The FFT bin index.
	 * @return a frequency, in Hz, calculated using available phase info.
	 */
	private float getFrequencyForBin(int i,float[] magnis) {
		final float frequencyInHertz;
		
		if(i > 0 && i < fft.size()/2  - 1 &&  magnis[i+1] < magnis[i] && magnis[i] > magnis[i-1] ){
				float p = (magnis[i+1] - magnis[i-1])/(2*(2*magnis[i] - magnis[i+1] - magnis[i-1]));
				//float y = amplitudes[i] - 0.25f*(amplitudes[i-1]-amplitudes[i+1])*p;
				//float a = 0.5f*(amplitudes[i-1] - 2*amplitudes[i] + amplitudes[i+1]);
				frequencyInHertz = (i+p)*defaultSampleRate/(float)zeroPaddedData.length;
				//System.out.println(String.format("%d %.3f %.3f %.3f ",i, p,magnis[i],frequencyInHertz));
		} else {
			magnis[i]=0;
			frequencyInHertz = 0;//(float) fft.binToHz(binIndex, sampleRate);
		}
		return frequencyInHertz;
	}
	

	@Override
	public void processingFinished() {
		packEventPointsIntoFingerprints();
	}
	
	public List<QIFFTFingerprint> getFingerprints(){
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
		
		TreeMap<Float,QIFFTFingerprint> printsOrderedByEnergy = new TreeMap<Float,QIFFTFingerprint>();
					
		//int countPrint = 0;
		//Pack the event points into fingerprints
		for(int i = 0; i < eventPoints.size();i++){
			float t1 = eventPoints.get(i).t;
			float f1 =  eventPoints.get(i).getFrequencyInCents();
			//int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			float maxfFirstLevel = f1 + maxEventPointDeltaFInCents;
			float minfFirstLevel = f1 - maxEventPointDeltaFInCents;
				
			for(int j = 0; j < eventPoints.size() ;j++){
				float t2 = eventPoints.get(j).t;
				float f2 = eventPoints.get(j).getFrequencyInCents();
				if(t1 < t2 && f1 != f2 &&  Math.abs(t2-t1) * frameDurationInMS > minEventPointDeltaTInMs &&  Math.abs(t2-t1) * frameDurationInMS < maxEventPointDeltaTInMs && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					float energy = eventPoints.get(i).contrast + eventPoints.get(j).contrast;
					
					QIFFTFingerprint fingerprint;
					fingerprint = new QIFFTFingerprint(eventPoints.get(i),eventPoints.get(j));
					fingerprint.energy = energy;
					printsOrderedByEnergy.put(energy,fingerprint);
				}
			}
		}
		
		//System.out.println(countPrint + " prints created, stored : " + printsOrderedByEnergy.size());
		//int countPrint=0;
		int maxPrintsPerPoint = Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT);
		HashMap<QIFFTEventPoint,Integer> printsPerPoint = new HashMap<QIFFTEventPoint, Integer>();
		for(int i = 0; i < eventPoints.size();i++){
			printsPerPoint.put(eventPoints.get(i), 0);
		}
		for(Float key: printsOrderedByEnergy.descendingKeySet()){
			QIFFTFingerprint print = printsOrderedByEnergy.get(key);
			if(printsPerPoint.get(print.p1)<maxPrintsPerPoint && printsPerPoint.get(print.p2)<maxPrintsPerPoint){
				printsPerPoint.put(print.p1,printsPerPoint.get(print.p1)+1);
				printsPerPoint.put(print.p2,printsPerPoint.get(print.p2)+1);
				fingerprints.add(print);
				//countPrint++;
			}
		}
	}

	public List<QIFFTEventPoint> getEventPoints() {
		return eventPoints;
	}

	public void reset() {
		eventPoints.clear();
		fingerprints.clear();
		analysisFrameIndex=0;
		magnitudesIndex=0;

		previousMagintudes.clear();
		previousMaxMagnitudes.clear();
		previousMinMagnitudes.clear();
	}
	
}
