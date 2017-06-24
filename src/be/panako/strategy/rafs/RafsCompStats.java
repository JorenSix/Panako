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
import java.util.List;
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
		RafsStrategy strategy = new RafsStrategy();
		
		for(int i = 1 ; i < args.length; i++){
			others[i-1] = args[i];
			otherPrints.add(extractPackedPrints(new File(args[i]),false));
			float diff = strategy.align(refPrints, otherPrints.get(i-1));
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
		final int overlap = size - Config.getInt(Key.RAFS_FFT_STEP_SIZE);
		String file = f.getAbsolutePath();
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		d.setZeroPadFirstBuffer(true);
		RafsExtractor ex = new RafsExtractor(file, trackProbabilities);
		//String baseName = f.getName();
		d.addAudioProcessor(ex);
		d.run();
		return ex.fingerprints;
	}

}
