package be.panako.ui.syncsink;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

import be.tarsos.dsp.ui.layers.Layer;

public class ResetCursorLayer implements Layer, MouseMotionListener {

	public ResetCursorLayer() {
		
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if(!e.isConsumed()){
			((JComponent)(e.getSource())).setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	@Override
	public void draw(Graphics2D graphics) {

	}

	@Override
	public String getName() {
		return "Reset Cursor";
	}

}
