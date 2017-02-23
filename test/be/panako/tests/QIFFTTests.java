package be.panako.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import be.panako.strategy.qifft.QIFFTEventPointProcessor;
import be.panako.strategy.qifft.QIFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class QIFFTTests {
	
	@Test
	public void testFingerprintExctraction(){
		
		for(int t = 0 ; t <  10; t++){
			File[] pair = TestUtilities.getRandomRefQueryPair();
			List<QIFFTFingerprint> ref = extractFingerprintsFromQuery(pair[0].getAbsolutePath());
			List<QIFFTFingerprint> query = extractFingerprintsFromQuery(pair[1].getAbsolutePath());
			
			//System.out.println("Extracted " + query.size() + " prints from query, " + ref.size() + " prints from ref" );
			
			int counter =0;
			for(int i = 0 ; i < query.size() ; i++){
				for(int j = 0 ; j < ref.size() ; j++){
					if(query.get(i).hash() == ref.get(j).hash()){
						counter++;
					}
				}
			}
			
			System.out.println(String.format("Success for: %s, extracted %d from ref, %d from query,  with  %d (%.2f %%) matching prints" ,pair[0].getName(),ref.size(),query.size() , counter, 100 * counter/(float) query.size()));
			assertTrue("At least 10% of fingerprints from query should be present in ref: " + counter + " fingerprints in common, should be at least: " + query.size()/10, counter > query.size()/10);
		}
	}
	
	@Test
	public void testTrueNegativeFingerprintExctraction(){
		
		for(int t = 0 ; t <  10; t++){
			File[] pair = TestUtilities.getRandomRefQueryPair();
			File[] otherPair = pair;
			while(otherPair[0].getName().equals(pair[0].getName())){
			  otherPair = TestUtilities.getRandomRefQueryPair();
			}
			
			List<QIFFTFingerprint> ref = extractFingerprintsFromQuery(pair[0].getAbsolutePath());
			List<QIFFTFingerprint> query = extractFingerprintsFromQuery(otherPair[0].getAbsolutePath());
			
			int counter =0;
			for(int i = 0 ; i < query.size() ; i++){
				for(int j = 0 ; j < ref.size() ; j++){
					if(query.get(i).hash() == ref.get(j).hash()){
						counter++;
					}
				}
			}
			
			assertTrue("At least 10% of fingerprints from query should be present in ref: " + counter + " fingerprints in common, should be at least: " + query.size()/100, counter < query.size()/100);
			System.out.println(String.format("False positive hash matches: %.2f %%", 100 * counter / (float) Math.max(query.size(), ref.size())));
		}
	}
	
	public static List<QIFFTFingerprint> extractFingerprintsFromQuery(String query){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		final QIFFTEventPointProcessor minMaxProcessor = new QIFFTEventPointProcessor(size,overlap,samplerate,4);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		List<QIFFTFingerprint> fingerprints = new ArrayList<QIFFTFingerprint>(minMaxProcessor.getFingerprints());
		return fingerprints;
	}


}
