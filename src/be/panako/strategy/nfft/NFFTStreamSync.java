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
	private final String[] others;

	private final List<NFFTSyncMatch> matches;

	// fingerprints to sync are only used between 100 and 4000Hz.
	private static final float minFrequency = 100;// hz
	private static final float maxFrequency = 4000;// hz

	public NFFTStreamSync(String reference, String[] others) {
		this.reference = reference;
		this.others = others;
		matches = new ArrayList<NFFTSyncMatch>();
	}

	private List<NFFTFingerprint> extractFingerprints(String resource) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);

		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap);
		// d.skip(millisecondsSkipped/1000.0f);
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size, overlap, samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		return new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
	}

	public void synchronize() {
		// extract fingerprints for reference audio
		// if needed, the reference prints are cached.
		List<NFFTFingerprint> referencePrints;
		if (reference == prevReference) {
			referencePrints = prevReferencePrints;
			System.out.println("Using cached reference prints");
		} else {
			referencePrints = extractFingerprints(reference);
			filterPrints(referencePrints);
			prevReferencePrints = referencePrints;
			prevReference = reference;
		}

		// extract fingerprints for other audio streams
		List<List<NFFTFingerprint>> otherPrints = new ArrayList<List<NFFTFingerprint>>();
		for (String other : others) {
			List<NFFTFingerprint> extracted = extractFingerprints(other);
			filterPrints(extracted);
			otherPrints.add(extracted);
		}
		// match the reference with all other streams

		match(referencePrints, otherPrints);
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

	private void match(List<NFFTFingerprint> referencePrints, List<List<NFFTFingerprint>> otherPrints) {
		// create a map with the fingerprint hash as key, and fingerprint object
		// as value.
		// Warning: only a single object is kept for each hash.
		HashMap<Integer, NFFTFingerprint> referenceHash = fingerprintsToHash(referencePrints);

		int otherIndex = 0;
		for (List<NFFTFingerprint> otherPrint : otherPrints) {
			HashMap<Integer, NFFTFingerprint> otherHash = fingerprintsToHash(otherPrint);
			printOffset(referenceHash, otherHash, otherIndex);
			otherIndex++;
		}
	}

	private void printOffset(HashMap<Integer, NFFTFingerprint> referenceHash,
			HashMap<Integer, NFFTFingerprint> otherHash, int otherIndex) {
		// key is the offset, value a list of fingerprint objects.
		HashMap<Integer, List<NFFTFingerprint>> mostPopularOffsets = new HashMap<Integer, List<NFFTFingerprint>>();
		int minimumAlignedMatchesThreshold = Config.getInt(Key.SYNC_MIN_ALIGNED_MATCHES);
		int maxAlignedOffsets = 0;

		NFFTSyncMatch match = new NFFTSyncMatch(reference, others[otherIndex]);
		matches.add(match);

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
				}
				numberOfMatchingHashes++;
			}
		}
		System.out.println("Number of matching hashes between reference and query: " + numberOfMatchingHashes
				+ " aligned: " + maxAlignedOffsets);

		boolean onlyKeepBestAlignedMatch = true;
		int removeBelow = minimumAlignedMatchesThreshold;
		if (onlyKeepBestAlignedMatch) {
			removeBelow = maxAlignedOffsets;
		}
		if (maxAlignedOffsets >= minimumAlignedMatchesThreshold) {
			// remove each offset below the minimum threshold
			List<Integer> offsetsToRemove = new ArrayList<Integer>();
			for (Map.Entry<Integer, List<NFFTFingerprint>> entry : mostPopularOffsets.entrySet()) {
				if (entry.getValue().size() / 2 < removeBelow) {
					offsetsToRemove.add(entry.getKey());
				}
			}
			for (Integer offsetToRemove : offsetsToRemove) {
				mostPopularOffsets.remove(offsetToRemove);
			}

			// now only 'real' matching offsets remain in the list. These need
			// to be refined and reported.
			for (Map.Entry<Integer, List<NFFTFingerprint>> entry : mostPopularOffsets.entrySet()) {
				// int offset = entry.getKey();//offset in blocks
				List<NFFTFingerprint> matchingPairs = entry.getValue();

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

				double offsetAtStart = refineMatchWithCrossCovariance(minReferenceFingerprintTimeIndex,
						minOtherFingerprintTimeIndex, otherIndex);
				double offsetAtEnd = refineMatchWithCrossCovariance(maxReferenceFingerprintTimeIndex,
						maxOtherFingerprintTimeIndex, otherIndex);
				double refinedOffset = -100000;

				int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
				float fftHopSizesS = Config.getInt(Key.NFFT_STEP_SIZE) / (float) samplerate;

				double refinedOffsetErrorAllowed = 4 / (float) samplerate;

				// allow 4 samples error
				if (Math.abs(offsetAtEnd - offsetAtStart) > refinedOffsetErrorAllowed) {
					System.out.println("Offset at start not the same as offset at end of match!");

					// Look for a refined offset that appears more than once.
					// If it is within an error of 4 samples it is probably
					// correct.
					List<Double> refinedOffsets = new ArrayList<Double>();
					refinedOffsets.add(offsetAtStart);
					refinedOffsets.add(offsetAtEnd);
					for (int i = 2; i < matchingPairs.size() - 2; i += 2) {
						NFFTFingerprint refFingerprint = matchingPairs.get(i);
						NFFTFingerprint otherFingerprint = matchingPairs.get(i + 1);
						double refinedOffsetAtIndex = refineMatchWithCrossCovariance(refFingerprint.t1,
								otherFingerprint.t1, otherIndex);
						boolean matchesOtherOffset = false;
						for (Double alreadyFoundRefindOffsets : refinedOffsets) {
							if (Math.abs(
									refinedOffsetAtIndex - alreadyFoundRefindOffsets) <= refinedOffsetErrorAllowed) {
								matchesOtherOffset = true;
							}
						}
						if (matchesOtherOffset) {
							refinedOffset = refinedOffsetAtIndex;
							break;
						}
						refinedOffsets.add(refinedOffsetAtIndex);
					}
				} else {
					refinedOffset = (offsetAtEnd + offsetAtStart) / 2.0;
				}

				if (refinedOffset == -100000) {
					System.out.println("No matching offset found using crosscovariance.");
					refinedOffset = offsetAtStart;
				}

				double minReferenceFingerprintTime = minReferenceFingerprintTimeIndex * fftHopSizesS;
				double minOtherFingerprintTime = minOtherFingerprintTimeIndex * fftHopSizesS;
				double maxReferenceFingerprintTime = maxReferenceFingerprintTimeIndex * fftHopSizesS;
				double maxOtherFingerprintTime = maxOtherFingerprintTimeIndex * fftHopSizesS;

				String referenceFileName = new File(reference).getName();
				String otherFileName = new File(others[otherIndex]).getName();

				// This method doesn't use the refined offset:
				// match.addMatch((float) minReferenceFingerprintTime, (float)
				// maxReferenceFingerprintTime, (float) minOtherFingerprintTime,
				// (float) maxOtherFingerprintTime,matchingPairs.size()/2);

				// Adding the refined result from the
				// refineMatchWithCrossCovariance method.
				match.addMatch((float) minReferenceFingerprintTime, (float) maxReferenceFingerprintTime,
						(float) (minReferenceFingerprintTime - refinedOffset),
						(float) (maxReferenceFingerprintTime - refinedOffset), matchingPairs.size() / 2);

				String message = String.format(
						"%s [%.1fs - %.1fs] matches %s [%.1fs - %.1fs] with an offset of %.4fs (%d matches)",
						referenceFileName, minReferenceFingerprintTime, maxReferenceFingerprintTime, otherFileName,
						minOtherFingerprintTime, maxOtherFingerprintTime, refinedOffset, matchingPairs.size() / 2);
				System.out.println(message);

			}

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

	public List<NFFTSyncMatch> getMatches() {
		return matches;
	}

	double referenceAudioStart = 0;
	double otherAudioStart = 0;

	private double refineMatchWithCrossCovariance(int referenceTime, int otherTime, int otherIndex) {
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);

		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);

		float sizeS = size / (float) samplerate;
		float fftHopSizesS = Config.getInt(Key.NFFT_STEP_SIZE) / (float) samplerate;

		final float[] referenceAudioFrame = new float[size];
		final double referenceAudioToSkip = sizeS + referenceTime * fftHopSizesS;

		AudioDispatcher d = AudioDispatcherFactory.fromPipe(reference, samplerate, size, overlap);
		d.addAudioProcessor(new AudioProcessor() {

			@Override
			public void processingFinished() {
			}

			@Override
			public boolean process(AudioEvent audioEvent) {

				if (Math.abs(audioEvent.getTimeStamp() - referenceAudioToSkip) < 0.00001) {
					referenceAudioStart = audioEvent.getTimeStamp();
					// System.out.println("r " + audioEvent.getTimeStamp() + " "
					// + referenceAudioStart);
					float[] buffer = audioEvent.getFloatBuffer();
					for (int i = 0; i < buffer.length; i++) {
						referenceAudioFrame[i] = buffer[i];
					}
					return false;
				}
				return true;
			}
		});
		d.run();

		final float[] otherAudioFrame = new float[size];
		final double otherAudioToSkip = sizeS + otherTime * fftHopSizesS;

		d = AudioDispatcherFactory.fromPipe(others[otherIndex], samplerate, size, overlap);
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
			}

			@Override
			public boolean process(AudioEvent audioEvent) {
				if (Math.abs(audioEvent.getTimeStamp() - otherAudioToSkip) < 0.00001) {
					// System.out.println("o " + audioEvent.getTimeStamp() + " "
					// + otherAudioToSkip);
					otherAudioStart = audioEvent.getTimeStamp();
					float[] buffer = audioEvent.getFloatBuffer();
					for (int i = 0; i < buffer.length; i++) {
						otherAudioFrame[i] = buffer[i];
					}
					return false;
				}
				return true;

			}
		});

		d.run();

		// lag in samples, determines how many samples the other audio frame
		// lags with respect to the reference audio frame.
		int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);

		// double offsetFramesInSeconds = (referenceTime - otherTime) *
		// fftHopSizesS;
		double offsetStartEvent = referenceAudioStart - otherAudioStart;

		// lag in seconds
		double offsetLagInSeconds = (size - lag) / (float) samplerate;
		double offsetLagInSeconds2 = lag / (float) samplerate;

		// Happens when the fingerprint algorithm underestimated the real latency
		double offsetTotalInSeconds1 = offsetStartEvent + offsetLagInSeconds; 
		
		// Happens when the fingerprint algorithm overestimated the real latency
		double offsetTotalInSeconds2 = offsetStartEvent - offsetLagInSeconds2; 
		double offsetFromMatching = (referenceTime - otherTime) * fftHopSizesS;

		// Calculating the difference between the fingerprint match and the
		// covariance results.
		double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
		double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

		double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;

		// lag is wrong if lag introduces a larger offset than algorithm:
		if (Math.abs(offsetFromMatching - offsetTotalInSeconds) >= 2 * fftHopSizesS) {
			offsetTotalInSeconds = offsetFromMatching;
			System.err.println("Covariance lag incorrect!");
		}

		return offsetTotalInSeconds;

	}

	public int bestCrossCovarianceLag(float[] reference, float[] target) {
		double[] covariances = crossCovariance(reference, target);
		double maxCovariance = -10000000;
		int maxCovarianceIndex = -1;
		for (int i = 0; i < covariances.length; i++) {
			if (maxCovariance < covariances[i]) {
				maxCovarianceIndex = i;
				maxCovariance = covariances[i];
			}
		}
		return maxCovarianceIndex;
	}

	public double[] crossCovariance(float[] reference, float[] target) {
		double[] covariances = new double[reference.length];
		for (int i = 0; i < reference.length; i++) {
			covariances[i] = covariance(reference, target, i);
		}
		return covariances;
	}

	public double covariance(float[] reference, float[] target, int lag) {
		double covariance = 0.0;
		for (int i = 0; i < reference.length; i++) {
			int targetIndex = (i + lag) % reference.length;
			covariance += reference[i] * target[targetIndex];
		}
		return covariance;
	}
}
