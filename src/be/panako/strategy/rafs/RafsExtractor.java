package be.panako.strategy.rafs;

import java.util.Arrays;
import java.util.BitSet;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;


/**
 * @author Joren Six
 * 
 * Implements the 
 * 
 * Further reading:
 * 
 * Plapous, C., Berrani, SA., Besset, B. et al. Multimed Tools Appl (2017). doi:10.1007/s11042-017-4505-4
 * 
 * A highly robust audio fingerprinting system J.A. Haitsma, A.A.C.M. Kalker ISMIR 2002 paper: http://www.ismir2002.ismir.net/proceedings/02-FP04-2.pdf
 * 
 * Better version in JNMR:
 * A highly robust audio fingerprinting system with an efficient search strategy Journal of New Music Research, Vol. 32(2003), No. 2, p. 211-222
 */
public class RafsExtractor implements AudioProcessor {
	
	final int sampleRate = 5500;//2250Hz Nyquist frequency
	final int size = 2048;
	final int overlap = 2048-64; //about an fft every 11.6ms (64/5500)
	
	final int centsStart = 6200; //about 300Hz
	final int centStop = 9700; //about 2000Hz
	final int numberOfBands = 33;//to arrive at 32bits integer
	
	final float[] binStartingPointsInCents;
	final float[] binHeightsInCents;
	
	final float[] currentFFTMagnitudes;
	
	float[] previousMagnitudes = new float[33];
	float[] currentMagnitudes = new float[33];
	float[] tempMagnitudes = new float[33];
	
	public final TreeMap<Float,BitSet> fingerprints;
	
	//represents a 32bit value in an easy to use interface, BitSet. 
	BitSet currentFingerprint = new BitSet(32);
	
	float[] audioBuffer;
	
	float currentMedian;
	
	final FFT fft;
	
	private String file;
	
	public RafsExtractor(String file,RafsExtractor ref){
		fingerprints = new TreeMap<>();
		
		this.file = file;
		fft = new FFT(size,new HammingWindow());
       
		
		currentFFTMagnitudes = new float[size/2];
		
		binStartingPointsInCents = new float[size];
		binHeightsInCents = new float[size];
		for (int i = 1; i < size; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
	}
	
	public void starExtraction(){
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		d.addAudioProcessor(this);
		d.run();
	}
	
	float minChroma = 10000;
	float maxChroma = - 110000;

	@Override
	public boolean process(AudioEvent audioEvent) {
 		
		audioBuffer = audioEvent.getFloatBuffer().clone();
		
		fft.forwardTransform(audioBuffer);
	    
	    fft.modulus(audioBuffer, currentFFTMagnitudes);
		
		//clear
		Arrays.fill(currentMagnitudes, 0);
		
		//make chroma with C as starting point (MIDI key 0) 
		for(int i = 0 ; i < currentFFTMagnitudes.length ;i++){
			//only process from about 300Hz to 2000Hz 
			if(binStartingPointsInCents[i] > centsStart && binStartingPointsInCents[i] < centStop){
				//currentFFTMagnitudes[i] = (float) Math.log1p(currentFFTMagnitudes[i]);
				int bandIndex = Math.round((binStartingPointsInCents[i]-centsStart)/(float) (centStop - centsStart)*32);
				currentMagnitudes[bandIndex] += currentFFTMagnitudes[i];
			}
		}
				
		float timeStamp = (float) audioEvent.getTimeStamp();
		//this.magnitudes.put(timeStamp , magnitudes);
		
		if(previousMagnitudes != null){
			//this makes sure that length is 32
			currentFingerprint.set(31,true);
			for(int i = 0 ; i < currentFingerprint.length(); i++){
				boolean value = currentMagnitudes[i] - currentMagnitudes[i+1] - (previousMagnitudes[i] - previousMagnitudes[i+1]) > 0;
				currentFingerprint.set(i, value);
			}
			fingerprints.put(timeStamp, (BitSet) currentFingerprint.clone());
		}
		
		//switch pointers
		tempMagnitudes = previousMagnitudes;
		previousMagnitudes = currentMagnitudes;
		currentMagnitudes = tempMagnitudes;
		
		return true;
	}

	@Override
	public void processingFinished() {
		 //fft.destroy();
	}
}
