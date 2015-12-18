package be.panako.tests;

import static org.junit.Assert.*;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import be.panako.strategy.nfft.NFFTStrategy;
import be.panako.strategy.nfft.storage.Storage;
import be.panako.strategy.nfft.storage.redisson.NFFTRedisStorage;

public class NFFTRedisStorageTests {
	
	@Test
	public void test() {
		Storage storage = NFFTRedisStorage.getInstance();
		Random r = new Random();
		for( int i = 0 ; i < 10 ; i++){
			int nrOfAudioObjectsPrev = storage.getNumberOfAudioObjects();
			int nrOfFingerprintsPrev = storage.getNumberOfFingerprints();
			
			storage.addAudio(r.nextInt(), "description");
			
			storage.addFingerprint(511 + r.nextInt(), 12,  r.nextInt());
			storage.addFingerprint(5111, 123, r.nextInt());
			
			int nrOfAudioObjectsCurr = storage.getNumberOfAudioObjects();
			int nrOfFingerprintsCurr = storage.getNumberOfFingerprints();
			
			assertEquals(nrOfAudioObjectsPrev+1, nrOfAudioObjectsCurr);
			assertEquals(nrOfFingerprintsPrev+2, nrOfFingerprintsCurr);	
		}
		
		
	}

}
