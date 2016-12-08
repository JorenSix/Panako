package be.panako.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;


public class TestUtilities {
	
	public static File getResource(String fileName){
		String file = "/be/panako/tests/res/" + fileName;
		final URL url = TestUtilities.class.getResource(file);
		try {
			return new File(new URI(url.toString()));
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	
	public static File[] getResources(String folderName, final String ext){
		String file = "/be/panako/tests/res/" + folderName;
		final URL url = TestUtilities.class.getResource(file);
		try {
			 File folder = new File(new URI(url.toString()));
			 return folder.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(ext.toLowerCase());
				}
			});
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	private static String refFolder   = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/reference";
	private static String queryFolder = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/queries";
	
	public static File[] getRandomRefQueryPair(){
		FilenameFilter mp3Filter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".mp3");
			}
		};
		File[] referenceFiles = new File(refFolder).listFiles(mp3Filter);
		File[] queryFiles = new File(queryFolder).listFiles(mp3Filter);
		
		File referenceFile = referenceFiles[new Random().nextInt(referenceFiles.length)];
		File queryFile = null;
		String refName = referenceFile.getName();
		for(File q : queryFiles ){
			if(q.getName().startsWith(refName.substring(0, 5))){
				queryFile = q;
			}
		}
		File[] pair = {referenceFile,queryFile};
		return pair;
	}
	
	public static File getQueryFile(){
		return getResource("11266_69s-89s.mp3");
	}
	
	public static File getReferenceFile(){
		return getResource("11266.mp3");
	}
	
	
	public static float[] getAudioBuffer(File file,double start,double stop){
	
		double sampleRate = 44100;
		int sampleStart = (int) Math.round(sampleRate * start);
		int sampleStop = (int) Math.round(sampleRate * stop);
		int diff = sampleStop - sampleStart;
		final float[] audioBuffer = new float[diff];
		
		AudioDispatcher d;
		
		d = AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), 44100,diff, 0);
		d.skip(start);
		d.addAudioProcessor(new AudioProcessor() {
			boolean filled = false;
			@Override
			public void processingFinished() {
			}

			@Override
			public boolean process(AudioEvent audioEvent) {
				if(!filled){
					for (int i = 0; i < audioEvent.getFloatBuffer().length; i++) {
						audioBuffer[i] = audioEvent.getFloatBuffer()[i];
					}
					filled = true;
				}
				return false;
			}
		});
		d.run();
		
		
		
		return audioBuffer;
	}
	
	
	
	

}
