package be.panako.strategy.rafs;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.FileUtils;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.mih.BitSetWithID;
import be.tarsos.mih.MultiIndexHasher;
import be.tarsos.mih.storage.MapDBStorage;

public class RafsStrategy extends Strategy {

	private final MultiIndexHasher mih = new MultiIndexHasher(128, 14, 4, new MapDBStorage(4, "rafs_storage.db"));
	
	public RafsStrategy(){
		
	}
	
	@Override
	public double store(String resource, String description) {
		List<BitSetWithID> prints =  extractPackedPrints(new File(resource), FileUtils.getIdentifier(resource));
		
		for(BitSetWithID print : prints){
			mih.add(print);
		}
		return 0;
	}

	@Override
	public void query(String query, int maxNumberOfResults, QueryResultHandler handler) {
		List<BitSetWithID> prints =  extractPackedPrints(new File(query), FileUtils.getIdentifier(query));
		
		TreeMap<Long, ArrayList<BitSetWithID>> mostPopularOffsets = new TreeMap<>();
		
		int maxMatches = 200;
		int numMatches = -1000;
		for(BitSetWithID print : prints){
			if(numMatches < maxMatches){
			Collection<BitSetWithID> response = mih.query(print);	
				if(!response.isEmpty()  ){
					BitSetWithID closest = response.iterator().next();
					long queryIdentifier = closest.getIdentifier() >> 32;
					long queryOffset = closest.getIdentifier() - (queryIdentifier<<32);
					
					long printIdentifier = print.getIdentifier() >> 32;
					long printOffset = print.getIdentifier() - (printIdentifier<<32);
					
					queryOffset = queryOffset - printOffset;
					if(!mostPopularOffsets.containsKey(queryOffset)){
						mostPopularOffsets.put(queryOffset, new ArrayList<BitSetWithID>());
					}
					mostPopularOffsets.get(queryOffset).add(closest);
					numMatches = Math.max(mostPopularOffsets.get(queryOffset).size(),numMatches);
					//System.out.println(queryOffset);
				}
			}
		}
		
		List<BitSetWithID> maxVal=null;
		int maxCount=-1;
		for(ArrayList<BitSetWithID> val :  mostPopularOffsets.values()){
			if(maxCount < val.size()){
				maxCount = val.size();
				maxVal = val;
			}
		}
		
		if(maxCount > 4){
			BitSetWithID closest = maxVal.iterator().next();
			long queryIdentifier = closest.getIdentifier() >> 32;
			long queryOffset = closest.getIdentifier() - (queryIdentifier<<32);
			handler.handleQueryResult(new QueryResult(queryOffset, 0, "desc", "" + queryIdentifier ,maxCount, queryOffset, 1.0, 1.0));
		}else{
			handler.handleEmptyResult(new QueryResult(0, 0, "","", 0, 0, 0,0));
		}
		
		
	}

	@Override
	public void monitor(String query, int maxNumberOfReqults, QueryResultHandler handler) {
		
	}

	@Override
	public boolean hasResource(String resource) {
		
		return false;
	}

	@Override
	public boolean isStorageAvailable() {
		return true;
	}

	@Override
	public void printStorageStatistics() {

	}

	@Override
	public String resolve(String filename) {
		return null;
	}
	
	private static List<BitSetWithID> extractPackedPrints(File f,int fileIndex){		
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
		List<BitSetWithID> prints = new ArrayList<>();
		
		for (Map.Entry<Float, BitSet> frameEntry : packer.packedFingerprints.entrySet()) {
			int offset = (int) (frameEntry.getKey() * 1000);
			prints.add(new BitSetWithID(fileIndex * (1L<<32)  + offset, frameEntry.getValue()));
		}
		return prints;		
	}

}
