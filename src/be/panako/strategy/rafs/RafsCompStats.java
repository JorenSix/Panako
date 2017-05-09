package be.panako.strategy.rafs;

import java.io.File;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.panako.util.Config;
import be.panako.util.Hamming;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;


public class RafsCompStats {

	public static void main(String[] args) {
		
		for(int i = 1 ; i < args.length; i++){
			System.out.printf("\"%s\" ",new File(args[i]).getName());
		}
		System.out.println();
		
		String reference = args[0];
		TreeMap<Float, BitSet> refPrints = extractPackedPrints(new File(reference),false);
		
		List<TreeMap<Float, BitSet>> otherPrints = new ArrayList<>();
		String[] others = new String[args.length-1];
		float[] diffs = new float[args.length-1];
		
		TreeMap<String, List<Integer>> score = new TreeMap<>();
		
		for(int i = 1 ; i < args.length; i++){
			others[i-1] = args[i];
			otherPrints.add(extractPackedPrints(new File(args[i]),false));
			float diff = ((RafsStrategy) RafsStrategy.getInstance()).align(refPrints, otherPrints.get(i-1));
			diffs[i-1] = diff;
			System.out.println(diff);
			score.put(new File(args[i]).getName(), new ArrayList<>());
		}
		int frame = 0;
		
		for(Entry<Float,BitSet> entry : refPrints.entrySet()){
			System.out.print(frame);
			
			int i = 0;
			for(TreeMap<Float, BitSet> otherPrint : otherPrints){
				float diff = diffs[i];
				Entry<Float,BitSet> otherEntry = otherPrint.ceilingEntry(entry.getKey()+diff/1000.0f);
				int d = 32;
				if(otherEntry != null){
					d = Hamming.d(entry.getValue(), otherEntry.getValue());
				}
				score.get(new File(args[i+1]).getName()).add(d);
				System.out.printf(" %d", d);
				i++;
			}
			frame ++;
			System.out.println();
		}
		
		for(Entry<String,List<Integer>> entry : score.entrySet()){
			int sum = entry.getValue().stream().mapToInt(Integer::intValue).sum();
			double average = sum / (double) entry.getValue().size() / 32.0;
			System.out.printf("%s %.3f \n",entry.getKey(),average);			
		}
		
	}
	
	private static TreeMap<Float, BitSet> extractPackedPrints(File f,boolean trackProbabilities){		
		final int sampleRate = Config.getInt(Key.RAFS_SAMPLE_RATE);//2250Hz Nyquist frequency
		final int size = Config.getInt(Key.RAFS_FFT_SIZE);
		final int overlap =  Config.getInt(Key.RAFS_FFT_STEP_SIZE);
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		RafsExtractor ex = new RafsExtractor(file, trackProbabilities);
		//String baseName = f.getName();
		d.addAudioProcessor(ex);
		d.run();
		return ex.fingerprints;
	}

}
