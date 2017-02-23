package be.panako.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Map;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.strategy.qifft.QIFFTEventPoint;
import be.panako.strategy.qifft.QIFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.util.PitchConverter;

public class QIFFTAudioInfoLayer implements Layer, MouseMotionListener {
	
	final float[] binStartingPointsInCents;
	final float[] binHeightsInCents;
	private final CoordinateSystem cs;
	private final QIFFTAudioFileInfo fileInfo;
	private boolean drawFFT;
	
	private Graphics2D graphics;
	
	public QIFFTAudioInfoLayer(CoordinateSystem cs,QIFFTAudioFileInfo fileInfo) {
		int size = Config.getInt(Key.NFFT_SIZE);
		binStartingPointsInCents = new float[size*4];
		binHeightsInCents = new float[size*4];
		for (int i = 1; i < size * 4; i++) {
			binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(i * 8000 / (double) binStartingPointsInCents.length);
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		this.cs = cs;
		this.fileInfo = fileInfo;
	}

	@Override
	public void draw(Graphics2D graphics) {
		this.graphics = graphics;
		float frameDurationInMS = Config.getInt(Key.NFFT_STEP_SIZE)/  ((float) Config.getInt(Key.NFFT_SAMPLE_RATE)) * 1000.f;
		float frameOffsetInMS = frameDurationInMS/2.0f ;
		
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
		
		for(QIFFTEventPoint point : fileInfo.eventpoints){
			int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000) ;
			graphics.setColor(Color.BLUE);			
			if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
				float cents = point.getFrequencyInCents();
				float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
				float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
				graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
			}
		}
		
		
		for(QIFFTFingerprint print : fileInfo.fingerprints){
			int timeInMsT1 = (int) (print.t1 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			int timeInMsT2 = (int) (print.t2 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			graphics.setColor(Color.ORANGE);
			if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
				graphics.drawLine(Math.round(timeInMsT1), (int) Math.round(print.f1Estimate), Math.round(timeInMsT2),(int) Math.round(print.f2Estimate));
			}
		}
		
		for(QIFFTEventPoint point : fileInfo.matchingEventPoints){
			int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000 );
			graphics.setColor(Color.GREEN);
			if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
				float cents = point.getFrequencyInCents();
				float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
				float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
				graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
			}
		}
		
		for(QIFFTFingerprint print : fileInfo.matchingPrints){
			int timeInMsT1 = (int) (print.t1 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			int timeInMsT2 = (int) (print.t2 * frameDurationInMS + frameOffsetInMS + fileInfo.getTimeOffset() * 1000);
			graphics.setColor(Color.GREEN);
			if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
				graphics.drawLine(Math.round(timeInMsT1), (int) Math.round(print.f1Estimate), Math.round(timeInMsT2),(int) Math.round(print.f2Estimate));
			}
		}
	}
	
	

	@Override
	public String getName() {		
		return "QIFFT Audio Info Layer";
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
