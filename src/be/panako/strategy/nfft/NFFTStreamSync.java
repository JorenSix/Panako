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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class NFFTStreamSync {

	private static String prevReference;
	private static List<NFFTFingerprint> prevReferencePrints;

	private final String reference;
	private final String other;

	private NFFTSyncMatch match;

	// fingerprints to sync are only used between 100 and 4000Hz.
	private static final float minFrequency = 100;// Hz
	private static final float maxFrequency = 4000;// Hz

	public NFFTStreamSync(String reference, String other) {
		this.reference = reference;
		this.other = other;
		match = null;
	}

	private List<NFFTFingerprint> extractFingerprints(String resource) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);

		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap);
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size, overlap, samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		return new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
	}

	public void synchronize() {
		// extract fingerprints for reference audio
		// if needed, the reference prints are cached.
		List<NFFTFingerprint> referencePrints;
		if (reference.equals(prevReference)) {
			referencePrints = prevReferencePrints;
			System.out.println("Using cached reference prints");
		} else {
			referencePrints = extractFingerprints(reference);
			filterPrints(referencePrints);
			prevReferencePrints = referencePrints;
			prevReference = reference;
		}

		// extract fingerprints for other audio stream				
		List<NFFTFingerprint> extracted = extractFingerprints(other);
		filterPrints(extracted);
		
		match(referencePrints, extracted);
	}

	private void filterPrints(List<NFFTFingerprint> prints) {
		float samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		float size = Config.getInt(Key.NFFT_SIZE);
		int minf = (int) Math.ceil(minFrequency / (samplerate / size));
		int maxf = (int) Math.floor(maxFrequency / (samplerate / size));
		int numberRemoved = 0;
		int prevSize = prints.size();
		for (int i = 0; i < prints.size(); i++) {
			NFFTFingerprint print = prints.get(i);
			boolean smallerThanMin = print.f1 <= minf || print.f2 <= minf;
			boolean biggerThanMax = print.f1 >= maxf || print.f2 >= maxf;
			if (smallerThanMin || biggerThanMax) {
				prints.remove(i);
				i--;
				numberRemoved++;
			}
		}
		System.out.println("Filtered " + numberRemoved + " of " + prevSize + " fingerprints");
	}

	private void match(List<NFFTFingerprint> referencePrints,List<NFFTFingerprint> otherPrints) {
		// create a map with the fingerprint hash as key, and fingerprint object
		// as value.
		// Warning: only a single object is kept for each hash.
		HashMap<Integer, NFFTFingerprint> referenceHash = fingerprintsToHash(referencePrints);

		HashMap<Integer, NFFTFingerprint> otherHash = fingerprintsToHash(otherPrints);
		printOffset(referenceHash, otherHash);		
	}

	private void printOffset(HashMap<Integer, NFFTFingerprint> referenceHash,
			HashMap<Integer, NFFTFingerprint> otherHash) {
		// key is the offset, value a list of fingerprint objects.
		HashMap<Integer, List<NFFTFingerprint>> mostPopularOffsets = new HashMap<Integer, List<NFFTFingerprint>>();
		
		int minimumAlignedMatchesThreshold = Config.getInt(Key.SYNC_MIN_ALIGNED_MATCHES);
		int maxAlignedOffsets = 0;

		List<NFFTFingerprint> matchingPairs = null;
		int bestOffset = -1;

		int numberOfMatchingHashes = 0;
		// iterate each fingerprint in the reference stream
		for (Map.Entry<Integer, NFFTFingerprint> entry : referenceHash.entrySet()) {
			// if the fingerprint is also present in the other stream
			if (otherHash.containsKey(entry.getKey())) {

				NFFTFingerprint referenceFingerprint = entry.getValue();
				NFFTFingerprint otherFingerprint = otherHash.get(entry.getKey());
				int offset = referenceFingerprint.t1 - otherFingerprint.t1;
				// add the offset to the tree, if it is not already in the tree.
				if (!mostPopularOffsets.containsKey(offset)) {
					mostPopularOffsets.put(offset, new ArrayList<NFFTFingerprint>());
				}
				// add the reference and other fingerprint to the list.
				// add the other fingerprint to the list.
				// the reference fingerprints are at even, the other ad odd
				// indexes.
				mostPopularOffsets.get(offset).add(referenceFingerprint);
				mostPopularOffsets.get(offset).add(otherFingerprint);

				// keep a max count
				if (mostPopularOffsets.get(offset).size() / 2 > maxAlignedOffsets) {
					maxAlignedOffsets = mostPopularOffsets.get(offset).size() / 2;
					//keep a reference to the best offset, matching pairs
					matchingPairs = mostPopularOffsets.get(offset);
					bestOffset = offset;
				}
				numberOfMatchingHashes++;
			}
		}
		System.out.println("Number of matching hashes between reference and query: " + numberOfMatchingHashes + " aligned: " + maxAlignedOffsets);
		
		if (maxAlignedOffsets >= minimumAlignedMatchesThreshold) {

			int minReferenceFingerprintTimeIndex = Integer.MAX_VALUE;
			int minOtherFingerprintTimeIndex = Integer.MAX_VALUE;
			int maxReferenceFingerprintTimeIndex = Integer.MIN_VALUE;
			int maxOtherFingerprintTimeIndex = Integer.MIN_VALUE;
			// find where the offset matches start and stop
			for (int i = 0; i < matchingPairs.size(); i += 2) {
				NFFTFingerprint refFingerprint = matchingPairs.get(i);
				NFFTFingerprint otherFingerprint = matchingPairs.get(i + 1);
				minReferenceFingerprintTimeIndex = Math.min(refFingerprint.t1, minReferenceFingerprintTimeIndex);
				minOtherFingerprintTimeIndex = Math.min(otherFingerprint.t1, minOtherFingerprintTimeIndex);
				maxReferenceFingerprintTimeIndex = Math.max(refFingerprint.t1, maxReferenceFingerprintTimeIndex);
				maxOtherFingerprintTimeIndex = Math.max(otherFingerprint.t1, maxOtherFingerprintTimeIndex);
			}

			int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
			float fftHopSizesS = Config.getInt(Key.NFFT_STEP_SIZE) / (float) samplerate;

			double minReferenceFingerprintTime = minReferenceFingerprintTimeIndex * fftHopSizesS;
			double minOtherFingerprintTime = minOtherFingerprintTimeIndex * fftHopSizesS;
			
			double maxReferenceFingerprintTime = maxReferenceFingerprintTimeIndex * fftHopSizesS;
			double maxOtherFingerprintTime = maxOtherFingerprintTimeIndex * fftHopSizesS;
			

			String referenceFileName = new File(reference).getName();
			String otherFileName = new File(other).getName();

			// This method doesn't use the refined offset:
			// match.addMatch((float) minReferenceFingerprintTime, (float)
			// maxReferenceFingerprintTime, (float) minOtherFingerprintTime,
			// (float) maxOtherFingerprintTime,matchingPairs.size()/2);

			// Adding the refined result from the
			// refineMatchWithCrossCovariance method.
			double roughOffsetInSeconds = bestOffset * fftHopSizesS;
			
			double refinedOffset = refineMatchWithCrossCovariance(minReferenceFingerprintTimeIndex, minOtherFingerprintTimeIndex,0);
			
			NFFTSyncMatch match = new NFFTSyncMatch(reference, other,
					minReferenceFingerprintTime,maxReferenceFingerprintTime,
					minOtherFingerprintTime,maxOtherFingerprintTime,
					matchingPairs.size() / 2,
					roughOffsetInSeconds,
					refinedOffset);
			this.match = match;

			String message = String.format(
					"%s [%.1fs - %.1fs] matches %s [%.1fs - %.1fs] with an offset of %.4fs (%d matches) - fingerprint offset: %.4fs",
					referenceFileName, minReferenceFingerprintTime, maxReferenceFingerprintTime, otherFileName,
					minOtherFingerprintTime, maxOtherFingerprintTime, refinedOffset, matchingPairs.size() / 2, roughOffsetInSeconds);
			System.out.println(message);

		} else {
			System.out.println("No alignment found");
		}
	}

	private HashMap<Integer, NFFTFingerprint> fingerprintsToHash(List<NFFTFingerprint> fingerprints) {
		HashMap<Integer, NFFTFingerprint> hash = new HashMap<>();
		for (NFFTFingerprint fingerprint : fingerprints) {
			hash.put(fingerprint.hash(), fingerprint);
		}
		return hash;
	}

	public NFFTSyncMatch getMatch() {
		return match;
	}

	double referenceAudioStart = 0;
	double otherAudioStart = 0;
	
	public static float[] getAudioData(String fileName, int sampleRate, double startTime, double duration){
	
		int durationInSamples = (int) Math.round(duration * sampleRate);
		final float[] audioData = new float[durationInSamples];
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(fileName, sampleRate, durationInSamples, 0,startTime,duration);
		d.addAudioProcessor(new AudioProcessor() {
			int counter = 0;
			@Override
			public void processingFinished() {
				//only one pass is expected!
				assert counter == 1;
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] audioBuffer = audioEvent.getFloatBuffer();
				for(int i  = 0 ; i < audioBuffer.length ; i++){
					audioData[i] = audioBuffer[i];
				}
				counter ++;
				return true;
			}
		});
		d.run();
		return audioData;
	}

	private double refineMatchWithCrossCovariance(int referenceBlockIndex, int otherBlockIndex,int retry) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		
		float sizeS = size / (float) samplerate;//in seconds
		float fftHopSizesS = Config.getInt(Key.NFFT_STEP_SIZE) / (float) samplerate;//in seconds
		
		int sampleRateForCovariance = 10000;
		
		//start a bit a 'sizeS' early so that the other audio always starts later
		// or the covariance lag is always positive
		final double referenceAudioStart = referenceBlockIndex * fftHopSizesS + retry;
		final double referenceAudioDuration = 0.5; //make sure the other audio is included
		final float[] referenceAudioFrame = getAudioData(reference, sampleRateForCovariance, referenceAudioStart, referenceAudioDuration);		
		
		final double otherAudioStart = sizeS + otherBlockIndex * fftHopSizesS + retry;
		final double otherAudioDuration = 0.2;
		final float[] otherAudioFrame = getAudioData(other, sampleRateForCovariance, otherAudioStart, otherAudioDuration);
		
		// lag in samples, determines how many samples the other audio frame
		// lags with respect to the reference audio frame.
		// is always positive or zero
		int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);
		assert lag >= 0;
		
		//subtract the sizeS value that was added to the reference start
		// the lag in seconds can be negative again
		double lagInSeconds = lag/10000.0 - sizeS;
		
		double offsetFromMatching = (referenceBlockIndex - otherBlockIndex) * fftHopSizesS; //in seconds
		double refinedOffset = offsetFromMatching + lagInSeconds;
		
		// if the difference between offset and refined offset
		// is bigger than one fft block step then something went
		// wrong during calculation of cross covariance
		if( Math.abs(offsetFromMatching - refinedOffset) > 2 * sizeS){
			System.err.println(String.format("Refined offset %.4fs differs more than two blocks (%.4fs) from fft offset  %.4fs. Retry %d ",refinedOffset,2*sizeS,offsetFromMatching,retry));
			if(retry < 5){
				refinedOffset = refineMatchWithCrossCovariance(referenceBlockIndex,otherBlockIndex,retry+1);
			}else{
				System.err.println(String.format("Did not find correct cross covar after 5 retries => fall back to fft offset"));
				refinedOffset = offsetFromMatching;
			}			
		}
		return refinedOffset;
	}

	/**
	 * Finds the index of the best match of target in reference as defined by the cross covariance. 
	 * @param reference The buffer with the reference time value signal.
	 * @param target The target time value signal.
	 * @return The index at which target is most similar to reference (as defined by the cross covariance).
	 */
	public static int bestCrossCovarianceLag(float[] reference, float[] target) {
		
		double maxCovariance = -10000000;
		int maxCovarianceIndex = -1;
	
		for (int i = 0; i < reference.length; i++) {
			double covariance = covariance(reference, target, i);
			if (covariance > maxCovariance) {
				maxCovarianceIndex = i;
				maxCovariance = covariance;
			}
		}		
		return maxCovarianceIndex;
	}


	public static double covariance(float[] reference, float[] target, int lag) {
		double covariance = 0.0;
		//int i; 
		for (int i= 0; i < target.length && lag + i < reference.length; i++) {
			covariance += reference[lag+i] * target[i];
		}
		/*
		// The factor multiplies the covariance with the number of samples
		// processed divided by the total number of samples in the query. This
		// is to allow matches without using the complete query information.
		// This can be seen as a kind of normalisation operation.
		// Do not allow less than 50 samples to prevent false positives (if i =
		// 0 then factor (and covariance) is infinite.
		final double factor;
		if(i < 50){
			factor = 0;
		}else{
			factor = target.length / (double)i;	
		}
		 */
		return covariance;
	}
}
