package be.panako.strategy.rafs;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.panako.cli.Play;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.lsh.hamming.BinLSH;
import be.tarsos.lsh.hamming.BinVector;

public class RafsCliTest {

	public static void main(String[] args) {
		List<File> files = new Play().getFilesFromArguments(args);
		HashMap<Integer, String> fileNames = new HashMap<>();
		
		List<BinVector> dataset = new ArrayList<>();
		for(int i = 0 ; i < files.size() - 1 ; i++){
			dataset.addAll(extractPackedPrints(files.get(i),i));
			fileNames.put(i, files.get(i).getName());
		}
		
		System.out.println("Dataset size: " + dataset.size());
		BinLSH binLSH = new BinLSH(dataset);
		
		long start = System.currentTimeMillis();
		binLSH.buildIndex(3,8,30);
		long stop = System.currentTimeMillis();
		System.out.printf("build index in %d ms\n", stop - start);
		
		String queryFileName = files.get(files.size() -1).getName();
		List<BinVector> query = extractPackedPrints(files.get(files.size() -1),-1);
		
		start = System.currentTimeMillis();
		for(BinVector q : query){
			List<BinVector> neighbours = binLSH.query(q, 3);
			
			if(neighbours.isEmpty()){
				System.out.printf("%s;%.3f;NO_MATCH\n",queryFileName,q.getOffset()/1000.0);
			}else{
				for(BinVector neighbour:neighbours){
					String neighbourName = fileNames.get(neighbour.getIdentifier());
					int neighbourOffset = neighbour.getOffset();				
					System.out.printf("%s;%.3f;%s;%.3f;%.3f\n",queryFileName,q.getOffset()/1000.0,neighbourName,neighbourOffset/1000.0,(neighbourOffset-q.getOffset())/1000.0);
				}
			}
		}
		stop = System.currentTimeMillis();
		System.out.printf("Executed %d queries in %d ms on %d prints\n", query.size(), stop - start, dataset.size());
		
		//System.out.println("Dataset size: " + dataset.size());
	}
	
	private static List<BinVector> extractPackedPrints(File f,int fileIndex){		
		final int sampleRate = 5500;//2250Hz Nyquist frequency
		final int size = 2048;
		final int overlap = 2048-64; //about an fft every 11.6ms (64/5500)
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		RafsExtractor ex = new RafsExtractor(file, null);
		RafsPacker packer = new RafsPacker(ex);
		//String baseName = f.getName();
		
		d.addAudioProcessor(ex);
		d.addAudioProcessor(packer);
		d.run();
		List<BinVector> prints = new ArrayList<>();
		
		for (Map.Entry<Float, BitSet> frameEntry : packer.packedFingerprints.entrySet()) {
			int offset = (int) (frameEntry.getKey() * 1000);
			prints.add(new BinVector(fileIndex,offset, frameEntry.getValue()));
		}
		return prints;		
	}

}
