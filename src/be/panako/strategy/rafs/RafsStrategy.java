package be.panako.strategy.rafs;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Hamming;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.mih.BitSetWithID;
import be.tarsos.mih.EclipseStorage;
import be.tarsos.mih.MultiIndexHasher;
import be.tarsos.mih.storage.MapDBStorage;

public class RafsStrategy extends Strategy {

	private final MultiIndexHasher mih = new MultiIndexHasher(128, 14, 4, new EclipseStorage(4));
	
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
	
	public long align(TreeMap<Float,BitSet> reference,TreeMap<Float,BitSet> other){
		long numberOfMilliseconds = -1;
		HashMap<Integer, Integer> mostPopularOffsets = new HashMap<>();
		int popularityCount = -1;
		float stepSizeInSeconds = (Config.getInt(Key.RAFS_FFT_SIZE) - Config.getInt(Key.RAFS_FFT_STEP_SIZE))/Config.getFloat(Key.RAFS_SAMPLE_RATE);
		int subFingerprintSequenceSize = 8;
		MultiIndexHasher mih = new MultiIndexHasher(32, 6, 2, new EclipseStorage(2));
		
		//create a multi index hash map 
		for (Map.Entry<Float, BitSet> refPrint : reference.entrySet()) {
			BitSet bitSet = refPrint.getValue();
			long id = (long) (refPrint.getKey() * 10000);
			BitSetWithID val = new BitSetWithID(id, bitSet);
			mih.add(val);
		}
		
		for (Map.Entry<Float, BitSet> otherPrint : other.entrySet()) {
			
			BitSetWithID q = new BitSetWithID(0,otherPrint.getValue());			
			Collection<BitSetWithID> nn = mih.query(q,10);
			
			int currentMinHamming = 33 * subFingerprintSequenceSize;
			int offset = -1;
			
			float otherSequenceEnd = otherPrint.getKey() + subFingerprintSequenceSize * stepSizeInSeconds + stepSizeInSeconds/2.0f;
			Collection<BitSet> otherSequence = other.subMap(otherPrint.getKey(), otherSequenceEnd).values();
			
			for(BitSetWithID neighbor : nn){
				float neigborTime = neighbor.getIdentifier()/10000.0f;
				float endofSequence = neigborTime + subFingerprintSequenceSize * stepSizeInSeconds + stepSizeInSeconds/2.0f;
				Collection<BitSet> refSequence = reference.subMap(neigborTime- stepSizeInSeconds/2.0f,endofSequence).values();
				if(otherSequence.size() == refSequence.size()){
					int d = 0; 
					Iterator<BitSet> refIterator = refSequence.iterator();
					Iterator<BitSet> otherIterator = otherSequence.iterator();
					while(refIterator.hasNext()){
						BitSet ref = refIterator.next();
						BitSet oth = otherIterator.next();
						if(ref.length() > 5 && oth.length() > 5){
							d += Hamming.d(ref, oth);
						}else{
							d += 33;
						}
					}
					if(d < currentMinHamming){
						currentMinHamming = d;
						offset = Math.round((otherPrint.getKey()- neigborTime)  * 1000);
					}
				}
			}
						
			//System.out.println(offset);
			
			if(offset != -1){
				if(mostPopularOffsets.containsKey(offset)){
					int newValue = mostPopularOffsets.get(offset)+1;
					if(newValue > popularityCount){
						popularityCount = newValue;
						numberOfMilliseconds = offset;
						
						//10% match or more than 200 in agreement, skip evaluating the rest
						if(popularityCount > 200 || popularityCount > 0.1 * Math.max(reference.size(),other.size())){
							break;
						}
					}
					mostPopularOffsets.put(offset,newValue);
				}else{
					//System.out.println(offset);
					mostPopularOffsets.put(offset,1);
				}
			}
		}
		return numberOfMilliseconds;
	}
	
	
	private static List<BitSetWithID> extractPackedPrints(File f,int fileIndex){		
		final int sampleRate = Config.getInt(Key.RAFS_SAMPLE_RATE);//2250Hz Nyquist frequency
		final int size = Config.getInt(Key.RAFS_FFT_SIZE);
		final int overlap =  Config.getInt(Key.RAFS_FFT_STEP_SIZE); //about an fft every 11.6ms (64/5500)
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
