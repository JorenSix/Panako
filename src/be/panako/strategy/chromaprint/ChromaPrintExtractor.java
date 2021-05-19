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

package be.panako.strategy.chromaprint;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class ChromaPrintExtractor implements AudioProcessor {
	
	final int sampleRate = 11025;
	final int size = 4096;
	final int overlap = (4096*2)/3;
	
	final float[] binStartingPointsInCents;
	final float[] binHeightsInCents;
	
	public final TreeMap<Float,float[]> magnitudes;
	public final TreeMap<Float,float[]> chromaMagnitudes;
	public final TreeSet<Float> orderedMagnitudes;//for median
	float currentMedian;
	
	final FFT fft;
	
	String file;
	
	public ChromaPrintExtractor(String file,ChromaPrintExtractor ref){
		chromaMagnitudes = new TreeMap<>();
		magnitudes = new TreeMap<>();
		orderedMagnitudes = new TreeSet<>();
		this.file = file;
		fft = new FFT(size,new HammingWindow());
		
		binStartingPointsInCents = new float[size];
		binHeightsInCents = new float[size];
		for (int i = 1; i < size; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		
		
	}
	
	public void starExtraction(){
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(file, sampleRate, size, overlap);
		d.addAudioProcessor(this);
		d.run();
	}
	
	public long getHash(){
		long hash = 0;
		int avgs[][] = new int[5][12];
		int count[] = new int[5];
		Iterator<Entry<Float,float[]>> it = chromaMagnitudes.entrySet().iterator();
		float div = chromaMagnitudes.lastKey()/4.99f;
		
		for(int i = 0 ; i <  chromaMagnitudes.size() ; i++){
			Entry<Float,float[]> e = it.next();
			float[] values = e.getValue();
			float time = e.getKey();
			int timeIndex = (int) Math.floor(time/div);
			for(int j = 0 ; j < values.length ; j++){
				avgs[timeIndex][j] += values[j]; 
			}
			count[timeIndex]++;
		}
		
		float[][] averages = new float[5][12];
		for(int i = 0 ; i < avgs.length ; i++ ){
			for(int j = 0 ; j < avgs[i].length ; j++){
				averages[i][j] = avgs[i][j]/(float) count[i];
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < avgs.length ; i++ ){
			for(int j = 0 ; j < avgs[i].length ; j++){
				if(averages[i][j] > 0.5)
					sb.append("1");
				else
					sb.append("0");
			}
		}
		hash = Long.parseLong(sb.toString(), 2);
		return hash;
	}
	
	float minChroma = 10000;
	float maxChroma = - 110000;

	@Override
	public boolean process(AudioEvent audioEvent) {
 		float[] buffer = audioEvent.getFloatBuffer().clone();
		float[] magnitudes = new float[buffer.length/2];
		float[] chroma = new float[12];
		fft.forwardTransform(buffer);
		fft.modulus(buffer, magnitudes);
		
		//make chroma with C as starting point (MIDI key 0) 
		for(int i = 0 ; i < magnitudes.length ;i++){
			//only process from MIDI key 29 (43Hz) to 107 (3951Hz) 
			if(binStartingPointsInCents[i] > 2900 && binStartingPointsInCents[i] < 10700){
				magnitudes[i] = (float) Math.log1p(magnitudes[i]);
				int  chromaIndex = Math.round(binStartingPointsInCents[i]/100) % 12;
				chroma[chromaIndex] += magnitudes[i];
			}
		}
		
		//normalize on the euclidean norm
		float squares = 0;
		for(int i = 0 ; i < chroma.length ; i++){
			squares += chroma[i] * chroma[i];
		}
		squares = (float) Math.sqrt(squares);
		for(int i = 0 ; i < chroma.length ; i++){
			chroma[i] = chroma[i]/squares;
		}
		
		//keep a running median
		for(int i = 0 ; i < chroma.length ; i++){
			orderedMagnitudes.add(chroma[i]);
			if(orderedMagnitudes.size()==1){
				currentMedian = chroma[i];
			}else{
				SortedSet<Float> h = orderedMagnitudes.headSet(currentMedian,true);
	            SortedSet<Float> t = orderedMagnitudes.tailSet(currentMedian,false);
	            int x = 1 - orderedMagnitudes.size() % 2;
	            if (h.size() < t.size() + x)
	            	currentMedian = t.first();
	            else if (h.size() > t.size() + x)
	            	currentMedian = h.last();
			}
		}
		
		
		//use the running median to binarize chroma
		for(int i = 0 ; i < chroma.length ; i++){
			if(chroma[i] > currentMedian)
				chroma[i] = 1;
			else
				chroma[i] = 0;
		}
		
		
				
		float timeStamp = (float) audioEvent.getTimeStamp();
		this.magnitudes.put(timeStamp , magnitudes);
		this.chromaMagnitudes.put(timeStamp, chroma);
				
		return true;
	}
	
	public double getDuration(){
		return chromaMagnitudes.lastKey();
	}

	@Override
	public void processingFinished() {
		getHash();
	}

}
