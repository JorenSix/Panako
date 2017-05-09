package be.panako.strategy.rafs;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

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

	private final MultiIndexHasher mih;
	private final NavigableMap<Integer, String> audioNameStore;
	
	private final DB db;
	
	public RafsStrategy(){
		
		String mapsDBFileName = Config.get(Key.RAFS_DATABASE);
		int searchRadius = Config.getInt(Key.RAFS_HAMMINNG_SEARCH_RADIUS);
		int chunks = Config.getInt(Key.RAFS_MIH_CHUNKS);
		int numBits = Config.getInt(Key.RAFS_HAMMING_SPACE_NUM_BITS);
		
		mih = new MultiIndexHasher(numBits, searchRadius, chunks, new MapDBStorage(chunks,mapsDBFileName));
		
		 File dbFile = new File(Config.get(Key.RAFS_DATABASE) + "desc.db");
		 db = DBMaker.fileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.make();
			
		 final String audioStore = "audio_store";
		// The meta-data store.
		audioNameStore = db.treeMap(audioStore).keySerializer(Serializer.INTEGER).valueSerializer(Serializer.STRING)
		.counterEnable() // enable size counter
		.createOrOpen();
			
	}
	
	@Override
	public double store(String resource, String description) {
		int identifier = FileUtils.getIdentifier(resource);
		audioNameStore.put(identifier, description);
		
		List<BitSetWithID> prints =  extractPackedPrints(new File(resource), identifier,false);
		for(BitSetWithID print : prints){
			mih.add(print);
		}
		
		long finalTimeStamp = getOffset(prints.get(prints.size()-1).getIdentifier());
		double audioDuration = finalTimeStamp/1000.0;
		
		return audioDuration;
	}
	
	private long getOffset(long originalValue){
		return originalValue - (getIdentifier(originalValue)<<32);
	}
	private long getIdentifier(long originalValue){
		return  originalValue >> 32;	
	}

	@Override
	public void query(String query, int maxNumberOfResults, QueryResultHandler handler) {
		List<BitSetWithID> prints =  extractPackedPrints(new File(query), FileUtils.getIdentifier(query),true);
		
		TreeMap<Long, ArrayList<BitSetWithID>> mostPopularOffsets = new TreeMap<>();
	
		int numMatches = -1000;
		int agreementThreshold = 4;
		boolean agreementReached = false;
		long matchIdentifier = -1;
		long matchOffset = -1;
		
		//for each fingerprint extracted from the query
		for(BitSetWithID print : prints){
			//if the the number of agreeing offsets is over 200, then stop
			if(agreementReached){
				break;
			} else {
				//query for near neighbors
				Collection<BitSetWithID> response = mih.query(print);
				if(!response.isEmpty()  ){
					//only the first (nearest) neighbor is considered
					BitSetWithID closest = response.iterator().next();
					long queryOffset = getOffset(closest.getIdentifier());
					long printOffset = getOffset(print.getIdentifier());
					
					queryOffset = queryOffset - printOffset;
					if(!mostPopularOffsets.containsKey(queryOffset)){
						mostPopularOffsets.put(queryOffset, new ArrayList<BitSetWithID>());
					}
					
					//store the offset
					mostPopularOffsets.get(queryOffset).add(closest);
					
					//keep the max number of agreeing matches
					numMatches = Math.max(mostPopularOffsets.get(queryOffset).size(),numMatches);
					//if there are more than x offsets the same
					if(numMatches >= agreementThreshold){
						
						//check if there are more than x identifiers the same
						HashMap<Long, Integer> identifierCounts = new HashMap<>();
						int maxIdentifierCount = 0;
						List<BitSetWithID> list =mostPopularOffsets.get(queryOffset);
						for(BitSetWithID id : list ){
							long key = getIdentifier(id.getIdentifier());
							if(!identifierCounts.containsKey(key)){
								identifierCounts.put(key,0);
							}
							identifierCounts.put(key,identifierCounts.get(key)+1);
							if(maxIdentifierCount<identifierCounts.get(key)){
								maxIdentifierCount = identifierCounts.get(key);
								matchIdentifier = key;
							}
						}
						//if there are x offsets in agreement with the same identifier => match! 
						if(maxIdentifierCount>=agreementThreshold){
							matchOffset = queryOffset;
							agreementReached = true;
						}
					} 
				}
			}
		}
		
		if(agreementReached){
			int fftOffset = 87;//(484 samples /5500Hz * 1000) in ms
			long actualOffset = fftOffset + matchOffset;
			String desc = audioNameStore.get((int) matchIdentifier);
			handler.handleQueryResult(new QueryResult(0, 0, desc, "" + actualOffset ,4, actualOffset, 1.0, 1.0));
		}else{
			handler.handleEmptyResult(new QueryResult(0, 0, "","", 0, 0, 0,0));
		}
	}

	@Override
	public void monitor(String query, int maxNumberOfReqults, QueryResultHandler handler) {
		
	}

	@Override
	public boolean hasResource(String resource) {
		return new File(resource).getName().equals(resolve(resource));
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
		int identifier = FileUtils.getIdentifier(filename);
		return audioNameStore.get(identifier);
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
	
	
	private static List<BitSetWithID> extractPackedPrints(File f,int fileIndex,boolean trackProbabilities){		
		final int sampleRate = Config.getInt(Key.RAFS_SAMPLE_RATE);//2250Hz Nyquist frequency
		final int size = Config.getInt(Key.RAFS_FFT_SIZE);
		final int overlap =  Config.getInt(Key.RAFS_FFT_STEP_SIZE); //about an fft every 11.6ms (64/5500)
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		RafsExtractor ex = new RafsExtractor(file, trackProbabilities);
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
