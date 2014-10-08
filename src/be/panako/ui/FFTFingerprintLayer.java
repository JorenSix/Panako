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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import be.panako.strategy.fft.FFTFingerprint;
import be.panako.strategy.fft.FFTFingerprintExtractorNaive;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.util.PitchConverter;


public class FFTFingerprintLayer implements Runnable, Layer, AudioProcessor {
	private final List<FFTFingerprint> fingerprints;
	private final int fingerprintsPerSecond;
	private final File file;
	private TreeMap<Double, float[]> spectrum;
	private float binWidth; //in ms
	private float[] binStartingPointsInCents;
	private final  CoordinateSystem cs;
	private final LayerFinishedHandler finishedHandler;
	
	float max = 0;
	float min = 100000;
	
	int diameterInPixels = 8;
	
	public FFTFingerprintLayer(CoordinateSystem cs, File file,int landmarksPerSecond, LayerFinishedHandler layerFinishedHandler) {
		this.cs=cs;
		fingerprints = new ArrayList<FFTFingerprint>();
		this.fingerprintsPerSecond = landmarksPerSecond;
		this.file = file;
		this.binWidth = Config.getFloat(Key.FFT_STEP_SIZE)/ Config.getFloat(Key.FFT_SAMPLE_RATE) * 1000;
		this.finishedHandler = layerFinishedHandler;
		
		int fftSize = Config.getInt(Key.FFT_SIZE);
		binStartingPointsInCents = new float[fftSize];
		for(int i = 1 ; i < fftSize ; i++){
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(i *  Config.getFloat(Key.FFT_SAMPLE_RATE) / fftSize);
		}
		
		new Thread(this, "LandmarkLayer Initialization").start();
		
	}

	@Override
	public void draw(Graphics2D graphics) {
		if(spectrum !=null && !fingerprints.isEmpty()){
			drawSpectrum(graphics);
			drawLandmarks(graphics);
		}
	}

	private void drawLandmarks(Graphics2D graphics) {
		for(FFTFingerprint fingerprint :  fingerprints){
			Color color = Color.GREEN;
			graphics.setColor(color);
			drawLandmark(graphics,fingerprint.t1,fingerprint.f1);
			drawLandmark(graphics,fingerprint.t2,fingerprint.f2);	
			drawLine(graphics,fingerprint.t1,fingerprint.f1,fingerprint.t2,fingerprint.f2);
		}
	}

	private void drawLine(Graphics2D graphics, int t1, int f1, int t2, int f2) {
		float time1 = timeIndexToTime(t1)+binWidth/2;
		float freq1 = frequencyIndexToCents(f1)+getBinHeight(f1)/2;
		float time2 = timeIndexToTime(t2)+binWidth/2;
		float freq2 = frequencyIndexToCents(f2)+getBinHeight(f2)/2;
		graphics.drawLine(Math.round(time1), Math.round(freq1), Math.round(time2), Math.round(freq2));
	}
	
	private void drawLandmark(Graphics2D graphics, int t1, int f1) {
		float timeDiameter = LayerUtilities.pixelsToUnits(graphics, diameterInPixels, true);
		float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, diameterInPixels, false);
		float time = (timeIndexToTime(t1) + binWidth/2) - timeDiameter/2.0f;
		float frequencyInCents = (frequencyIndexToCents(f1)+getBinHeight(f1)/2) - frequencyDiameter/2.0f;		
		graphics.drawOval(Math.round(time) , Math.round(frequencyInCents), Math.round(timeDiameter), Math.round(frequencyDiameter));
	}
	
	private float timeIndexToTime(int timeIndex){
		return timeIndex * binWidth;
	}
	private float getBinHeight(int frequencyIndex){
		return binStartingPointsInCents[frequencyIndex+1] - binStartingPointsInCents[frequencyIndex];
	}
	private float frequencyIndexToCents(int frequencyIndex){
		return binStartingPointsInCents[frequencyIndex];
	}

	private void drawSpectrum(Graphics2D graphics) {
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
							(int) Math.round(binWidth),
							(int) Math.ceil(getBinHeight(i)));
				}
			}
		}
		
	}

	@Override
	public String getName() {
		return "FFT Landmark Layer";
	}
	

	@Override
	public boolean process(AudioEvent audioEvent) {
		FFTFingerprintExtractorNaive explorer = new FFTFingerprintExtractorNaive(Config.getFloat(Key.FFT_SAMPLE_RATE), fingerprintsPerSecond);
		explorer.keepSpectralFeatures();
		Set<FFTFingerprint> foundLandmarks = explorer.findFingerprints(audioEvent.getFloatBuffer(),0);
		float[][] spectralFeatures = explorer.getSpectralFeatures();
		TreeMap<Double,float[]> spectralInfo = new TreeMap<Double,float[]>();
		for(int i = 0 ; i < spectralFeatures.length ; i++){
			double time = timeIndexToTime(i);
			spectralInfo.put(time, spectralFeatures[i]);
			for (int j = 0; j < spectralFeatures[i].length; j++) {
				max = Math.max(spectralFeatures[i][j], max);
				min = Math.min(spectralFeatures[i][j], min);
			}
		}
		fingerprints.addAll(foundLandmarks);
		spectrum = spectralInfo;
		return true;
	}

	@Override
	public void processingFinished() {
	}

	@Override
	public void run() {
		
		try{
			AudioDispatcher d;
			int sampleRate = Config.getInt(Key.FFT_SAMPLE_RATE);
			d = AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), sampleRate, 8000 * 100, 0);
			d.addAudioProcessor(this);
			d.run();
			if(finishedHandler !=null){
				finishedHandler.LayerFinished(null);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
