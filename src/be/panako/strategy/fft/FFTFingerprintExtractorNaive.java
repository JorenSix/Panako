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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HannWindow;

/**
 * The main task for an explorer is to find fingerprints. 
 * It detects peaks features in the time-frequency landscape
 * which are then used for query-matching or storage. 
 * Only a few fingerprints need to be in common to both
 * query audio and stored audio. The configured density of fingerprints (in both
 * query and stored audio) determines the recall, but also the impact on the database and 
 * query-response time.
 * 
 * @author Joren Six
 * @author Dan Ellis
 */
public class FFTFingerprintExtractorNaive {

	/**     
	 *	The actual frequency and time differences are quantized and
	*     packed into the final hash; if they exceed the limited size
	*     described above, the hashes become irreversible (aliased);
	*     however, in most cases they still work (since they are
	*     handled the same way for query and reference).
	*/
	float sampleRate;
	
	/**
	 * The number of landmarks per second to generate.
	 */
	int landmarksPerSecond;
	
	/**
	 * The FFT-size in samples.
	 */
	int fftSizeBins;
	
	/**
	 * The hop size of the FFT in samples.
	 */
	int fftHopSizeBins;
	
	float  highPassFilterPole = 0.98f;
	

	
	
	//keep the spectral features
	private boolean keepSpectralFeatures = false;
	private float[][] spectralFeatures;
	
	/**
	 * Disables public access to default constructor
	 * @param sampleRate The sample rate in samples per second.
	 * @param fingerprintsPerSecond The requested maximum number of landmarks per second. The actual number of landmarks depend on the input: e.g. generating landmarks on total silence generates not much landmarks.  
	 */
	public FFTFingerprintExtractorNaive(float sampleRate,int fingerprintsPerSecond){
		this.sampleRate = sampleRate;
		this.landmarksPerSecond = fingerprintsPerSecond;
		
		fftSizeBins = Config.getInt(Key.FFT_SIZE);
		fftHopSizeBins = Config.getInt(Key.FFT_STEP_SIZE);
	}
	
	public void calculateSpectralFeatures(float[][] amplitudes,float[] audioBuffer){
		
	
		
		FFT fft = new FFT(fftSizeBins, new HannWindow());
		int completeFrames = amplitudes.length;
		// e.g. first frame from 0-512, second from 256 - 768 - 512
		int firstSampleInLastCompleteFrame = completeFrames * fftHopSizeBins + fftHopSizeBins - fftSizeBins;
		for (int i = 0; i <= firstSampleInLastCompleteFrame; i += fftHopSizeBins) {
			float[] data = new float[fftSizeBins];
			for (int j = 0; j < data.length; j++) {
				data[j] = audioBuffer[i + j];
			}
			// transform
			fft.forwardTransform(data);
			fft.modulus(data, amplitudes[i / fftHopSizeBins]);
		}

		//float maxValue = maxValue(amplitudes);
		//replace zero values with a very small value to
		//prevent log(0)
		removeZeroValues(amplitudes);
		// calculate the natural logarithm for each value
		log(amplitudes);
		// calculate the mean of all values
		float mean = mean(amplitudes);
		// make it zero mean, so the start-up transients for the filter are
		// minimized
		add(amplitudes, -mean);

		filter(amplitudes, (float) -highPassFilterPole);
		
		if(keepSpectralFeatures){
			spectralFeatures = amplitudes.clone();
		}
	}
	
	/**
	 * Find landmarks in the audio defined by the float array audioBuffer, for a
	 * certain sample rate. The last parameter defines how many landmarks are
	 * retrieved every second (more or less).
	 * 
	 * @param audioBuffer
	 *            The float buffer with audio. E.g. 5 seconds at 8kHz takes
	 *            5x8000x32bit = 5 x 8k x 4 bytes = 156 kB, 15 seconds takes 478
	 *            kB.
	 * @param timeOffset
	 *            The offset, in analysis frames.
	 * @return A set of fingerprints found within the audio.
	 */
	public Set<FFTFingerprint> findFingerprints(float[] audioBuffer,int timeOffset){
		HashSet<FFTFingerprint> fingerprints = new HashSet<FFTFingerprint>();
		
		float[][] amplitudes;
		//1s at 8kHz = 8000 samples / 256 = 15.625
		//e.g. 1000 samples, 256 samples hop size = 1000/256 = 3 - 1 = 2
		int completeFrames = audioBuffer.length/fftHopSizeBins-1;
		amplitudes = new float[completeFrames][fftSizeBins/2];
		calculateSpectralFeatures(amplitudes, audioBuffer);
		
		List<FFTEventPoint> eventPoints = findEventPointsViaTiling(amplitudes);
		
		refineEventPointLocations(eventPoints,amplitudes);
		
		packEventPointsIntoFingerprints(eventPoints,fingerprints);
		
		return fixOffsets(limitNumberOfFingerprints(fingerprints),timeOffset);
	}
	
	private Set<FFTFingerprint> limitNumberOfFingerprints(HashSet<FFTFingerprint> fingerprints) {
		int timeWindow = 3;//seconds
		int timeWindowInSteps = (int) (timeWindow * sampleRate/(float)fftHopSizeBins);
		TreeMap<Integer,FFTFingerprint> fingerprintsOrderedByTime = new TreeMap<Integer,FFTFingerprint>();
		for(FFTFingerprint fingerprint : fingerprints){
			fingerprintsOrderedByTime.put(fingerprint.t1 + fingerprint.timeDelta()/2, fingerprint);
		}
		
		for(int i = fingerprintsOrderedByTime.firstKey() ; i < fingerprintsOrderedByTime.lastKey() - timeWindowInSteps ; i+= 100){
			if(fingerprintsOrderedByTime.subMap(i, i+timeWindowInSteps).size() > timeWindow * landmarksPerSecond){
				TreeMap<Double,FFTFingerprint> fingerprintsOrderedByEnergy = new TreeMap<Double,FFTFingerprint>();
				for(FFTFingerprint fingerprint: fingerprintsOrderedByTime.subMap(i, i+timeWindowInSteps).values()){
					fingerprintsOrderedByEnergy.put(fingerprint.energy, fingerprint);
				}
				while(fingerprintsOrderedByEnergy.size() > timeWindow * landmarksPerSecond){
					FFTFingerprint toRemove = fingerprintsOrderedByEnergy.pollFirstEntry().getValue();
					fingerprintsOrderedByTime.remove(toRemove.t1 +  toRemove.timeDelta()/2);
				}
			} 
		}
		Set<FFTFingerprint> prints = new HashSet<FFTFingerprint>();
		prints.addAll(fingerprintsOrderedByTime.values());
		return prints;
	}
	
	private Set<FFTFingerprint> fixOffsets(Set<FFTFingerprint> fingerprints,int offset) {
		if(offset == 0){
			return fingerprints;
		}
		HashSet<FFTFingerprint> fixedPrints = new HashSet<FFTFingerprint>(fingerprints.size());
		for(FFTFingerprint print : fingerprints){
			fixedPrints.add(new FFTFingerprint(print.t1+offset, print.f1, print.t2+offset, print.f2));
		}
		return fixedPrints;
	}

	private void packEventPointsIntoFingerprints(List<FFTEventPoint> eventPoints,Set<FFTFingerprint> fingerprints){
		int maxEventPointDeltaTInSteps = 100; //about two seconds
		int maxEventPointDeltaFInBins = 17; // 256 is the complete spectrum
		
		int maxFingerprintsPerEventPoint = 4;
		
		int minTimeDifference = 10;//time steps about 200ms
		//Pack the event points into fingerprints
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			int f1 = eventPoints.get(i).f;
			int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			int maxfFirstLevel = f1 + maxEventPointDeltaFInBins;
			int minfFirstLevel = f1 - maxEventPointDeltaFInBins;
			
			//A list of fingerprints Per Event Point, ordered by energy of the combined event points
			TreeMap<Float,FFTFingerprint> fingerprintsPerEventPoint = new TreeMap<Float,FFTFingerprint>();
			
			for(int j = i + 1; j < eventPoints.size()  && eventPoints.get(j).t < maxtFirstLevel;j++){
				int t2 = eventPoints.get(j).t;
				int f2 = eventPoints.get(j).f;
				if(t1 != t2 && t2 > t1 + minTimeDifference && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					float energy = eventPoints.get(i).contrast + eventPoints.get(j).contrast;
					FFTFingerprint fingerprint = new FFTFingerprint(t1, f1, t2, f2);
					fingerprint.energy = energy;
					fingerprintsPerEventPoint.put(energy,fingerprint);					
				}
			}

			if(fingerprintsPerEventPoint.size() >= maxFingerprintsPerEventPoint ){
				for(int s = 0 ; s < maxFingerprintsPerEventPoint ; s++){
					Entry<Float, FFTFingerprint> e = fingerprintsPerEventPoint.lastEntry();
					fingerprints.add(e.getValue());
					fingerprintsPerEventPoint.remove(e.getKey());
				}
			}else{
				fingerprints.addAll(fingerprintsPerEventPoint.values());	
			}
		}
	}
	
	private void refineEventPointLocations(List<FFTEventPoint> eventPoints, float[][] spectralInfo) {
		for(FFTEventPoint eventPoint : eventPoints){
			//Now we have the local maximums in spectralInfo, but to handle bin boundaries well,
			// we need to look at the surrounding bins. If they contain a lot of energy too, then
			// we should take the average of the surrounding bins and relocate the to where
			// a new maximum appears. For this we use a simple box blur of the center pixel and some 
			// surrounding pixels. The offsets indicate how much time and frequency bin index change
			int[] offsets = refineLocation(eventPoint.t,eventPoint.f,spectralInfo);
			eventPoint.t = eventPoint.t + offsets[0];
			eventPoint.f = eventPoint.f + offsets[1];
			if(eventPoint.f < 0)
				eventPoint.f = 0;
		}
	}
	
	private int[] refineLocation(int i, int j, float[][] spectralInfo) {
		//take a 3x3 kernel and use the average as a value (1/9=0.111).
		float kernel[][] = {
		        {2,  5, 2}, 
		        {5, 15, 5}, 
		        {2,  5, 2}, 
		};
	
		
		int neighbourhoudSize=7;// size of the neighbors to blur
		int halfNeighbourhoodSize = neighbourhoudSize/2;
		
		float sigma = 1.0f;
		int kernelWidth = 5;
		
		//create a gaussian kernel
		//check in octave with:
		// m = 5 ; n = 5 ; sigma = 1
		// [h1, h2] = meshgrid(-(m-1)/2:(m-1)/2, -(n-1)/2:(n-1)/2);
		// hg = exp(- (h1.^2+h2.^2) / (2*sigma^2));
		// h = hg ./ sum(hg(:));
		// h
		kernel = new float[kernelWidth][kernelWidth];
		int halfKernelRowLength = kernel.length/2;
		int halfKernelColLength = kernel[0].length/2;
		for (int x = 0; x < kernelWidth; ++x){ 
			 for (int y = 0; y < kernelWidth; ++y){
				 kernel[x][y] = (float) (Math.exp( -0.5 * ( Math.pow((x-halfKernelRowLength)/sigma, 2.0) + Math.pow((y-halfKernelColLength)/sigma, 2.0))) / (2*Math.PI * sigma * sigma)); 
			 }
		}
		
		float[][] result = new float[neighbourhoudSize][neighbourhoudSize];
		
		for(int row = 0; row < result.length; row++){
			for(int col = 0; col < result[row].length; col++){
				for(int kernelRow = 0; kernelRow < kernel.length;kernelRow++){
					//the actual row in the spectrum
					int actualRowIndex = row + i - halfNeighbourhoodSize - halfKernelRowLength + kernelRow;
					if(actualRowIndex < 0){
						actualRowIndex = 0;
					}
					if(actualRowIndex >= spectralInfo.length){
						actualRowIndex = spectralInfo.length-1;
					}
					for(int kernelCol = 0; kernelCol < kernel[kernelRow].length;kernelCol++){
						int actualColIndex = col + j - halfNeighbourhoodSize - halfKernelColLength + kernelCol;
						if(actualColIndex < 0){
							actualColIndex = 0;
						}
						if(actualColIndex >= spectralInfo[actualRowIndex].length){
							actualColIndex = spectralInfo[actualRowIndex].length-1;
						}
						float factor = kernel[kernelRow][kernelCol];
						result[row][col] = result[row][col] + factor * spectralInfo[actualRowIndex][actualColIndex];
					}
				}
			}	
		}
		
		//find the maximum in the new result
		int maxRow=-1;
		int maxCol=-1;
		float maxValue = -10000;
		for(int row = 0; row < result.length; row++){
			for(int col = 0; col < result[row].length; col++){
				if(result[row][col] > maxValue){
					maxValue = result[row][col];
					maxRow = row;
					maxCol = col;
				}
			}
		}
		
		//System.out.println(String.format("expected (%d,%d) actual (%d,%d)",halfNeighbourhoodSize,halfNeighbourhoodSize,maxRow,maxCol));
		
		int[] offsets = {maxRow - halfNeighbourhoodSize,maxCol - halfNeighbourhoodSize};
		return offsets;
	}
	
	
	private List<FFTEventPoint> findEventPointsViaTiling(float[][] amplitudes) {
		List<float[]> spectralInfo; 
		
		//copy the original spectrum
		spectralInfo = new ArrayList<float[]>();
		for(float[] spectrum:amplitudes){
			spectralInfo.add(spectrum.clone());
		}
		
		int deltaTInSteps = 10;//200ms =~ 5Hz
		int deltaFInBins = 70;//3 per frame
		
		//Mark the local maximums. 
		//Sets everything else to zero
		for(float[] spectrum:spectralInfo){
			localMaximum(spectrum);
		}
	
		//Find peaks within a local neighborhood. For every local maximum (t,f) in the spectrogram
		//Points in the region from (t-delta t,f - delta f) to (t+delta t, f + delta f) are compared and the maximum is kept.
		//TODO: include an absolute threshold to avoid very low energy event points
		for(int i = 0 ; i < spectralInfo.size() ; i++){
			for(int j = 0 ; j < spectralInfo.get(i).length; j ++){
				if(spectralInfo.get(i)[j] != 0){
					//look in the neighborhood and try to set as many fields as possible to 0
					//to avoid extra comparisons
					int minTimeIndex = Math.max(0, i-deltaTInSteps/2);
					int maxTimeIndex = Math.min(spectralInfo.size(), i+deltaTInSteps/2);
					int minPitchIndex = Math.max(0, j-deltaFInBins/2);
					int maxPitchIndex = Math.min(spectralInfo.get(i).length, j+deltaFInBins/2);
					
					int tileMaxTimeIndex = i;
					int tileMaxPitchIndex = j;
					float tileMaxValue  = spectralInfo.get(i)[j];
					for(int timeIndex = minTimeIndex ; timeIndex < maxTimeIndex ; timeIndex++){
						for(int pitchIndex = minPitchIndex ; pitchIndex < maxPitchIndex; pitchIndex++){
							float value = spectralInfo.get(timeIndex)[pitchIndex];
							if(value > tileMaxValue ){
								tileMaxValue = value;
								spectralInfo.get(tileMaxTimeIndex)[tileMaxPitchIndex]=0;
								tileMaxPitchIndex = pitchIndex;
								tileMaxTimeIndex = timeIndex;
							} else if(value != 0 && value !=tileMaxValue){
								spectralInfo.get(timeIndex)[pitchIndex]=0;
							}
						}
					}
				}
			}
		}
		
		List<FFTEventPoint> eventPoints = new ArrayList<FFTEventPoint>();
		
		//Currently the spectrogram only contains points that are maximum in their neighborhood.
		//These are the event points that need to be paired to form a hash of sorts.
		for(int i = 0 ; i < spectralInfo.size() ; i++){
			for(int j = 0 ; j < spectralInfo.get(i).length; j ++){
				if(spectralInfo.get(i)[j] != 0){
					float contrast = getContrast(i,j,spectralInfo);
					eventPoints.add(new FFTEventPoint(i,j,spectralInfo.get(i)[j],contrast));
				}
			}
		}
		
		return eventPoints;
	}
	
	private float getContrast(int i,int j,List<float[]>spectralInfo){
		float contrast = 0.0f;
		float eventPointEnergy = spectralInfo.get(i)[j];
		int neighbours = 3;
		int count = 0;
		for(int row = i - neighbours; row <= i + neighbours ; row ++){
			int actualRow = row; 
			if(actualRow < 0){
				actualRow = 0;
			}
			if(actualRow >= spectralInfo.size()){
				actualRow = spectralInfo.size()-1;
			}
			for(int col = j - neighbours ; col <= j + neighbours ; col ++){
				int actualCol = col;
				if(actualCol < 0){
					actualCol = 0;
				}
				if(actualCol >= spectralInfo.get(actualRow).length){
					actualCol = spectralInfo.get(actualRow).length-1;
				}
				if(!(actualRow == i && actualCol == j)){
					float neighborEnergy = spectralInfo.get(actualRow)[actualCol];
					contrast += Math.abs(eventPointEnergy-neighborEnergy);
					count++;
				}
			}
		}
		return contrast/(float)count +  2 * eventPointEnergy;
	}
	

	/**
	*  A high pass filter, applied in the log-magnitude
	* domain.  It blocks slowly-varying terms (like Automatic Gain Control), but also 
	* emphasizes onsets.  Placing the pole closer to the unit circle 
	* (i.e. making the -.8 closer to -1) reduces the onset emphasis.
	* 
	* <p>
	* The filter is implemented using the following equation:
	* <pre>
	*          N                   M
	*y(n) = - SUM c(k+1) y(n-k) + SUM d(k+1) x(n-k)  for 0&#60;=n&#60;length(x)
	*         k=1                 k=0
	*
	*a = [1, hpf_pole]
	*b = [1, -1]
	*c = [1/a[0], hpf_pole/a[0]] = a / a[0] = a
	*d = [1/a[0], -1/a[0]] = b / a[0] = b
	*
	*N = |a| - 1 = 2
	*M = |b| - 1 = 2
	*</pre>
	*
	 * @param amplitudes log
	 * @param hpf_pole
	 */
	private void filter(float[][] amplitudes, float hpf_pole) {		
		for(int i = 0 ; i < amplitudes[0].length;i++){
			float prevX = amplitudes[0][i];
			for(int j = 1 ; j < amplitudes.length; j++){
				float filteredSample = -hpf_pole * amplitudes[j-1][i] + amplitudes[j][i] + -prevX;
				prevX = amplitudes[j][i];
				amplitudes[j][i] = filteredSample;
			}
		}
	}
	
	/**
	 * Transforms x so that it only contains the points in (vector) X which are local maxima
	 * @param x the vector to check
	 */
	private void localMaximum(float[] x){		
		//stores true if the value at this index is larger or equal to the next, false otherwise.
		boolean currentLargerOrEqualThanNext = x[0]<x[1];
		boolean currentLargerOrEqualThanPrev = true;
		for(int i = 1 ; i < x.length - 1 ; i ++){
			boolean newCurrentLargerOrEqualThanPrev = x[i] >= x[i-1];
			if(!(!currentLargerOrEqualThanNext && currentLargerOrEqualThanPrev)){
				x[i-1]=0;
			}
			currentLargerOrEqualThanPrev = newCurrentLargerOrEqualThanPrev;
			currentLargerOrEqualThanNext = x[i+1] >= x[i];
		}
		if(!(!currentLargerOrEqualThanNext && currentLargerOrEqualThanPrev)){
			x[x.length-2]=0;
		}
		//makes sure the last value is no local max.
		x[x.length-1]=0;			
	}
	
	
	private float mean(float[][] amplitudes){
		float sum = 0;
		int count = 0;
		for(int i = 0 ; i < amplitudes.length ; i ++){
			for(int j = 0 ; j < amplitudes[i].length ; j ++){
				sum += amplitudes[i][j];
				count++;
			}
		}
		return sum/(float) count;
	}
	
	private void add(float[][] amplitudes,float value){
		for(int i = 0 ; i < amplitudes.length ; i ++){
			for(int j = 0 ; j < amplitudes[i].length ; j ++){
				amplitudes[i][j] = amplitudes[i][j] + value;
			}
		}
	}
	
	private void removeZeroValues(float[][] amplitudes){
		float minValue = 5/1000000.0f;
		for(int i = 0 ; i < amplitudes.length ; i ++){
			for(int j = 0 ; j < amplitudes[i].length ; j ++){
				if(amplitudes[i][j] < minValue){
					amplitudes[i][j] = minValue;
				}
			}
		}
	}
	private void log(float[][] amplitudes){
		for(int i = 0 ; i < amplitudes.length ; i ++){
			for(int j = 0 ; j < amplitudes[i].length ; j ++){
				amplitudes[i][j] = (float) Math.log(amplitudes[i][j]);
			}
		}
	}
	
	public float[][] getSpectralFeatures() {
		return spectralFeatures;
	}
	
	public static Set<FFTFingerprint> calculateLandmarks(float[] audioBuffer, float sampleRate,int landmarksPerSecond,int offset){
		FFTFingerprintExtractorNaive explorer = new FFTFingerprintExtractorNaive(sampleRate, landmarksPerSecond);
		return explorer.findFingerprints(audioBuffer,offset);
	}
	
	public void keepSpectralFeatures() {
		keepSpectralFeatures = true;
	}
}
