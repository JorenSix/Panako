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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.panako.cli.Play;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.mih.BitSetWithID;
import be.tarsos.mih.MultiIndexHasher;
import be.tarsos.mih.storage.MapDBStorage;


public class RafsCliTest {

	public static void main(String[] args) {
		if(args.length == 2 ){
//			int maxHammingDistance = 12;
//			//int minPercentage = 10;
//			List<File> files = new Play().getFilesFromArguments(args);
//			List<BitSetWithID> reference = extractPackedPrints(files.get(0), 0);
//			List<BinVector> query = extractPackedPrints(files.get(1),1);
//			int numNeighboursFound = 0;
//			int comps = 0;
//			int actualComps=0;
//			for(BitSetWithID q : query){
//				int closest = 129;
//				BitSetWithID closestInRef = null;
//				for(BitSetWithID r : reference){	
//					int d = Hamming.d(q.getBitSet(), r.getBitSet());
//					if(d < closest){
//						closest = d;
//						closestInRef =r;
//					}
//				}
//				if(closest <= maxHammingDistance){				
//					int neighbourOffset = closestInRef.getOffset();
//					System.out.printf("%.3f;%.3f;%.3f\n",q.getOffset()/1000.0,neighbourOffset/1000.0,(neighbourOffset-q.getOffset())/1000.0);
//					numNeighboursFound++;
//				}
//				comps++;
//				actualComps++;
//				if(comps == 10000){
//					System.out.printf("%d comps of %d \n", actualComps , reference.size() * query.size());
//					comps=0;
//				}
//			}
//			
//			List<BitSetWithID> dataset = new ArrayList<>();
//			BinLSH binLSH = new BinLSH(dataset);
//			dataset.addAll(reference);
//			long seed = 66546988;
//			binLSH.buildIndex(2,20,27,seed);
//			int lshNeighboursFound = 0;
//			int lshFalsePositive = 0;
//			System.out.printf("%.3f percent  match, LSH: %.3f \n",numNeighboursFound / (float) query.size() * 100.0, lshNeighboursFound / (float) query.size() * 100.0);
//			
//			for(BinVector q : query){
//				List<BinVector> closeList = binLSH.query(q, 10);
//				for(BinVector neighbour : closeList){
//					int d = Hamming.d(q.getBitSet(), neighbour.getBitSet());
//					if(d <= maxHammingDistance){						
//						int neighbourOffset = neighbour.getOffset();
//						System.out.printf("%.3f;%.3f;%.3f\n",q.getOffset()/1000.0,neighbourOffset/1000.0,(neighbourOffset-q.getOffset())/1000.0);
//						lshNeighboursFound++;
//					}else{
//						lshFalsePositive++;
//					}
//				}
//			}
//			
//			System.out.printf("%.3f percent  match, LSH: %.3f, FP count %d / %d \n",numNeighboursFound / (float) query.size() * 100.0, lshNeighboursFound / (float) query.size() * 100.0,lshFalsePositive, query.size());
		}else{
			List<File> files = new Play().getFilesFromArguments(args);
			HashMap<Integer, String> fileNames = new HashMap<>();
			
			List<BitSetWithID> dataset = new ArrayList<>();
			
			MultiIndexHasher mih  = new MultiIndexHasher(128, 13, 4,new MapDBStorage(4,"name.db"));
			
			if(mih.size() < 10000){
				for(int i = 0 ; i < files.size() - 1 ; i++){
					List<BitSetWithID> prints = extractPackedPrints(files.get(i),i);
					for(BitSetWithID print : prints){
						mih.add(print);
					}
					fileNames.put(i,files.get(i).getAbsolutePath());
				}
			}
			
			for(int i = 0 ; i < files.size() - 1 ; i++){
				fileNames.put(i,files.get(i).getAbsolutePath());
			}
			
			System.out.println("Dataset size: " + dataset.size());
			
			
			long start = System.currentTimeMillis();
			
			long stop = System.currentTimeMillis();
			System.out.printf("build index in %d ms\n", stop - start);
			
			String queryFileName = files.get(files.size() -1).getName();
			List<BitSetWithID> query = extractPackedPrints(files.get(files.size() -1),-1);
			
			start = System.currentTimeMillis();
			for(BitSetWithID q : query){
				Collection<BitSetWithID> neighbours = mih.query(q, 3);
				
				long queryIdentifier = q.getIdentifier() >> 32;
				long queryOffset = q.getIdentifier() - (queryIdentifier<<32);
				
				
				if(neighbours.isEmpty()){
					System.out.printf("%s;%.3f;NO_MATCH\n",queryFileName,queryOffset/1000.0f);
				}else{
					for(BitSetWithID neighbour:neighbours){
						String neighbourName = fileNames.get(neighbour.getIdentifier());
						long neighbourIdentifier = neighbour.getIdentifier() >> 32;
						long neighbourOffset = neighbour.getIdentifier() - (neighbourIdentifier<<32);
						
						System.out.printf("%s;%.3f;%s;%.3f;%.3f\n",queryFileName,queryOffset/1000.0,neighbourName,neighbourOffset/1000.0,(neighbourOffset-queryOffset)/1000.0);
					}
				}
			}
			stop = System.currentTimeMillis();
			System.out.printf("Executed %d queries in %d ms on %d prints\n", query.size(), stop - start, mih.size());
		}
		
		//System.out.println("Dataset size: " + dataset.size());
	}
	
	private static List<BitSetWithID> extractPackedPrints(File f,int fileIndex){		
		final int sampleRate = Config.getInt(Key.RAFS_SAMPLE_RATE);//2250Hz Nyquist frequency
		final int size = Config.getInt(Key.RAFS_FFT_SIZE);
		final int overlap =  size - Config.getInt(Key.RAFS_FFT_STEP_SIZE); 
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		RafsExtractor ex = new RafsExtractor(file, true);
		RafsPacker packer = new RafsPacker(ex,true);
		//String baseName = f.getName();
		d.setZeroPadFirstBuffer(true);
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
