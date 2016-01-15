package be.panako.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;

public class MarkCenterLayer implements Layer {

	private final CoordinateSystem cs;
	private boolean enabled;
	
	public MarkCenterLayer(CoordinateSystem cs) {
		this.enabled = true;
		this.cs = cs;
	}

	@Override
	public void draw(Graphics2D graphics) {
		if(enabled){
			float maxX = cs.getMax(Axis.X);
			float minX = cs.getMin(Axis.X);
			int middle = Math.round((minX+maxX)/2.0f);
			int minY = Math.round(cs.getMin(Axis.Y));
			int maxY = Math.round(cs.getMax(Axis.Y));
			graphics.setColor(Color.CYAN);
			graphics.drawLine(middle,minY , middle, maxY);
			middle = Math.round((minY+maxY)/2.0f);
			graphics.setColor(Color.CYAN);
			graphics.drawLine( Math.round(minX),middle , Math.round(maxX),middle);
		}
	}
	
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}

	@Override
	public String getName() {
		return "Mark Center Layer";
	}

}
