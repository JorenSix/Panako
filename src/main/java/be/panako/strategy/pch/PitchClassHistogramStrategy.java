/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2022 - Joren Six / IPEM                             *
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


package be.panako.strategy.pch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.util.PitchConverter;

/**
 * Implementation of the strategy described in 
 * (2012) <a href="http://0110.be/files/attachments/415/2012.01.20.fingerprinter.submitted.pdf">A Robust Audio Fingerprinter Based on Pitch Class Histograms: Applications for Ethnic Music Archives</a>, 
 * by Joren Six and Olmo Cornelis in Proceedings of the International Workshop of Folk Music Analysis (FMA 2012)
 * 
 * 
 * @author joren
 *
 */
public class PitchClassHistogramStrategy extends Strategy {

	/**
	 * Create a new instance
	 */
	public PitchClassHistogramStrategy(){

	}

	@Override
	public double store(String resource, String description) {
		int identifier = FileUtils.getIdentifier(resource);
		int[] pch = new int[1200];
		double duration = extractPch(resource, pch);
		storePch(identifier,description,pch);
		return duration;
	}

	@Override
	public double delete(String resource) {
		throw new RuntimeException("Delete is currently not implemented for the PCH strategy");
	}

	private double extractPch(String resource, int[] pch){
		final List<Float> pitchTrack = new ArrayList<Float>();
		
		int sampleRate =  Config.getInt(Key.PCH_SAMPLE_RATE);
		int step = Config.getInt(Key.PCH_SIZE);
		int overlap = Config.getInt(Key.PCH_OVERLAP);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(resource, sampleRate,step,overlap);
		PitchDetectionHandler handler;
		handler = new PitchDetectionHandler() {
			@Override
			public void handlePitch(PitchDetectionResult pitchDetectionResult,
					AudioEvent audioEvent) {
				if(pitchDetectionResult.isPitched()){
					pitchTrack.add(pitchDetectionResult.getPitch());
				}
			}
		};
		
		d.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN,sampleRate, step, handler));
		d.run();
		
		
		for(float pitch  : pitchTrack){
			double centValue = PitchConverter.hertzToRelativeCent(pitch);
			int roundedCentValue = (int) Math.round(centValue);
			if(roundedCentValue >= 0 && roundedCentValue < 1200){
				pch[roundedCentValue] += 1;
			}
		}
		return -10;
		//return d.durationInSeconds();
	}
	
	private void storePch(int identifier, String description,int[] pch){
		StringBuilder sb = new StringBuilder();
		sb.append(description).append("\n");
		for(int i : pch){
			sb.append(i).append("\n");
		}
		String directory = Config.get(Key.PCH_FILES);
		directory = FileUtils.expandHomeDir(directory);
		String fileName = FileUtils.combine(directory,identifier + ".txt");
		FileUtils.writeFile(sb.toString(), fileName);
	}
	
	private String readPch(int identifier, int[] pch){
		String directory = Config.get(Key.PCH_FILES);
		String fileName = FileUtils.combine(directory,identifier + ".txt");
		
		String content = FileUtils.readFile(fileName);
		String[] data = content.split("\n");
		String description = data[0];
		for(int i = 1 ; i < data.length ; i++){
			pch[i-1] = Integer.valueOf(data[i]);
		}		
		return description;
	}
	
	private double[] match(int[] query, int[] reference){
		float maxMatch = 0;
		int bestShift = 0;
		int referenceArea = 0;
		int queryArea = 0;
		for(int i = 0 ; i < reference.length ; i++){
			referenceArea += reference[i];
			queryArea += query[i];
		}
		float maxArea = Math.max(referenceArea, queryArea);
		
		for(int shift = 0 ; shift < reference.length; shift++){	
			int overlap = 0;
			for(int i = 0 ; i < reference.length ; i++){
				overlap += Math.min(query[i], reference[(i+shift) % reference.length]);
			}
			float match = overlap / maxArea;
			if(match > maxMatch){
				maxMatch = match;
				bestShift = shift;
			}
		}
		double[] result = {maxMatch,bestShift};
		return result;
	}
	
	

	@Override
	public void query(String query, int maxNumberOfResults,Set<Integer> avoid,
			QueryResultHandler handler) {
		
		String directory = Config.get(Key.PCH_FILES);
		List<String> files = FileUtils.glob(directory, "\\d*.txt", false);
		
		int[] queryPch = new int[1200];
		extractPch(query, queryPch);
		
		TreeMap<Double, Integer> scores = new TreeMap<>();
		TreeMap<Double, Double> shifts = new TreeMap<>();
		TreeMap<Double, String> descriptions = new TreeMap<>();
		
		for(String file : files){
			int[] referencePch = new int[1200];
			int identifier = Integer.valueOf(FileUtils.basename(file));
			String description = readPch(identifier, referencePch);
			double[] result = match(queryPch,referencePch);
			double match = result[0];
			double shift = result[1];
			double ratio = 100 * PitchConverter.centToRatio(shift);
			
			scores.put(match, identifier);
			shifts.put(match, ratio);
			descriptions.put(match, description);
		}
		
		
		int handled = 0;
		for(Double key : scores.descendingKeySet()){
			if(handled < maxNumberOfResults){
				int identifier = scores.get(key);
				String description = descriptions.get(key);
				double frequencyRatio = shifts.get(key);
				QueryResult qr = new QueryResult(query,-1, -1, String.valueOf(identifier), description, (int) (key*100) , 0, -1, frequencyRatio,0,0);
				handler.handleQueryResult(qr);
				handled++;
			}
		}
	}

	@Override
	public void monitor(String query, int maxNumberOfReqults,Set<Integer> avoid,
			QueryResultHandler handler) {
		throw new Error("Operation not supported");
		
	}

	@Override
	public boolean hasResource(String resource) {
		int identifier = FileUtils.getIdentifier(resource);
		String directory = Config.get(Key.PCH_FILES);
		String fileName = FileUtils.combine(directory,identifier + ".txt");
		
		return FileUtils.exists(fileName);
	}

	@Override
	public boolean isStorageAvailable() {
		return true;
	}

	@Override
	public void printStorageStatistics() {
		int size = FileUtils.glob(".", "\\d*.txt", false).size();
		System.out.println("Stored " + size + " files");
	}
	
	@Override
	public String resolve(String filename) {
		return "" + FileUtils.getIdentifier(filename);
	}

	@Override
	public void print(String path, boolean sonicVisualizerOutput, boolean printOnlyEPs) {
		throw new RuntimeException("Print is currently not implemented for the PCH strategy");
	}

	@Override
	public String toJson(String path) {
		throw new RuntimeException("toJson is currently not implemented for the PCH strategy");
	}

	/*
	public String name() {
		return "PCH";
	}*/

	@Override
	public void clear() {
		
	}

	@Override
	public String metadata(String path) {
		return null;
	}

}
