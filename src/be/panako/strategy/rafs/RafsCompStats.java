package be.panako.strategy.rafs;

import java.io.File;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import be.panako.util.Hamming;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;


public class RafsCompStats {

	public static void main(String[] args) {
		
		for(int i = 1 ; i < args.length; i++){
			System.out.printf("\"%s\" ",new File(args[i]).getName());
		}
		System.out.println();
		
		String reference = args[0];
		TreeMap<Float, BitSet> refPrints = extractPackedPrints(new File(reference));
		
		List<TreeMap<Float, BitSet> > otherPrints = new ArrayList<>();
		String[] others = new String[args.length-1];
		float[] diffs = new float[args.length-1];
		
		for(int i = 1 ; i < args.length; i++){
			others[i-1] = args[i];
			otherPrints.add(extractPackedPrints(new File(args[i])));
			float diff = ((RafsStrategy) RafsStrategy.getInstance()).align(refPrints, otherPrints.get(i-1));
			diffs[i-1] = diff;
			System.out.println(diff);
		}
		int frame = 0;
		
		for(Entry<Float,BitSet> entry : refPrints.entrySet()){
			System.out.print(frame);
			
			int i = 0;
			for(TreeMap<Float, BitSet> otherPrint : otherPrints){
				float diff = diffs[i];
				Entry<Float,BitSet> otherEntry = otherPrint.ceilingEntry(entry.getKey()+diff/1000.0f);
				if(otherEntry == null){
					System.out.print( " 32 ");
				}else{
					System.out.printf(" %d ",Hamming.d(entry.getValue(), otherEntry.getValue()));
				}
				i++;
			}
			frame ++;
			System.out.println();
		}
		
	}
	
	private static TreeMap<Float, BitSet> extractPackedPrints(File f){		
		final int sampleRate = 5500;//2250Hz Nyquist frequency
		final int size = 2048;
		final int overlap = 2048-64; //about an fft every 11.6ms (64/5500)
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		RafsExtractor ex = new RafsExtractor(file, null);
		//String baseName = f.getName();
		d.addAudioProcessor(ex);
		d.run();
		return ex.fingerprints;
	}

}
