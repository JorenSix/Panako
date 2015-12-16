package be.panako.ui;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import be.panako.strategy.nfft.NFFTEventPoint;
import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.StopWatch;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

class NFFTAudioFileInfo implements AudioProcessor{
	
	private static final Set<Integer> selectedFingerprintHashes = new HashSet<Integer>();
	
	public static void clearSelectedFingerprints(){
		selectedFingerprintHashes.clear();
	}
	
	public static void addFingerprintToSelection(NFFTFingerprint print){
		selectedFingerprintHashes.add(print.hash());
	}
	
	public static boolean isFingerprintSelected(NFFTFingerprint print){
		return selectedFingerprintHashes.contains(print.hash());
	}
	
	
	public final TreeMap<Float,float[]> magnitudes;
	public final List<NFFTEventPoint> eventpoints;
	public final List<NFFTFingerprint> fingerprints;
	
	private NFFTEventPointProcessor eventPointProcessor;
	private AudioDispatcher d;
	private float runningMaxMagnitude;
	
	//matching with reference
	public final List<NFFTEventPoint> matchingEventPoints;
	public final List<NFFTFingerprint> matchingPrints;
	//time offset in seconds with respect to the reference
	private double timeOffset;
	
	private final NFFTAudioFileInfo referenceAudioFileInfo;
	
	private final File audioFile;
	
	public NFFTAudioFileInfo(File audioFile, NFFTAudioFileInfo referenceAudioFileInfo){
		this.audioFile = audioFile;
		
		magnitudes = new TreeMap<Float,float[]>();
		eventpoints = new ArrayList<NFFTEventPoint>();
		fingerprints = new ArrayList<NFFTFingerprint>();
		
		matchingPrints = new ArrayList<NFFTFingerprint>();
		matchingEventPoints = new ArrayList<NFFTEventPoint>();
		
		this.referenceAudioFileInfo = referenceAudioFileInfo;
	}
	
	public void extractInfoFromAudio(final Component componentToRepaint){
		int samplerate = Config.getInt(Key.NFFT_SAMPLE_RATE);
		int size = Config.getInt(Key.NFFT_SIZE);
		int overlap = size - Config.getInt(Key.NFFT_STEP_SIZE);
		StopWatch w = new StopWatch();
		w.start();
		
		d = AudioDispatcherFactory.fromPipe(audioFile.getAbsolutePath(), samplerate, size, overlap);
		eventPointProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(eventPointProcessor);
		d.addAudioProcessor(this);
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						componentToRepaint.repaint();
					}
				});
				if(referenceAudioFileInfo!=null)
				referenceAudioFileInfo.setMatchingFingerprints(matchingPrints);
			}			
			@Override
			public boolean process(AudioEvent audioEvent) {
				return true;
			}
		});
		new Thread(d).start();
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] currentMagnitudes = eventPointProcessor.getMagnitudes().clone();
		
		//for visualization purposes:
		//store the new max value or, decay the running max
		float currentMaxValue = max(currentMagnitudes);
		if(currentMaxValue > runningMaxMagnitude){
			runningMaxMagnitude = currentMaxValue;
		}else{
			runningMaxMagnitude = 0.9999f * runningMaxMagnitude;
		}
		normalize(currentMagnitudes);
		
		magnitudes.put((float)audioEvent.getTimeStamp(),currentMagnitudes);
		
		return true;
	}
	
	@Override
	public void processingFinished() {
		//ArrayList<NFFTEventPoint> otherEventPoints =  new ArrayList<NFFTEventPoint>();
		//ArrayList<NFFTEventPoint> matchingEventPoints =  new ArrayList<NFFTEventPoint>();
		//ArrayList<NFFTFingerprint> matchingPrints =  new ArrayList<NFFTFingerprint>();
		
		eventpoints.addAll(eventPointProcessor.getEventPoints());
		fingerprints.addAll(eventPointProcessor.getFingerprints());
		HashMap<Integer, Integer> mostPopularTimeOffsetCounter = new HashMap<>();

		
		//if there is a reference, compare it!		
		if(referenceAudioFileInfo!=null){
			double duration = d.secondsProcessed();
			int numberOfEqualEventPoints = 0;
			for(NFFTEventPoint other : referenceAudioFileInfo.eventpoints){
				for(NFFTEventPoint these : eventpoints){
					if(other.t == these.t && other.f == these.f){
						matchingEventPoints.add(these);
						numberOfEqualEventPoints++;
					}
				}
			}
		
			
			for(NFFTFingerprint otherPrint : referenceAudioFileInfo.fingerprints){
				for(NFFTFingerprint thisPrint : this.fingerprints){
					if(thisPrint.hashCode()==otherPrint.hashCode()){
						matchingPrints.add(thisPrint);
						int timeDiff = otherPrint.t1-thisPrint.t1;
						if(!mostPopularTimeOffsetCounter.containsKey(timeDiff)){
							mostPopularTimeOffsetCounter.put(timeDiff, 0);
						}
						mostPopularTimeOffsetCounter.put(timeDiff, mostPopularTimeOffsetCounter.get(timeDiff)+1);
					}
				}
			}
			
			int maxAlignedFingerprints = -1;
			int bestOffset = -1;
			for(Entry<Integer,Integer> entry : mostPopularTimeOffsetCounter.entrySet()){
				if(entry.getValue()>maxAlignedFingerprints){
					maxAlignedFingerprints = entry.getValue();
					bestOffset = entry.getKey();
				}
			}
	
			timeOffset = bestOffset * Config.getInt(Key.NFFT_STEP_SIZE)/  ((float) Config.getInt(Key.NFFT_SAMPLE_RATE));
					
			
			float percentageEqualEventPoints = numberOfEqualEventPoints/ ((float) Math.max(eventpoints.size(), referenceAudioFileInfo.eventpoints.size()));
			System.out.println("Done. Found " + numberOfEqualEventPoints + " matching event points, or " + numberOfEqualEventPoints/duration + " per second or " + percentageEqualEventPoints + " % .");
		}	
	}
	
	public double getTimeOffset(){
		return timeOffset;
	}
	
	private float max(float[] magnitudes){
		float max = 0;
		for(int i = 0 ; i < magnitudes.length ;i++){
			if(magnitudes[i]!=0){
				max = Math.max(max, magnitudes[i]);
			}
		}
		return max;
	}
	
	/**
	 * Normalizes the magnitude values to a range of [0,1].
	 */
	private void normalize(float[] magnitudes){
		for(int i = 0 ; i < magnitudes.length ;i++){
			if(magnitudes[i]!=0){
				magnitudes[i] = magnitudes[i]/runningMaxMagnitude;
			}
		}
	}

	public void setMatchingFingerprints(List<NFFTFingerprint> matchingPrints2) {
		matchingPrints.clear();
		HashMap<Integer,NFFTFingerprint> prints = new HashMap<Integer, NFFTFingerprint>();
		for(NFFTFingerprint print : fingerprints){
			prints.put(print.hash(), print);
		}
		for(NFFTFingerprint print : matchingPrints2){
			if(prints.containsKey(print.hash())){
				matchingPrints.add(prints.get(print.hash()));
			}
		}
	}
	
}