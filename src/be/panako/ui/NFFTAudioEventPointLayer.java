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

package be.panako.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Map;

import be.panako.strategy.nfft.NFFTEventPoint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;

public class NFFTAudioEventPointLayer implements Layer, MouseMotionListener {
	
	final float[] binStartingPointsInCents;
	final float[] binHeightsInCents;
	private final CoordinateSystem cs;
	private final NFFTAudioFileInfo fileInfo;
	private boolean drawFFT;
	
	private Graphics2D graphics;
	
	private NFFTEventPoint selectedEventPoint=null;
	private NFFTEventPoint prevSelectedEventPoint;
	
	public NFFTAudioEventPointLayer(CoordinateSystem cs,NFFTAudioFileInfo fileInfo) {
		int size = Config.getInt(Key.NFFT_SIZE);
		FFT fft = new FFT(size);
		binStartingPointsInCents = new float[size];
		binHeightsInCents = new float[size];
		for (int i = 1; i < size; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,8000));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		this.cs = cs;
		this.fileInfo = fileInfo;
	}

	@Override
	public void draw(Graphics2D graphics) {
		this.graphics = graphics;
		float frameDurationInMS = Config.getInt(Key.NFFT_STEP_SIZE)/  ((float) Config.getInt(Key.NFFT_SAMPLE_RATE)) * 1000.f;
		float frameOffsetInMS = frameDurationInMS/2.0f;
		
		if(drawFFT){
			Map<Float, float[]> magnitudesSubMap = fileInfo.magnitudes.subMap(cs.getMin(Axis.X) / 1000.0f - (float) fileInfo.getTimeOffset(), cs.getMax(Axis.X) / 1000.0f - (float) fileInfo.getTimeOffset());
					
			for (Map.Entry<Float, float[]> frameEntry : magnitudesSubMap.entrySet()) {
				double timeStart = frameEntry.getKey() + fileInfo.getTimeOffset();// in seconds
				float[] magnitudes = frameEntry.getValue();
			
				// draw the pixels
				for (int i = 0; i < magnitudes.length; i++) {
					Color color = Color.black;
					
					//actual energy at frame.frequencyEstimates[i];
					
					float centsStartingPoint = binStartingPointsInCents[i];
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y)
							&& centsStartingPoint <= cs.getMax(Axis.Y)) {
					
						int greyValue = 255 - (int) (magnitudes[i] * 4 * 255);
						greyValue = Math.max(0, greyValue);
						greyValue = Math.min(255, greyValue);
						color = new Color(greyValue, greyValue, greyValue);
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(timeStart * 1000),
								Math.round(centsStartingPoint),
								(int) Math.round(frameDurationInMS),
								(int) Math.ceil(binHeightsInCents[i]));
					}
				}
			}
		}
		

		
		for(NFFTEventPoint point : fileInfo.eventpoints){
			int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000) ;
			if(point == selectedEventPoint){
				graphics.setColor(Color.RED);
			}else{
				graphics.setColor(Color.BLUE);
			}
			if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
				float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
				float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
				float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
				
				graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
			}
		}
		
		for(NFFTEventPoint point : fileInfo.matchingEventPoints){
			int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000 );
			if(selectedEventPoint == point){
				graphics.setColor(Color.RED);
			}else{
				graphics.setColor(Color.GREEN);
			}
			if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
				float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
				float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
				float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
				
				graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
			}
		}
	}
	
	

	@Override
	public String getName() {		
		return "NFFT Audio Info Layer";
	}

	public void drawFFT(boolean selected) {
		drawFFT = selected;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if(graphics !=null ){
			LinkedPanel panel = (LinkedPanel) e.getComponent();
			Graphics2D g = (Graphics2D) panel.getGraphics();
			g.setTransform(panel.getTransform());
			
			float timeOffset = LayerUtilities.pixelsToUnits(g, e.getX(), true);
			float time = cs.getMin(Axis.X) + timeOffset;
			float frequencyOffset = LayerUtilities.pixelsToUnits(g, panel.getHeight() - e.getY(), false);
			float frequency = cs.getMin(Axis.Y) + frequencyOffset;
			
			float timeDiameter = LayerUtilities.pixelsToUnits(g, 10, true) * 2f;
			float frequencyDiameter = LayerUtilities.pixelsToUnits(g, 10, false)* 2f;
			
			selectedEventPoint = null;
			for(NFFTEventPoint eventPoint : fileInfo.eventpoints){
				float time1 = timeIndexToTime(eventPoint.t) - timeDiameter/2.0f ;
				float frequencyInCents1 = (frequencyIndexToCents(eventPoint.f)) - frequencyDiameter/2.0f;	
				
				if( time >= time1 && time <= time1+timeDiameter && 
					frequency >= frequencyInCents1 && frequency <= frequencyInCents1 + frequencyDiameter
					){
					selectedEventPoint = eventPoint;
				}	
			}
			if(selectedEventPoint!=null && selectedEventPoint!=prevSelectedEventPoint ){
				prevSelectedEventPoint = selectedEventPoint;
				System.out.println(String.format("Selected (t,f,fe,c): (%d,%d,%02fHz,%02f)",selectedEventPoint.t,selectedEventPoint.f,selectedEventPoint.frequencyEstimate,selectedEventPoint.contrast));
				e.getComponent().getParent().invalidate();
				for(Component c : e.getComponent().getParent().getComponents()){
					if(c instanceof LinkedPanel){
						c.repaint();
					}
				}
			}
		}
	}

	private float frequencyIndexToCents(int f) {
		return binStartingPointsInCents[f] + binHeightsInCents[f]/2.0f;
	}

	private float timeIndexToTime(int t) {
		float frameDurationInMS = Config.getInt(Key.NFFT_STEP_SIZE)/  ((float) Config.getInt(Key.NFFT_SAMPLE_RATE)) * 1000.f;
		float frameOffsetInMS = frameDurationInMS/2.0f;
		return (float) (t * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
	}

}
