package be.panako.strategy.nfft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
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
			otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*1/4));
			otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*2/4));
			otherPrints.add(extractFingerprints(other,Config.getInt(Key.NFFT_STEP_SIZE)*3/4));
		}
		
		match(referencePrints,otherPrints);
	}

	private void match(List<NFFTFingerprint> referencePrints,
			List<List<NFFTFingerprint>> otherPrints) {
		HashMap<Integer, NFFTFingerprint> referenceHash = fingerprintsToHash(referencePrints);
		
		for(List<NFFTFingerprint> otherPrint: otherPrints){
			printOffset(referenceHash,fingerprintsToHash(otherPrint));
		}
		
	}
	
	private void printOffset(HashMap<Integer, NFFTFingerprint> referenceHash,HashMap<Integer, NFFTFingerprint> otherHash){
		//key is the offset, value the numer of times this offet appears.
		HashMap<Integer,Integer> mostPopularOffsets = new HashMap<Integer,Integer>();
		int minimumAlignedMatchesThreshold = 0;
		int maxAlignedOffsets = 0;
		int bestAlignedOffset = 0 ;
		for(Map.Entry<Integer,NFFTFingerprint> entry : referenceHash.entrySet()){
			if(otherHash.containsKey(entry.getKey())){
				NFFTFingerprint referenceFingerprint = entry.getValue();
				NFFTFingerprint otherFingerprint =otherHash.get(entry.getKey());
				int offset = referenceFingerprint.t1 - otherFingerprint.t1;
				// add the offset to the tree, if it is not already in the tree.
				if(!mostPopularOffsets.containsKey(offset)){
					mostPopularOffsets.put(offset, 0);
				}
				//add one to the counter
				int numberOfAlignedOffsets = 1 + mostPopularOffsets.get(offset);
				mostPopularOffsets.put(offset,numberOfAlignedOffsets);	
				
				if(numberOfAlignedOffsets > maxAlignedOffsets){
					maxAlignedOffsets = numberOfAlignedOffsets;
					bestAlignedOffset = offset;
				}
			}
		}
		
		if(maxAlignedOffsets >= minimumAlignedMatchesThreshold){
			//match found
			float fftHopSizesS = Config.getInt(Key.NFFT_STEP_SIZE) / (float) Config.getInt(Key.NFFT_SAMPLE_RATE);
			//float deltaS = fftHopSizesS;
			double offsetInSeconds = bestAlignedOffset * fftHopSizesS ;
			System.out.println(maxAlignedOffsets + " " + offsetInSeconds);
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

}
