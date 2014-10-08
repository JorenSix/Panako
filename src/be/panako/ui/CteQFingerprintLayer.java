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


package be.panako.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import be.panako.strategy.cteq.CteQFingerprint;
import be.panako.strategy.cteq.CteQFingerprintProcessor;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.util.PitchConverter;


public class CteQFingerprintLayer implements Runnable,Layer, MouseMotionListener {
	
	private TreeMap<Double, float[]> spectrum;
	private Set<CteQFingerprint> fingerprints;
	
	private final List<CteQFingerprint> selectedFingerprints;
	private final List<CteQFingerprint> comparableFingerprints;
	
	private final LandmarkSelectionHandler selectionHandler;
	private final CoordinateSystem cs;
	private final File audioFile;
	
	/**
	 * The default minimum pitch, in absolute cents (+-83 Hz)
	 */
	 int minimumFrequencyInCents; //4000
	/**
	 * The default maximum pitch, in absolute cents (+-3520 Hz)
	 */
	 int maximumFrequencyInCents;//10500;
	/**
	 * The default number of bins per octave.
	 */
	 int binsPerOctave;//36;
	
	/**
	 * The default increment in samples.
	 */
	int increment;//1024+512/44.1kHz => each 34.8ms 
	 
	 
	int sampleRate ;
	private float[] binStartingPointsInCents;
	
	private final LayerFinishedHandler layerFinishedHandler;
	
	float max = 0;
	float min = 100000;
	
	int diameterInPixels = 8;
	
	boolean simplify = false;
	
	private int binWith;//ms
	private float binHeight;//cents

	public CteQFingerprintLayer(CoordinateSystem cs, File audioFile, int increment, int minFreqInCents,int maxFreqInCents, int binsPerOctave,LandmarkSelectionHandler handler, LayerFinishedHandler finishedHandler) {
		this.selectedFingerprints = new ArrayList<CteQFingerprint>();
		this.comparableFingerprints = new ArrayList<CteQFingerprint>();
		this.cs = cs;		
		this.audioFile = audioFile;		
		this.increment = increment;
		this.minimumFrequencyInCents = minFreqInCents;
		this.maximumFrequencyInCents = maxFreqInCents;
		this.binsPerOctave = binsPerOctave;
		this.layerFinishedHandler = finishedHandler;
		spectrum = null;
		this.selectionHandler = handler;
		new Thread(this, "LandmarkTrioLayer Initialization").start();	
	}
	
	

	@Override
	public void run() {
		
		int eventPointsPerSecond = Config.getInt(Key.CTEQ_EVENT_POINTS_PER_SECOND_FOR_STORAGE);
		int branchinFactor = Config.getInt(Key.CTEQ_EVENT_POINT_BRANCHING_FOR_STORAGE);
		CteQFingerprintProcessor constantQ = new CteQFingerprintProcessor(eventPointsPerSecond,branchinFactor,true,increment,binsPerOctave,minimumFrequencyInCents,maximumFrequencyInCents);
		int size = constantQ.getFFTlength();
		
		TreeMap<Double, float[]> spectralInfoMap = new TreeMap<Double, float[]>();
		
		float sampleRate = 44100;
		binWith = Math.round(increment	* 1000 / sampleRate);
		binHeight = 1200 / (float) binsPerOctave;
		
		int overlap = size - increment;
		AudioDispatcher adp;
		
			adp =  AudioDispatcherFactory.fromPipe(audioFile.getPath(), 44100, size, overlap);
			adp.addAudioProcessor(constantQ);
			adp.run();
			List<float[]> spectralInfo = constantQ.getSpectralInfo();
			fingerprints = constantQ.getFingerprints();
			int timeIndex = 0;
			for(float[] spectrumSlice : spectralInfo){
				double timeInSeconds = timeIndexToTime(timeIndex);
				spectralInfoMap.put(timeInSeconds, spectrumSlice);
				for (int i = 0; i < spectrumSlice.length; i++) {
					max = Math.max(spectrumSlice[i], max);
					min = Math.min(spectrumSlice[i], min);
				}
				min = Math.abs(min);
				timeIndex++;
			}		
			float[] startingPointsInHertz = constantQ.getProcessor().getFreqencies();
			binStartingPointsInCents = new float[startingPointsInHertz.length];
			for (int i = 0; i < binStartingPointsInCents.length; i++) {
				binStartingPointsInCents[i] = (float) PitchConverter
						.hertzToAbsoluteCent(startingPointsInHertz[i]);
			}
			spectrum = spectralInfoMap;
			
			if(layerFinishedHandler !=null){
				layerFinishedHandler.LayerFinished(this);
			}	
		
	}
	

	@Override
	public void draw(Graphics2D graphics) {
		if(spectrum != null && fingerprints != null){
			drawSpectrum(graphics);
			drawTrios(graphics);
		}
	}
	
	public void setComparableTrios(List<CteQFingerprint> list){
		comparableFingerprints.clear();
		comparableFingerprints.addAll(list);
	}
	
	private void drawSpectrum(Graphics2D graphics){
		if(spectrum != null){
			Map<Double, float[]> spectralInfoSubMap = spectrum.subMap(cs.getMin(Axis.X)/1.0, cs.getMax(Axis.X)/1.0);
			for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
				double timeStart = column.getKey();// in seconds
				float[] spectralEnergy = column.getValue();// in cents
	
				// draw the pixels
				for (int i = 0; i < spectralEnergy.length; i++) {
					Color color = Color.black;
					float centsStartingPoint = binStartingPointsInCents[i];
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
						int greyValue = (int) (spectralEnergy[i]/max * 255);
						greyValue = 255 - Math.max(0, greyValue);
						
						color = new Color(greyValue, greyValue, greyValue);
						
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(timeStart),
								Math.round(centsStartingPoint),
								(int) Math.round(binWith),
								(int) Math.ceil(binHeight));
					}
				}
			}
		}
	}
	
	private void drawTrios(Graphics2D graphics){
		HashSet<String> landmarkSet = new HashSet<String>();
		for(CteQFingerprint fingerprint :  fingerprints){
			String k1 = fingerprint.t1 +"_"+ fingerprint.f1;
			String k2 = fingerprint.t2 +"_"+ fingerprint.f2;
			String k3 = fingerprint.t3 +"_"+ fingerprint.f3;
			if(simplify && (landmarkSet.contains(k1) || landmarkSet.contains(k2) || landmarkSet.contains(k3)) ){
				continue;
			}else{
				//Random rand = new Random();
				//float r = 1.0f - (float) (rand.nextFloat()/4.0 + 0.5);
				//float g = 1.0f - (float) (rand.nextFloat()/4.0 + 0.5);
				//float b = 1.0f - (float) (rand.nextFloat()/4.0 + 0.5);
				Color color = Color.BLUE;
				
				if(comparableFingerprints.contains(fingerprint)){
					color = Color.GREEN;
				}
				
				if(selectedFingerprints.contains(fingerprint)){
					color = Color.RED;
				}
				graphics.setColor(color);
				drawLandmark(graphics,fingerprint.t1,fingerprint.f1);
				drawLandmark(graphics,fingerprint.t2,fingerprint.f2);
				drawLandmark(graphics,fingerprint.t3,fingerprint.f3);	
				drawLine(graphics,fingerprint.t1,fingerprint.f1,fingerprint.t2,fingerprint.f2);
				drawLine(graphics,fingerprint.t2,fingerprint.f2,fingerprint.t3,fingerprint.f3);
				landmarkSet.add(k1);
				landmarkSet.add(k2);
				landmarkSet.add(k3);
			}
		}
	}
	
	private void drawLine(Graphics2D graphics, int t1, int f1, int t2, int f2) {
		float time1 = timeIndexToTime(t1)+binWith/2;
		float freq1 = frequencyIndexToCents(f1)+binStartingPointsInCents[0]+binHeight/2;
		float time2 = timeIndexToTime(t2)+binWith/2;
		float freq2 = frequencyIndexToCents(f2)+binStartingPointsInCents[0]+binHeight/2;
		graphics.drawLine(Math.round(time1), Math.round(freq1), Math.round(time2), Math.round(freq2));
	}

	private void drawLandmark(Graphics2D graphics, int t1, int f1) {
		float timeDiameter = LayerUtilities.pixelsToUnits(graphics, diameterInPixels, true);
		float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, diameterInPixels, false);
		float time = (timeIndexToTime(t1) + binWith/2) - timeDiameter/2.0f;
		float frequencyInCents = (frequencyIndexToCents(f1)+binStartingPointsInCents[0]+binHeight/2) - frequencyDiameter/2.0f;		
		graphics.drawOval(Math.round(time) , Math.round(frequencyInCents), Math.round(timeDiameter), Math.round(frequencyDiameter));
	}
	
	private float timeIndexToTime(int timeIndex){
		return timeIndex * binWith;
	}
	
	private float frequencyIndexToCents(int frequencyIndex){
		return frequencyIndex * binHeight;
	}
	

	@Override
	public String getName() {
		return "Landmark Fingerprints";
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		if(fingerprints == null)
			return;
		
		LinkedPanel panel = (LinkedPanel) e.getComponent();
		Graphics2D g = (Graphics2D) panel.getGraphics();
		g.setTransform(panel.getTransform());
		
		float timeOffset = LayerUtilities.pixelsToUnits(g, e.getX(), true);
		float time = cs.getMin(Axis.X) + timeOffset;
		float frequencyOffset = LayerUtilities.pixelsToUnits(g, panel.getHeight() - e.getY(), false);
		float frequency = cs.getMin(Axis.Y) + frequencyOffset;
		//System.out.println(String.format("Hovering over (%.0fms;%.0fcents)", time,frequency));
		selectedFingerprints.clear();
		float timeDiameter = LayerUtilities.pixelsToUnits(g, diameterInPixels, true) * 2f;
		float frequencyDiameter = LayerUtilities.pixelsToUnits(g, diameterInPixels, false)* 2f;
		
		HashSet<String> landmarkSet = new HashSet<String>();
		for(CteQFingerprint fingerprint :  fingerprints){
			String k1 = fingerprint.t1 +"_"+ fingerprint.f1;
			String k2 = fingerprint.t2 +"_"+ fingerprint.f2;
			String k3 = fingerprint.t3 +"_"+ fingerprint.f3;
			if(simplify && (landmarkSet.contains(k1) || landmarkSet.contains(k2) || landmarkSet.contains(k3)) ){
				continue;
			}else{
				float time1 = (timeIndexToTime(fingerprint.t1) + binWith/2) - timeDiameter/2.0f ;
				float frequencyInCents1 = (frequencyIndexToCents(fingerprint.f1)+binStartingPointsInCents[0]+binHeight/2) - frequencyDiameter/2.0f;	
				float time2 = (timeIndexToTime(fingerprint.t2) + binWith/2)- timeDiameter/2.0f;
				float frequencyInCents2 = (frequencyIndexToCents(fingerprint.f2)+binStartingPointsInCents[0]+binHeight/2) - frequencyDiameter/2.0f;	
				float time3 = (timeIndexToTime(fingerprint.t3) + binWith/2)- timeDiameter/2.0f;
				float frequencyInCents3 = (frequencyIndexToCents(fingerprint.f3)+binStartingPointsInCents[0]+binHeight/2 - frequencyDiameter/2.0f);	
				
				if(
					(
					time >= time1 && time <= time1+timeDiameter && 
					frequency >= frequencyInCents1 && frequency <= frequencyInCents1 + frequencyDiameter
					)||(
							time >= time2 && time <= time2+timeDiameter && 
							frequency >= frequencyInCents2 && frequency <= frequencyInCents2 + frequencyDiameter
					)||(
							time >= time3 && time <= time3+timeDiameter && 
							frequency >= frequencyInCents3 && frequency <= frequencyInCents3 + frequencyDiameter
					)
					){
					
					selectedFingerprints.add(fingerprint);
				}
				landmarkSet.add(k1);
				landmarkSet.add(k2);
				landmarkSet.add(k3);
			}
		}
		if(selectedFingerprints.size()>0){
			panel.repaint();
		}
		if(selectionHandler != null){
			selectionHandler.selected(selectedFingerprints);
		}
	}
	
	public interface LandmarkSelectionHandler{
		void selected(List<CteQFingerprint> trios);
	}

	public Set<CteQFingerprint> getLandmarkTrios() {
		return fingerprints;
	}
}
