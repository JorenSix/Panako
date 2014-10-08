/***************************************************************************
*                                                                          *                     
* Panako - acoustic fingerprinting                                         *   
* Copyright (C) 2014 - Joren Six / IPEM                                    *   
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


package be.panako.strategy.quad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JFrame;

import be.panako.strategy.fft.FFTEventPoint;
import be.panako.strategy.fft.FFTFingerprint;
import be.panako.ui.FrequencyAxisLayer;
import be.panako.util.LemireMinMaxFilter;
import be.panako.util.StopWatch;
import be.panako.util.TimeUnit;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HannWindow;

public class MinMaxFFTEventPointProcessor implements AudioProcessor {

	private final FFT fft;
	private float[] magnitudes;
	
	private float[] visualizationMagnitudes;
	
	private final ArrayDeque<float[]> previousFrames;
	private final ArrayDeque<float[]> previousMinFrames;
	private final ArrayDeque<float[]> previousMaxFrames;
	
	private final List<FFTEventPoint> eventPoints = new ArrayList<>();

	int t = 0;
	
	private final Set<FFTFingerprint> fingerprints = new HashSet<>();
	
	private float runningMaxMagnitude;
	
	private final LemireMinMaxFilter maxFilterVertical;
	private final LemireMinMaxFilter minFilterVertical;
	

	private final int maxFilterWindowSize;
	private final int minFilterWindowSize;
	
	private final float[] maxHorizontal;
	private final float[] minHorizontal;
	
	
	public MinMaxFFTEventPointProcessor(int size){
		this(size,15,3);
	}
	
	public MinMaxFFTEventPointProcessor(int size, int maxFilterWindowSize,int minFilterWindowSize){
		fft = new FFT(size, new HannWindow());
		magnitudes = new float[size/2];
		
		previousFrames = new ArrayDeque<>();
		previousMaxFrames = new ArrayDeque<>();
		previousMinFrames = new ArrayDeque<>();
		
		maxFilterVertical = new LemireMinMaxFilter(maxFilterWindowSize, size/2,true);
		minFilterVertical = new LemireMinMaxFilter(minFilterWindowSize, size/2,true);
		
		maxHorizontal = new float[size/2];
		minHorizontal = new float[size/2];
		
		this.maxFilterWindowSize = maxFilterWindowSize;
		this.minFilterWindowSize = minFilterWindowSize;
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		//clone since the buffer is reused to slide
		float[] buffer = audioEvent.getFloatBuffer().clone();
		
		//calculate the fft
		fft.forwardTransform(buffer);
		
		//store the magnitudes (moduli) in magnitudes
		fft.modulus(buffer, magnitudes);
		
		//calculate the natural logarithm, and returns the maximum value of magnitudes
		float currentMaxValue = log();
		
		//run a maximum filter on the frame
		maxFilterVertical.filter(magnitudes);
		previousMaxFrames.addLast(maxFilterVertical.getMaxVal());
	
		//run a minimum filter on the frame
		minFilterVertical.filter(magnitudes);
		previousMinFrames.addLast(minFilterVertical.getMinVal());
		
		//store the frame magnitudes
		previousFrames.addLast(magnitudes.clone());
		
	
		
		//find the horziontal minima and maxima
		if(previousMaxFrames.size()==maxFilterWindowSize){
			
			horizontalFilter();
			previousMaxFrames.removeFirst();
		}
		
		//this makes sure that the first frame in previousMinFrames aligns with the center of 
		//previousmaxframes
		if(previousMinFrames.size() == maxFilterWindowSize/2 + minFilterWindowSize/2 + 1 ){
			previousMinFrames.removeFirst();
		}
		
		//this makes sure that the first frame in previousframes alignes with the center of 
		//previousmaxframes
		if(previousFrames.size() == maxFilterWindowSize/2 + minFilterWindowSize/2  ){
			previousFrames.removeFirst();
		}
		
		
		//for visualization purposes:
		//store the new max value or, decay the running max
		if(currentMaxValue > runningMaxMagnitude){
			runningMaxMagnitude = currentMaxValue;
		}else{
			runningMaxMagnitude = 0.9999f * runningMaxMagnitude;
		}	
		//Normalize according to the running maximum magnitude (for visualization)
		normalize();
		visualizationMagnitudes = magnitudes.clone();
		
		//frame counter
		t++;
		
		return true;
	}
	

	private void horizontalFilter() {
		Arrays.fill(maxHorizontal, 0);
		Arrays.fill(minHorizontal, 10000000);
		
		Iterator<float[]> prevMinFramesIterator = previousMinFrames.iterator();
		
		int i = 0;
		while(prevMinFramesIterator.hasNext() && i < minFilterWindowSize){
			float[] minFrame = prevMinFramesIterator.next();
			for(int j = 0 ; j < minFrame.length ; j++){
				minHorizontal[j] = Math.min(minHorizontal[j], minFrame[j]);
			}
			i++;
		}
		
		Iterator<float[]> prevMaxFramesIterator = previousMaxFrames.iterator();
		while(prevMaxFramesIterator.hasNext()){
			float[] maxFrame = prevMaxFramesIterator.next();
			for(int j = 0 ; j < maxFrame.length ; j++){
				maxHorizontal[j] = Math.max(maxHorizontal[j], maxFrame[j]);
			}
		}
		
		
		float[] frame = previousFrames.getFirst();
		
		float frameMaxVal=0;
		int timeInFrames = t-maxFilterWindowSize/2;
		
		//An event point is only valid if the ratio between min and max is larger than 20%
		//This eliminates points 
		float minRatioThreshold = 0.20f;
		//An event point is only valid if the ratio between min and max is smaller than 90%
		//This eliminates points in a region of equal energy (no contrast between min and max).
		float maxRatioThreshold = 0.90f;
		//An event point is only valid if it contains at least 10% 
		//of the maximum energy bin in the frame.
		//This eliminates low energy points.
		float minEnergyForPoint = 0.1f;
		
		for(i = 0 ; i<frame.length ; i++){
			float maxVal = maxHorizontal[i];
			float minVal = minHorizontal[i];
			float currentVal = frame[i];
			frameMaxVal = Math.max(frameMaxVal, maxVal);
			
			if(currentVal == maxVal && 
					currentVal !=0 && 
					minVal != 0 && 
					currentVal > minEnergyForPoint * frameMaxVal &&
					minVal/maxVal > minRatioThreshold  && 
					minVal/maxVal < maxRatioThreshold){
				eventPoints.add(new FFTEventPoint(timeInFrames, i, currentVal,minVal/maxVal) );
			}
		}
	}



	/**
	 * Normalizes the magnitude values to a range of [0,1].
	 */
	private void normalize(){
		for(int i = 0 ; i < magnitudes.length ;i++){
			if(magnitudes[i]!=0){
				magnitudes[i] = magnitudes[i]/runningMaxMagnitude;
			}
		}
	}
	
	
	/**
	 * calculate the natural log for the values in x
	 * @param magnitudes
	 */
	private float log(){
		float max = 0;
		for(int i = 0 ; i < magnitudes.length ;i++){
			if(magnitudes[i]!=0){
				magnitudes[i] = (float) Math.log1p(magnitudes[i]);
				max = Math.max(max, magnitudes[i]);
			}
		}
		return max;
	}
	
	

	@Override
	public void processingFinished() {	
		packEventPointsIntoFingerprints();
	}
	
	public Set<FFTFingerprint> getFingerprints(){
		return fingerprints;
	}
	
	private void packEventPointsIntoFingerprints(){
		Collections.sort(eventPoints,new Comparator<FFTEventPoint>() {

			@Override
			public int compare(FFTEventPoint o1, FFTEventPoint o2) {
				int val = Integer.valueOf(o1.t).compareTo(o2.t);
				if(val == 0){
					val = Integer.valueOf(o1.f).compareTo(o2.f);
				}
				return val;
			}
		});
		int maxEventPointDeltaTInSteps = 120; //about two seconds
		int maxEventPointDeltaFInBins = 19; // 256 is the complete spectrum
		
		int maxFingerprintsPerEventPoint = 2;
		
		int minTimeDifference = 8;//time steps about 200ms
		//Pack the event points into fingerprints
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			int f1 = eventPoints.get(i).f;
			int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			int maxfFirstLevel = f1 + maxEventPointDeltaFInBins;
			int minfFirstLevel = f1 - maxEventPointDeltaFInBins;
			
			//A list of fingerprints Per Event Point, ordered by energy of the combined event points
			TreeMap<Float,FFTFingerprint> fingerprintsPerEventPoint = new TreeMap<Float,FFTFingerprint>();
			
			for(int j = i + 1; j < eventPoints.size()  && eventPoints.get(j).t < maxtFirstLevel;j++){
				int t2 = eventPoints.get(j).t;
				int f2 = eventPoints.get(j).f;
				if(t1 != t2 && f1!=f2 && t2 > t1 + minTimeDifference && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					float energy = eventPoints.get(i).contrast + eventPoints.get(j).contrast;
					FFTFingerprint fingerprint = new FFTFingerprint(t1, f1, t2, f2);
					fingerprint.energy = energy;
					fingerprintsPerEventPoint.put(energy,fingerprint);					
				}
			}

			if(fingerprintsPerEventPoint.size() >= maxFingerprintsPerEventPoint ){
				for(int s = 0 ; s < maxFingerprintsPerEventPoint ; s++){
					Entry<Float, FFTFingerprint> e = fingerprintsPerEventPoint.lastEntry();
					fingerprints.add(e.getValue());
					fingerprintsPerEventPoint.remove(e.getKey());
				}
			}else{
				fingerprints.addAll(fingerprintsPerEventPoint.values());	
			}
		}
	}
	
	
	private static List<FFTFingerprint> otherFingerprints = new ArrayList<FFTFingerprint>();
	private static List<FFTEventPoint> eventPoints(){
		String source = "/home/joren/Desktop/02 My Ship_cut.ogg";
		source = "/home/joren/Desktop/small_jamendo_dataset/queries/43383_224s-244s.mp3";
		int samplerate = 8000;
		int size = 512;
		int overlap = 512-256;		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(source, samplerate, size, overlap);
		final MinMaxFFTEventPointProcessor quad = new MinMaxFFTEventPointProcessor(size);
		d.addAudioProcessor(quad);
		d.run();
		List<FFTEventPoint> e = new ArrayList<>();
		for(FFTEventPoint point : quad.eventPoints){
			e.add(new FFTEventPoint(point.t-1, point.f, 0, 0));
		}
		otherFingerprints.addAll(quad.fingerprints);
		return e ;
	}
	
	public static void main(String...strings){
		String source = "/media/data/datasets/Music/Keith Jarrett & Charlie Haden - Last Dance (2014) [mp3@320]/02 My Ship.mp3";
		source = "/home/joren/Desktop/small_jamendo_dataset/reference/43383.mp3";
		//source = "/home/joren/Desktop/small_jamendo_dataset/queries/1039187_207s-227s.mp3";
		int samplerate = 8000;
		int size = 512;
		int overlap = 512-256;
		final TreeMap<Float,float[]> magnitudes = new TreeMap<Float,float[]>();
		try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StopWatch w = new StopWatch();
		w.start();
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(source, samplerate, size, overlap);
		final MinMaxFFTEventPointProcessor quad = new MinMaxFFTEventPointProcessor(size);
		d.addAudioProcessor(quad);
		d.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
				
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				magnitudes.put((float)audioEvent.getTimeStamp(),quad.visualizationMagnitudes.clone());
				return true;
			}
		});
		d.run();
		
		double duration = magnitudes.lastEntry().getKey();
		
		System.out.println("Extracted  " + quad.eventPoints.size() + " ( " + quad.eventPoints.size()/duration + " points/s ) event points in " + w.formattedToString() + " or " + duration/w.timePassed(TimeUnit.SECONDS) + " times realtime");
		
		
		final List<FFTEventPoint> otherEventPoints = eventPoints();
		final List<FFTEventPoint> matchingEventPoints = new ArrayList<>();
		int numberOfEqualEventPoints = 0;
		for(FFTEventPoint other : otherEventPoints){
			for(FFTEventPoint these : quad.eventPoints){
				if(other.t == these.t && other.f == these.f){
					//System.out.println(these.t + " ; " + other.f);
					matchingEventPoints.add(other);
					numberOfEqualEventPoints++;
				}
			}
		}
		
		final List<FFTFingerprint> matchingPrints = new ArrayList<>();
		HashMap<Integer, Integer> counter = new HashMap<>();
		for(FFTFingerprint otherPrint : otherFingerprints){
			for(FFTFingerprint thisPrint : quad.fingerprints){
				if(thisPrint.hashCode()==otherPrint.hashCode()){
					matchingPrints.add(thisPrint);
					int timeDiff = thisPrint.t1-otherPrint.t1;
					if(!counter.containsKey(timeDiff)){
						counter.put(timeDiff, 0);
					}
					counter.put(timeDiff, counter.get(timeDiff)+1);
				}
			}
		}
		

		System.out.println("Found " + matchingPrints.size() + " matching fingerprints of " + quad.fingerprints.size() + " and query fingerprint size  of "+ otherFingerprints.size() +"."); 
		for(Entry<Integer,Integer> entry : counter.entrySet()){
			if(entry.getValue()>3){
			System.out.println(entry.getKey() + " " + entry.getValue());
			}
		}
		
		
		System.out.println("Done. Found " + numberOfEqualEventPoints + " matching event points, or " + numberOfEqualEventPoints/duration + " per second or " + numberOfEqualEventPoints/ ((float) Math.max(otherEventPoints.size(), quad.eventPoints.size())) + " % .");
	
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 600);
		
		final float[] binStartingPointsInCents = new float[size];
		final float[] binHeightsInCents = new float[size];
		FFT fft = new FFT(size);
		for (int i = 1; i < size; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,samplerate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		

		final CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, 3500, 11900);
		final LinkedPanel frequencyDomainPanel = new LinkedPanel(cs);
		frequencyDomainPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				frequencyDomainPanel.repaint();
				
			}
		});
		frequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		frequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		frequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		frequencyDomainPanel.addLayer(new Layer(){

			@Override
			public void draw(Graphics2D graphics) {
				Map<Float, float[]> magnitudesSubMap = magnitudes.subMap(
						cs.getMin(Axis.X) / 1000.0f, cs.getMax(Axis.X) / 1000.0f);
				for (Map.Entry<Float, float[]> frameEntry : magnitudesSubMap.entrySet()) {
					double timeStart = frameEntry.getKey();// in seconds
					float[] magnitudes = frameEntry.getValue();
				
					// draw the pixels
					for (int i = 0; i < magnitudes.length; i++) {
						Color color = Color.black;
						
						//actual energy at frame.frequencyEstimates[i];
						
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y)
								&& centsStartingPoint <= cs.getMax(Axis.Y)) {
						
							int greyValue = 255 - (int) (magnitudes[i]* 255);
							greyValue = Math.max(0, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000),
									Math.round(centsStartingPoint),
									(int) Math.round(8*4),
									(int) Math.ceil(binHeightsInCents[i]));
						}
					}
				}
				
				for(FFTEventPoint point : quad.eventPoints){
					int timeInMs = point.t * 8 * 4 + 4;
					graphics.setColor(Color.RED);
					if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
						float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
						float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
						float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
						
						graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
					}
				}
				
				for(FFTEventPoint point : otherEventPoints){
					int timeInMs = point.t * 8 * 4 + 4;
					graphics.setColor(Color.BLUE);
					if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
						float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
						float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
						float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
						
						graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
					}
				}
				
				for(FFTEventPoint point : matchingEventPoints){
					int timeInMs = point.t * 8 * 4 + 4;
					graphics.setColor(Color.GREEN);
					if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
						float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
						float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
						float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
						
						graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
					}
				}	
				
				for(FFTFingerprint print : quad.fingerprints){
					int timeInMsT1 = print.t1 * 8 * 4 + 4;
					int timeInMsT2 = print.t2 * 8 * 4 + 4;
					
					graphics.setColor(Color.ORANGE);
					if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
						float centsF1 = binStartingPointsInCents[print.f1] + binHeightsInCents[print.f1]/2.0f;
						
						float centsF2 = binStartingPointsInCents[print.f2] + binHeightsInCents[print.f2]/2.0f;
						
						graphics.drawLine(Math.round(timeInMsT1), Math.round(centsF1), Math.round(timeInMsT2), Math.round(centsF2));
					}
				}	
				
				for(FFTFingerprint print : matchingPrints){
					int timeInMsT1 = print.t1 * 8 * 4 + 4;
					int timeInMsT2 = print.t2 * 8 * 4 + 4;
					
					graphics.setColor(Color.GREEN);
					if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
						float centsF1 = binStartingPointsInCents[print.f1] + binHeightsInCents[print.f1]/2.0f;
						
						float centsF2 = binStartingPointsInCents[print.f2] + binHeightsInCents[print.f2]/2.0f;
						
						graphics.drawLine(Math.round(timeInMsT1), Math.round(centsF1), Math.round(timeInMsT2), Math.round(centsF2));
					}
				}	
			}

			@Override
			public String getName() {
				return "Quad Layer";
			}});
		
		frequencyDomainPanel.addLayer(new FrequencyAxisLayer(cs));
		frequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		frequencyDomainPanel.addLayer(new SelectionLayer(cs));
		frame.add(frequencyDomainPanel);
		frame.setVisible(true);
	}
}
