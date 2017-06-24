/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/

package be.panako.strategy.rafs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import be.panako.cli.Panako;
import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Hamming;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class RafsRepStrategy extends Strategy {

	private final NavigableMap<Integer, String> audioNameStore;
	private final HTreeMap<Integer,long[]> lut;
	private final HTreeMap<Integer,int[]> printsPerSong;
	
	private final static Logger LOG = Logger.getLogger(RafsRepStrategy.class.getName());
	
	private final float fftDuration;
	
	private final DB db;
	
	public RafsRepStrategy(){
		fftDuration = Config.getFloat(Key.RAFS_FFT_STEP_SIZE)/Config.getFloat(Key.RAFS_SAMPLE_RATE);
		
		String mapsDBFileName = Config.get(Key.RAFS_DATABASE);
		//int searchRadius = Config.getInt(Key.RAFS_HAMMINNG_SEARCH_RADIUS);
	
		 File dbFile = new File(mapsDBFileName + ".repl.db");
		 db = DBMaker.fileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.make();
			
		 final String audioStore = "audio_store";
		// The meta-data store.
		audioNameStore = db.treeMap(audioStore)
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.STRING)
				.counterEnable() // enable size counter
				.createOrOpen();
		
		lut = db.hashMap("lut")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.LONG_ARRAY)
				.createOrOpen();
		
		printsPerSong = db.hashMap("songprints")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INT_ARRAY)
				.counterEnable()
				.createOrOpen();
	}
	
	@Override
	public double store(String resource, String description) {
		int identifier = Integer.valueOf(resolve(resource));
		double audioDuration = -1;
		
		RafsExtractor ex =  extractExtractor(new File(resource), identifier,false);
		int[] songPrints = new int[ex.fingerprints.size()];
		int index = 0; 
		for(Entry<Float,BitSet> entry : ex.fingerprints.entrySet()){
			int intValue = bitSetToInt(entry.getValue());
			songPrints[index] = intValue;
			
			//store in lut
			int offset = (int) (entry.getKey() * 1000);
			long idAndOffset = packOffsetAndId(offset,identifier); 
			if(!lut.containsKey(intValue)){
				long[] lutData = {idAndOffset};
				lut.put(intValue, lutData);
			}else{
				long[] oldLutData = lut.get(intValue);
				long[] newLutData = Arrays.copyOf(oldLutData, oldLutData.length+1);
				newLutData[oldLutData.length]=idAndOffset;
				lut.put(intValue, newLutData);
			}
			index++;
			audioDuration = entry.getKey();
		}
		//store all prints for a song
		printsPerSong.put(identifier, songPrints);
		
		audioNameStore.put(identifier, description);
		
		return audioDuration;
	}
	private int bitSetToInt(BitSet bitset){
		int intValue = 0;
		BitSet bitsetValue = bitset;
		long[] data = bitsetValue.toLongArray();
		if(data.length==1){
			intValue = (int) data[0];
		}
		return intValue;
	}
	
	private long packOffsetAndId(int offset, int identifier){
		return identifier * (1L<<32)  + offset;
	}
	
	private long getOffset(long originalValue){
		return originalValue - (getIdentifier(originalValue)<<32);
	}
	private long getIdentifier(long originalValue){
		return  originalValue >> 32;	
	}
	
	public void countMatchingValues(String query){
		
		int queryIndex = FileUtils.getIdentifier(query);
		RafsExtractor ex = extractExtractor(new File(query),queryIndex ,true);
		int matchingPerfectKeys = 0;
		for(Entry<Float,BitSet> entry : ex.fingerprints.entrySet()){
			Integer key = bitSetToInt(entry.getValue());
			if(key==0){
				continue;
			}
			long[] matches = lut.get(key);
			if(matches !=null){
				matchingPerfectKeys++;
				//System.out.println("match " + key);
				continue;
			}
		}
		
		int uncertainBitsToFlip = 10 ;
		int matchingSwitchedKeys = 0;
		for(Entry<Float,BitSet> entry : ex.fingerprints.entrySet()){
			HashSet<Integer> keySet = new HashSet<>();
			int originalKey =  bitSetToInt(entry.getValue()); 
			if(originalKey == 0){
				continue;
			}
			keySet.add(originalKey);
			int[] probabilities = ex.fingerprintProbabilities.get(entry.getKey());
			modifyPrint(keySet,originalKey,probabilities,uncertainBitsToFlip);
		
			for(Integer key : keySet){
				long[] matches = lut.get(key);
				if(matches !=null){
					matchingSwitchedKeys++;
					continue;
				}
			}
		}
		
		System.out.printf("%s,%d,%d,%d\n",new File(query).getName(),matchingPerfectKeys,matchingSwitchedKeys,ex.fingerprints.size());
	}
	
	public static void main(String... args){
		System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int arg0) throws IOException {
                // keep empty
            }
        }));
		RafsRepStrategy strat = new RafsRepStrategy();
		
		HashSet<Integer> set = new HashSet<>();
		int[] probability = {3,1,2,0,4,5,6,7,8,9,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32};
		int originalKey = Integer.MAX_VALUE;
		set.add(originalKey);
		strat.modifyPrint(set,originalKey, probability, 8);
		//for(Integer i : set){
			//System.out.println(Integer.toBinaryString(i));
		//}
		System.out.println(set.size());
		
		strat.lut.clear();
		strat.audioNameStore.clear();
		strat.store(args[0], args[0]);
		
		for(String query : args){
			strat.countMatchingValues(query);
		}
	}

	@Override
	public void query(String query, int maxNumberOfResults,Set<Integer> avoid, QueryResultHandler handler) {
		int queryIndex = FileUtils.getIdentifier(query);
		
		System.err.println("Query");
		RafsExtractor ex = extractExtractor(new File(query),queryIndex ,true);
		//a list with query fingerprint and match (identifier and offset)
		int identifier = -1;
		int bestOffset = -1;
		int score = -1;
		int uncertainBitsToFlip=Config.getInt(Key.RAFS_BITS_TO_FLIP);
		int hammingDistanceThreshold = Config.getInt(Key.RAFS_HAMMING_DISTANCE_THRESHOLD);
		int timeOutInMilliseconds = Config.getInt(Key.RAFS_TIMEOUT);
		long start = System.currentTimeMillis();
		
		float startAt = fftDuration * 256 - fftDuration/2.0f;
		
		outerloop:
		for(Entry<Float,BitSet> entry : ex.fingerprints.subMap(startAt, 10000000f).entrySet()){
			
			HashSet<Integer> keySet = new HashSet<>();
			int originalKey =  bitSetToInt(entry.getValue()); 
			keySet.add(originalKey);
			int[] probabilities = ex.fingerprintProbabilities.get(entry.getKey());
			modifyPrint(keySet,originalKey,probabilities,uncertainBitsToFlip);

			for(Integer key : keySet){
				long[] matches = lut.get(key);
				if(matches !=null){
					for(int i = 0 ; i < matches.length ; i++){
						long matchID = getIdentifier(matches[i]);
						int[] prints = printsPerSong.get((int) matchID);
						long matchOffset = getOffset(matches[i]);
						float fromKey = matchOffset/1000.0f - 0.5f;
						float toKey = matchOffset/1000.0f + 0.5f;
						int fromIndex = (int) (fromKey/fftDuration);
						int toIndex = (int) (toKey/fftDuration);
						int index = -1;
						for(int j = fromIndex ; j > 0 && j < toIndex  && j < prints.length; j++){
							if(prints[j]==key)
								index = j;
						}
						toKey = entry.getKey() + fftDuration/2.0f;
						fromKey = entry.getKey() - 256*fftDuration - fftDuration/2.0f;
						
						if(fromKey>0 && index-256>0){	
							SortedMap<Float,BitSet> queryPrints = ex.fingerprints.subMap(fromKey,toKey);
							int frameCounter = 0;
							int hammingDistance = 0;
							for(int j = index-256; j < index;j++){
								float printKey = fromKey +  frameCounter * fftDuration;
								printKey = queryPrints.subMap(printKey,printKey+fftDuration).firstKey();
								hammingDistance += Hamming.d(prints[j-1], bitSetToInt(queryPrints.get(printKey)));
								if(hammingDistance>hammingDistanceThreshold){
									break;
								}
								frameCounter++;
							}
							if(hammingDistance < hammingDistanceThreshold){
								identifier = (int) matchID;
								bestOffset = Math.round(index * fftDuration * 1000 - entry.getKey() * 1000);
								score = hammingDistance;
								break outerloop;
							}
						}
					}
				}
			}
			
			//time out detected?
			if(System.currentTimeMillis() - start > timeOutInMilliseconds){
				break outerloop;
			}
		}
		System.err.println("Fingerprints length: " + ex.fingerprints.size());
		if(bestOffset!=-1){
			long actualOffset =  bestOffset;
			String desc = audioNameStore.get((int) identifier);
			handler.handleQueryResult(new QueryResult(0, 0, desc, "" + actualOffset ,score, actualOffset, 1.0, 1.0));
		}else{
			handler.handleEmptyResult(new QueryResult(0, 0, "","", 0, 0, 0,0));
		}
	}


	/**
	 * Modifies a print so that the bits that are least certain are flipped.
	 * @param acc An accumulator where modified prints are added. It is a set but a list should yield the same contents (no duplicates should be added).
	 * @param print The original print to modify
	 * @param probability The probabilities [0-31] for each bit, 0 means least certain. 
	 * @param uncertainBitsToFlip The number of bits to flip if e.g. 3 is given 2^3 items are returned.
	 */	
	private void modifyPrint(Set<Integer> acc,int print, int[] probability, int uncertainBitsToFlip) {
			
		for(int i = 0 ; i<probability.length ; i++){
			//only flip that one element
			if(probability[i] == uncertainBitsToFlip-1){
				int mask = 1;
				mask = mask << i;
				final int newKey;
				//bit i is set to one
				if(0 != (print & mask)){
					//make it zero
					int inverseMask = (Integer.MAX_VALUE ^ mask);
					newKey = (print & inverseMask);			
				}else{
					//bit i is zero, set it to one
					newKey = (print | mask);
				}
				//add the new key
				acc.add(newKey);
				
				if(uncertainBitsToFlip!=1){
					//recursively flip the other bits as well
					modifyPrint(acc,newKey,probability,uncertainBitsToFlip-1);
					modifyPrint(acc,print,probability,uncertainBitsToFlip-1);
				}
			}
		}
		
	}
	


	@Override
	public void monitor(String query, int maxNumberOfReqults, Set<Integer> avoid, QueryResultHandler handler) {
		int samplerate = Config.getInt(Key.RAFS_SAMPLE_RATE);
		int size = Config.getInt(Key.MONITOR_STEP_SIZE) * samplerate;
		int overlap = Config.getInt(Key.MONITOR_OVERLAP) * samplerate;
		AudioDispatcher d ;
		if (query.equals(Panako.DEFAULT_MICROPHONE)){
			try {
				d = AudioDispatcherFactory.fromDefaultMicrophone(samplerate,size, overlap);
			} catch (LineUnavailableException e) {
				LOG.warning("Could not connect to default microphone!" + e.getMessage());
				e.printStackTrace();
				d = null;
			}
		}else{
			d = AudioDispatcherFactory.fromPipe(query, samplerate, size, overlap);
		}
		d.setZeroPadFirstBuffer(true);
		d.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				double timeStamp = audioEvent.getTimeStamp() - Config.getInt(Key.MONITOR_OVERLAP);
				processMonitorQuery(audioEvent.getFloatBuffer().clone(), handler,timeStamp,avoid);
				return true;
			}
			
			@Override
			public void processingFinished() {
			}
		});
		d.run();
	}
	
	private void processMonitorQuery(float[] audioData,QueryResultHandler handler, double timeStamp,Set<Integer> avoid){
		int samplerate = Config.getInt(Key.RAFS_SAMPLE_RATE);
		int size = Config.getInt(Key.RAFS_FFT_SIZE);
		int overlap = size - Config.getInt(Key.RAFS_FFT_STEP_SIZE);
		
		AudioDispatcher d;
		try {
			d = AudioDispatcherFactory.fromFloatArray(audioData, samplerate, size, overlap);
			d.setZeroPadFirstBuffer(true);
			final RafsExtractor processor = new RafsExtractor(null,true);
			d.addAudioProcessor(processor);
			d.run();
			queryForMonitor(processor.fingerprints, processor.fingerprintProbabilities, 10 , avoid, handler);
		} catch (UnsupportedAudioFileException e) {
			LOG.severe("Unsupported audio");
		}
	}
	

	public void queryForMonitor(TreeMap<Float,BitSet> fingerprints, TreeMap<Float,int[]> fingerprintProbabilities, int maxNumberOfResults,Set<Integer> avoid, QueryResultHandler handler) {
		
	
		int identifier = -1;
		int bestOffset = -1;
		int score = -1;
		int uncertainBitsToFlip=Config.getInt(Key.RAFS_BITS_TO_FLIP);
		int hammingDistanceThreshold = Config.getInt(Key.RAFS_HAMMING_DISTANCE_THRESHOLD);
		int timeOutInMilliseconds = Config.getInt(Key.RAFS_TIMEOUT);
		long start = System.currentTimeMillis();
		
		float startAt = fftDuration * 256 - fftDuration/2.0f;
		
		outerloop:
		for(Entry<Float,BitSet> entry : fingerprints.subMap(startAt, 10000000f).entrySet()){
			
			HashSet<Integer> keySet = new HashSet<>();
			int originalKey =  bitSetToInt(entry.getValue()); 
			keySet.add(originalKey);
			int[] probabilities = fingerprintProbabilities.get(entry.getKey());
			modifyPrint(keySet,originalKey,probabilities,uncertainBitsToFlip);

			for(Integer key : keySet){
				long[] matches = lut.get(key);
				if(matches !=null){
					for(int i = 0 ; i < matches.length ; i++){
						long matchID = getIdentifier(matches[i]);
						if(avoid.contains((int)matchID)){
							continue;
						}
						int[] prints = printsPerSong.get((int) matchID);
						long matchOffset = getOffset(matches[i]);
						float fromKey = matchOffset/1000.0f - 0.5f;
						float toKey = matchOffset/1000.0f + 0.5f;
						int fromIndex = (int) (fromKey/fftDuration);
						int toIndex = (int) (toKey/fftDuration);
						int index = -1;
						for(int j = fromIndex ; j > 0 && j < toIndex  && j < prints.length; j++){
							if(prints[j]==key)
								index = j;
						}
						toKey = entry.getKey() + fftDuration/2.0f;
						fromKey = entry.getKey() - 256*fftDuration - fftDuration/2.0f;
						
						if(fromKey>0 && index-256>0){	
							SortedMap<Float,BitSet> queryPrints = fingerprints.subMap(fromKey,toKey);
							int frameCounter = 0;
							int hammingDistance = 0;
							for(int j = index-256; j < index;j++){
								float printKey = fromKey +  frameCounter * fftDuration;
								printKey = queryPrints.subMap(printKey,printKey+fftDuration).firstKey();
								hammingDistance += Hamming.d(prints[j-1], bitSetToInt(queryPrints.get(printKey)));
								if(hammingDistance>hammingDistanceThreshold){
									break;
								}
								frameCounter++;
							}
							if(hammingDistance < hammingDistanceThreshold){
								identifier = (int) matchID;
								bestOffset = Math.round(index * fftDuration * 1000 - entry.getKey() * 1000);
								score = hammingDistance;
								break outerloop;
							}
						}
					}
				}
			}
			
			//time out detected?
			if(System.currentTimeMillis() - start > timeOutInMilliseconds){
				break outerloop;
			}
		}
		System.err.println("Fingerprints length: " + fingerprints.size());
		if(bestOffset!=-1){
			long actualOffset =  bestOffset;
			String desc = audioNameStore.get((int) identifier);
			handler.handleQueryResult(new QueryResult(0, 0, desc, "" + actualOffset ,score, actualOffset, 1.0, 1.0));
		}else{
			handler.handleEmptyResult(new QueryResult(0, 0, "","", 0, 0, 0,0));
		}
	}
	

	@Override
	public boolean hasResource(String resource) {
		int resourceIdentifier = Integer.valueOf(resolve(resource));
		String desc = this.audioNameStore.get(resourceIdentifier);
		return new File(resource).getName().equals(desc);
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
		return String.valueOf(identifier);
	}
	
	
	private static RafsExtractor extractExtractor(File f, int fileIndex, boolean trackProbabilities){
		final int sampleRate = Config.getInt(Key.RAFS_SAMPLE_RATE);//2250Hz Nyquist frequency
		final int size = Config.getInt(Key.RAFS_FFT_SIZE);
		final int overlap = size -  Config.getInt(Key.RAFS_FFT_STEP_SIZE); //about an fft every 11.6ms (64/5500)
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		d.setZeroPadFirstBuffer(true);
		RafsExtractor ex = new RafsExtractor(file, trackProbabilities);
		//String baseName = f.getName();
		d.addAudioProcessor(ex);
		d.run();
		return ex;
	}

}
