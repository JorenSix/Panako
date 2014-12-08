package be.panako.strategy.nfft;

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
	private final String reference;
	private final String[] others;
	
	public NFFTStreamSync(String reference, String[] others){
		this.reference = reference;
		this.others = others;
	}
	
	private List<NFFTFingerprint> extractFingerprints(String resource, int millisecondsSkipped){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, samplerate, size, overlap);
		d.skip(millisecondsSkipped/1000.0f);
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,9,3);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		return new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
	}
	
	public void synchronize(){
		List<NFFTFingerprint> referencePrints = extractFingerprints(reference,0);
		List<List<NFFTFingerprint>> otherPrints = new ArrayList<List<NFFTFingerprint>>();
		for(String other : others){
			otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*0/4));
			
			//otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*1/4));
			//otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*2/4));
			//otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*3/4));
		}
		
		match(referencePrints,otherPrints);
	}

	private void match(List<NFFTFingerprint> referencePrints,
			List<List<NFFTFingerprint>> otherPrints) {
		HashMap<Integer, NFFTFingerprint> referenceHash = fingerprintsToHash(referencePrints);
		int otherIndex = 0;
		for(List<NFFTFingerprint> otherPrint: otherPrints){
			printOffset(referenceHash,fingerprintsToHash(otherPrint),otherIndex);
			otherIndex++;
		}
		
	}
	
	private void printOffset(HashMap<Integer, NFFTFingerprint> referenceHash,HashMap<Integer, NFFTFingerprint> otherHash, int otherIndex){
		//key is the offset, value the numer of times this offet appears.
		HashMap<Integer,Integer> mostPopularOffsets = new HashMap<Integer,Integer>();
		int minimumAlignedMatchesThreshold = 2;
		int maxAlignedOffsets = 0;
		int referenceFingerprintTimeIndex=-1;
		int otherFingerprintTimeIndex=-1;
		
		
		for(Map.Entry<Integer,NFFTFingerprint> entry : referenceHash.entrySet()){
			if(otherHash.containsKey(entry.getKey())){
				NFFTFingerprint referenceFingerprint = entry.getValue();
				NFFTFingerprint otherFingerprint = otherHash.get(entry.getKey());
				int offset = referenceFingerprint.t1 - otherFingerprint.t1;
				// add the offset to the tree, if it is not already in the tree.
				if(!mostPopularOffsets.containsKey(offset)){
					mostPopularOffsets.put(offset, 0);
				}
				//add one to the counter
				int numberOfAlignedOffsets = 1 + mostPopularOffsets.get(offset);
				mostPopularOffsets.put(offset,numberOfAlignedOffsets);	
				
				if(numberOfAlignedOffsets > maxAlignedOffsets){
					if(numberOfAlignedOffsets < 7){
					  referenceFingerprintTimeIndex = referenceFingerprint.t1;
					  otherFingerprintTimeIndex = otherFingerprint.t1;
					}
					maxAlignedOffsets = numberOfAlignedOffsets;
				}
			}
		}
		
		if(maxAlignedOffsets >= minimumAlignedMatchesThreshold){
			refineMatchWithCrossCovariance(maxAlignedOffsets,referenceFingerprintTimeIndex,otherFingerprintTimeIndex,otherIndex);
		}else{
			System.out.println("No alignment found");
		}
	}
	
	
	
	private HashMap<Integer, NFFTFingerprint> fingerprintsToHash(List<NFFTFingerprint> fingerprints){
		HashMap<Integer, NFFTFingerprint> hash = new HashMap<>();
		for(NFFTFingerprint fingerprint : fingerprints){
			hash.put(fingerprint.hash(),fingerprint);
		}
		return hash;
	}
	
	double referenceAudioStart = 0;
	double otherAudioStart = 0;
	
	private void refineMatchWithCrossCovariance(int maxAlignedOffsets,int referenceTime, int otherTime,int otherIndex){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		//int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		
		//match found
		float fftHopSizesS = Config.getInt(Key.NFFT_STEP_SIZE) / (float) samplerate;
		
		final float[] referenceAudioFrame = new float[size*4];
		final double referenceAudioToSkip = (referenceTime) * fftHopSizesS;
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(reference, samplerate, size*4, size*2);
		d.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			@Override
			public boolean process(AudioEvent audioEvent) {
				
				if(audioEvent.getTimeStamp() >= referenceAudioToSkip){
					referenceAudioStart =audioEvent.getTimeStamp();
					float [] buffer = audioEvent.getFloatBuffer();
					for(int i = 0 ; i < buffer.length; i++){
						referenceAudioFrame[i]=buffer[i];
					}
					return false;
				}else{
					return true;
				}
			}
		});
		d.run();
		
		final float[] otherAudioFrame = new float[size*4];
		final double otherAudioToSkip = (otherTime) * fftHopSizesS;
		
		d = AudioDispatcherFactory.fromPipe(others[otherIndex], samplerate, size*4, size *2);
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {	
			}			
			@Override
			public boolean process(AudioEvent audioEvent) {
				if(audioEvent.getTimeStamp() >= otherAudioToSkip){
					otherAudioStart =audioEvent.getTimeStamp();
					float [] buffer = audioEvent.getFloatBuffer();
					for(int i = 0 ; i < buffer.length; i++){
						otherAudioFrame[i]=buffer[i];
					}
					return false;
				}else{
					return true;
				}
			}
		});

		d.run();
		
		int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);
		
		//double offsetFramesInSeconds = (referenceTime - otherTime) * fftHopSizesS;
		double offsetStartEvent = referenceAudioStart - otherAudioStart;
		
		double offsetLagInSeconds = lag/(float) samplerate;
		
		double offsetTotalInSeconds = offsetStartEvent - offsetLagInSeconds;
		
		double offsetFromMatching = (referenceTime - otherTime) * fftHopSizesS;
		
		//lag is wrong if lag introduces a larger offset than algorithm:
		if(Math.abs(offsetFromMatching-offsetTotalInSeconds)>= fftHopSizesS){
			offsetTotalInSeconds = offsetFromMatching;
		}
		
		
		System.out.println(String.format("%d matches, offset %.3fs to go from %s to %s",maxAlignedOffsets,offsetTotalInSeconds,reference,others[otherIndex]));
		
	}
	
	public int bestCrossCovarianceLag(float[] reference, float[] target){
		double[] covariances = crossCovariance(reference, target);
		double maxCovariance = -10000000;
		int maxCovarianceIndex = -1;
		for(int i = 0 ; i < covariances.length;i++){
			if(maxCovariance < covariances[i]){
				maxCovarianceIndex = i;
				maxCovariance = covariances[i];
			}
		}
		return maxCovarianceIndex;
	}
	
	public double[] crossCovariance(float[] reference, float[] target){
		double[] covariances = new double[reference.length];
		for(int i = 0 ; i < reference.length;i++){
			covariances[i] = covariance(reference, target, i);
		}
		return covariances;
	}
	
	public double covariance(float[] reference, float[] target,int lag){
		double covariance = 0.0;
		for(int i = 0 ; i < reference.length;i++){
			int targetIndex = (i+lag)%reference.length;
			covariance += Math.abs(reference[i])*Math.abs(target[targetIndex]);
		}
		return covariance;
	}
}
