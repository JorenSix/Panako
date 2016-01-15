package be.panako.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.strategy.nfft.NFFTStrategy;

public class NFFTTest {

	@Test
	public void testFingerprintHashSign() {
		NFFTStrategy strategy = new NFFTStrategy();
		strategy.toString();
		//fail();
		
		NFFTFingerprint firstPrint = new NFFTFingerprint(2424,28,0.0f,2524,22,0.0f);
		NFFTFingerprint otherPrint = new NFFTFingerprint(887,28,0.0f,987,34,0.0f);
		assertEquals("Sign should be reversed", firstPrint.hash() * -1 , otherPrint.hash());
		
		firstPrint = new NFFTFingerprint(18732,42,0.0f,18799,28,0.0f);
		otherPrint = new NFFTFingerprint(809,42,0.0f,876,28,0.0f);		
		assertEquals("Sign should be the same", firstPrint.hash(), otherPrint.hash());
	}
	
	@Test
	public void testFingerprintExctraction(){
		NFFTStrategy strategy = new NFFTStrategy();
		
		for(int t = 0 ; t <  10; t++){
			File[] pair = TestUtilities.getRandomRefQueryPair();
			List<NFFTFingerprint> ref = strategy.extractFingerprintsFromQuery(pair[0].getAbsolutePath());
			List<NFFTFingerprint> query = strategy.extractFingerprintsFromQuery(pair[1].getAbsolutePath());
			
			//System.out.println("Extracted " + query.size() + " prints from query, " + ref.size() + " prints from ref" );
			
			int counter =0;
			for(int i = 0 ; i < query.size() ; i++){
				for(int j = 0 ; j < ref.size() ; j++){
					if(query.get(i).hash() == ref.get(j).hash()){
						counter++;
					}
				}
			}
			assertTrue("At least 10% of fingerprints from query should be present in ref: " + counter + " fingerprints in common, should be at least: " + query.size()/10, counter > query.size()/10);
		}
	}

}
