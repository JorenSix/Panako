package be.panako.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;

import be.panako.strategy.nfft.NFFTEventPoint;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;

public class NFFTAudioInfoLayer implements Layer {
	
	final float[] binStartingPointsInCents;
	final float[] binHeightsInCents;
	private final CoordinateSystem cs;
	private final NFFTAudioFileInfo fileInfo;
	private boolean drawFFT;
	
	public NFFTAudioInfoLayer(CoordinateSystem cs,NFFTAudioFileInfo fileInfo) {
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
			graphics.setColor(Color.BLUE);
			if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
				float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
				float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
				float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
				
				graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
			}
		}
		
		for(NFFTEventPoint point : fileInfo.matchingEventPoints){
			int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000 );
			graphics.setColor(Color.GREEN);
			if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
				float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
				float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
				float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
				
				graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
			}
		}	
		
		for(NFFTFingerprint print : fileInfo.fingerprints){
			int timeInMsT1 = (int) (print.t1 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			int timeInMsT2 = (int) (print.t2 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			
			graphics.setColor(Color.ORANGE);
			if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
				float centsF1 = binStartingPointsInCents[print.f1] + binHeightsInCents[print.f1]/2.0f;
				
				float centsF2 = binStartingPointsInCents[print.f2] + binHeightsInCents[print.f2]/2.0f;
				
				graphics.drawLine(Math.round(timeInMsT1), Math.round(centsF1), Math.round(timeInMsT2), Math.round(centsF2));
			}
		}	
		
		for(NFFTFingerprint print : fileInfo.matchingPrints){
			int timeInMsT1 = (int) (print.t1 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			int timeInMsT2 = (int) (print.t2 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			
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
		return "NFFT Audio Info Layer";
	}

	public void drawFFT(boolean selected) {
		drawFFT = selected;
	}

}
