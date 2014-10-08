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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class FFTFingerprintExtractor {
	
	
	/** The factors influencing the number of fingerprints returned are:
	*  A.  The number of local maxima found, which in turn depends on 
	*    A.1 The spreading width applied to the masking skirt from each
	*        found peak (gaussian half-width in frequency bins).  A
	*        larger value means fewer peaks found.
	*/
	int spreadingWidth = 30;

	/**    A.2 The decay rate of the masking skirt behind each peak
	*        (proportion per frame).  A value closer to one means fewer
	*        peaks found.
	*/        
	float  decayRate;
	/* 0.999 -> 2.5
	* 0.998 -> 5 hash/sec
	* 0.997 -> 10 hash/sec
	* 0.996 -> 14 hash/sec
	* 0.995 -> 18
	* 0.994 -> 22
	* 0.993 -> 27
	* 0.992 -> 30
	* 0.991 -> 33
	* 0.990 -> 37
	* 0.98  -> 67
	* 0.97  -> 97
	*/
	
	/**    A.3 The maximum number of peaks allowed for each frame.  In
	*        practice, this is rarely reached, since most peaks fall
	*        below the masking skirt
	*/        
	int  maxPeaksPerFrame = 5;

	/**    A.4 The high-pass filter applied to the log-magnitude
	*        envelope, which is parameterized by the position of the
	*        single real pole.  A pole close to +1.0 results in a
	*        relatively flat high-pass filter that just removes very
	*        slowly varying parts; a pole closer to -1.0 introduces
	*        increasingly extreme emphasis of rapid variations, which
	*        leads to more peaks initially.
	*/
	float  highPassFilterPole = 0.98f;

	/**  B. The number of pairs made with each peak.  All maxes within a
	*     "target region" following the seed max are made into pairs,
	*     so the larger this region is (in time and frequency), the
	*     more maxes there will be.  The target region is defined by a
	*     freqency half-width (in bins)
	*/     
	int  targetFrequencyDifference = 31;  // +/- 50 bins in freq (LIMITED TO -32..31 ?)

	/**
	 * .. and a time duration (maximum look ahead)
	 */
	int targetTimeDifference = 63;  // (LIMITED TO <64 ?)

	/**     The actual frequency and time differences are quantized and
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
	
	/**
	 * Contains the kernel used to convolve with in spread.
	 */
	final float[] kernel;
	
	
	//keep the spectral features
	private boolean keepSpectralFeatures = false;
	private float[][] spectralFeatures;
	
	/**
	 * Disables public access to default constructor
	 * @param sampleRate The sample rate in samples per second.
	 * @param fingerprintsPerSecond The requested maximum number of landmarks per second. The actual number of landmarks depend on the input: e.g. generating landmarks on total silence generates not much landmarks.  
	 */
	public FFTFingerprintExtractor(float sampleRate,int fingerprintsPerSecond){
		this.sampleRate = sampleRate;
		this.landmarksPerSecond = fingerprintsPerSecond;
		decayRate = 1-0.01f*(fingerprintsPerSecond/35.0f);
		// 
		
		fftSizeBins = Config.getInt(Key.FFT_SIZE);
		fftHopSizeBins = Config.getInt(Key.FFT_STEP_SIZE);
		
		//initialize the spreading kernel.
		int w = 4* spreadingWidth;
		kernel = new float[w*2+1];
		for(int i = -w ; i <= w;i++){
			float t = i/(float)spreadingWidth;
			kernel[i+w] = (float)Math.exp(-0.5 * t * t);			
		}
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
		
		final float[][] amplitudes; 
		
		//1s at 8kHz = 8000 samples / 256 = 15.625
		//e.g. 1000 samples, 256 samples hop size = 1000/256 = 3 - 1 = 2
		int completeFrames = audioBuffer.length/fftHopSizeBins-1;
		amplitudes = new float[completeFrames][fftSizeBins/2];
		calculateSpectralFeatures(amplitudes, audioBuffer);
		
		int maxFingerpintsPerSec = 30;

		float duration = audioBuffer.length/sampleRate;
		int maxFingerprints = (int) Math.round(maxFingerpintsPerSec * duration) + 1;
		float fingerprints[][] = new float[maxFingerprints][3];
		int nmaxes = 0;
		
		
		//find all the local prominent peaks, store in landmarks;		
		float[] sthresh = new float[amplitudes[0].length];
		for(int i = 0 ; i < 10 ; i ++){
			for(int j = 0 ; j < amplitudes[i].length ; j ++){
				sthresh[j] = Math.max(amplitudes[i][j],sthresh[j]);
			}
		}
		sthresh = spread(sthresh);
		
		
		for(int i = 0; i < amplitudes.length - 1; i++){
			float[] s_this = amplitudes[i];
			final float[] s_diff = new float[amplitudes[i].length];
			Integer[] indexSorter = new Integer[amplitudes[i].length];
			for(int j = 0 ; j < s_diff.length ; j ++){
				s_diff[j] =  Math.max(0, s_this[j]-sthresh[j]);
				indexSorter[j]=j;
			}
			//find local maxima
			localMaximum(s_diff);
			
			//sort indexes so that values of s_diff are in descending order 
			Arrays.sort(indexSorter,new Comparator<Integer>() {
				@Override
				public int compare(Integer o1, Integer o2) {
					float one = s_diff[o1];
					float other = s_diff[o2];
					int value = 1;
					if(one == other){
						value = 0;
					} else if (one < other){
						value = -1;
					}
					return value;
				}
			});
			
			int nmaxthistime = 0;
			for(int j = 0 ; j < s_diff.length ; j ++){
				int p = indexSorter[j];
				//Check to see if this peak is under our updated threshold
				//Do not allow more than maxpksperframe maxes				 
				if(nmaxes < fingerprints.length && s_diff[p] > 0 && nmaxthistime < maxPeaksPerFrame && s_this[p] > sthresh[p]){
	
					nmaxthistime++;
					
					fingerprints[nmaxes][0] = i;
					fingerprints[nmaxes][1] = p;					
					fingerprints[nmaxes][2] = s_this[p];
					nmaxes++;
					for(int k = 0; k < sthresh.length ; k++){
						float val = (k - p)/((float)spreadingWidth);
						float eew = (float) Math.exp(-0.5 * val * val);
						sthresh[k] = Math.max(sthresh[k],s_this[p]*eew);
					}
				}
			}
			for(int k = 0; k < sthresh.length ; k++){
				sthresh[k] = decayRate * sthresh[k];
			}
		}
		
		float[] lastColumn = new float[amplitudes[0].length-1];
		for(int i = 0; i <lastColumn.length;i++){
			lastColumn[i] = amplitudes[amplitudes.length-1][i];
		}
		sthresh = spread(lastColumn);
		
		List<int[]> maxes2 = new ArrayList<int[]>();
		
		
		// Backwards pruning of maxes
		int whichmax = nmaxes-1;
		for(int i = amplitudes.length - 1 ;i >= 0; i--){
			while(whichmax >= 0 && fingerprints[whichmax][0] == i){
				float p = fingerprints[whichmax][1];
				float v = fingerprints[whichmax][2];
				if(v>=sthresh[(int) p]){
					//keep this one
					int[] values = {(int)i,(int)p};
					
					for(int k = 0; k < sthresh.length ; k++){
						float val = (k-p)/((float)spreadingWidth);
						float eew = (float) Math.exp(-0.5 * val * val);
						sthresh[k] = Math.max(sthresh[k],v * eew);
					}
					maxes2.add(values);
				}
				whichmax--;
			}
			for(int k = 0; k < sthresh.length ; k++){
				sthresh[k] = decayRate * sthresh[k];
			}
		}
	
		Collections.reverse(maxes2);		
		
		//Pack the maxes into nearby pairs = fingerprints
		  
		//Limit the number of pairs that we'll accept from each peak
		int maxpairsperpeak=3;

		Set<FFTFingerprint> L = new HashSet<FFTFingerprint>();
		
		for(int i = 0; i < maxes2.size();i++){
			int startt = maxes2.get(i)[0];
			int F1 = maxes2.get(i)[1];
			int maxt = (int) (startt + targetTimeDifference);
			int maxf = (int) (F1 + targetFrequencyDifference);
			int minf = (int) (F1 - targetFrequencyDifference);
			List<int[]> matchmaxs = new ArrayList<int[]>();
			for(int j = 0; j < maxes2.size();j++){
				int time = maxes2.get(j)[0];
				int frequency = maxes2.get(j)[1];				
				if(time>startt && time < maxt && frequency > minf && frequency < maxf){
					matchmaxs.add(maxes2.get(j));
				}				
			}
			if(matchmaxs.size()>maxpairsperpeak){
				matchmaxs = matchmaxs.subList(0, maxpairsperpeak);
			}
			for(int[] match : matchmaxs){
				int t1 = startt + timeOffset;
				int f1 = F1;
				int t2 = match[0] + timeOffset;
				int f2 = match[1];
				int df = Math.abs(f1-f2);
				//some fingerprints are better than the other ;)
				//do not allow fingerprints with a low frequency component (f1 < 15 and small frequency difference df<2)
				if(df!=0 && ( (f1 > 20 && f1 < 249) || df > 2) ){
					FFTFingerprint l = new FFTFingerprint(t1,f1,t2,f2);
					L.add(l);
				}
			}
		}
		return L;
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
	 * Each point (maxima) in X is "spread" (convolved) with the profile E; Y is
	 * the pointwise max of all of these. If E is a scalar, it's the SD of a
	 * gaussian used as the spreading function (default 4).
	 * 
	 * @param values
	 * @return a spreaded vector.
	 */
	private float[] spread(float[] values){
		localMaximum(values);
		float[] y = new float[values.length];
		int spos = Math.round((kernel.length-1)/2);
		float eeValue = 0;
		for(int i = 0 ; i < values.length ; i++){
			if(values[i]!=0){
				for(int j=0;j<y.length;j++){
					int kernelIndex = i-j+spos;
					if(kernelIndex >= 0 && kernelIndex < kernel.length){
						eeValue = kernel[kernelIndex];
						y[j]=Math.max(y[j],values[i]*eeValue);
					}		
				}
			}
		}
		return y;
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
		FFTFingerprintExtractor explorer = new FFTFingerprintExtractor(sampleRate, landmarksPerSecond);
		return explorer.findFingerprints(audioBuffer,offset);
	}
	
	public void keepSpectralFeatures() {
		keepSpectralFeatures = true;
	}
}
