package be.panako.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import be.panako.strategy.nfft.NFFTStrategy;

public class NFFTTest {

	@Test
	public void test() {
		NFFTStrategy strategy = new NFFTStrategy();
		strategy.toString();
		fail();
	}

}
