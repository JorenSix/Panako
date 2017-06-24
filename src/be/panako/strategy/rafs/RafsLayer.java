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


package be.panako.strategy.rafs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;
import java.util.Map;

import be.panako.util.Config;
import be.panako.util.Hamming;
import be.panako.util.Key;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;

public class RafsLayer implements Layer, MouseMotionListener {

	private final CoordinateSystem cs;
	private final RafsExtractor fileInfo;
	private final RafsExtractor refFileInfo;
	private final float shift;
	private Graphics2D graphics;
	private static RafsStrategy strategy; 
	private boolean showDiff = false;
	float frameDurationInMS; 

	public RafsLayer(CoordinateSystem cs, RafsExtractor fileInfo, RafsExtractor refFileInfo,boolean diff) {
		if(strategy == null){
			strategy = new RafsStrategy();
		}
		
		frameDurationInMS = Config.getFloat(Key.RAFS_FFT_STEP_SIZE) / Config.getFloat(Key.RAFS_SAMPLE_RATE) * 1000.f;
		
		this.cs = cs;
		this.fileInfo = fileInfo;
		this.refFileInfo = refFileInfo;
		if (refFileInfo != null) {
			//round to the nearest frame and subract half a frame in ms
			shift = Math.round(strategy.align(refFileInfo.fingerprints, fileInfo.fingerprints)/frameDurationInMS) * frameDurationInMS - frameDurationInMS/2.0f;
			System.out.println("shift:" + shift);
		} else {
			shift = - frameDurationInMS/2.0f;
		}
		showDiff = diff;
		
	}

	@Override
	public void draw(Graphics2D graphics) {
		this.graphics = graphics;
		
	
		Stroke stroke = graphics.getStroke();
		if (showDiff) {

			Map<Float, BitSet> magnitudesSubMap = refFileInfo.fingerprints.subMap(cs.getMin(Axis.X) / 1000.0f,
					cs.getMax(Axis.X) / 1000.0f);

			graphics.setColor(Color.black);
			for (Map.Entry<Float, BitSet> frameEntry : magnitudesSubMap.entrySet()) {
				double timeStart = frameEntry.getKey();// in seconds
				BitSet refMagnitudes = frameEntry.getValue();
				Map.Entry<Float, BitSet> entry = fileInfo.fingerprints
						.ceilingEntry((float) timeStart + shift / 1000.0f);
				BitSet otherMagnitudes;
				if (entry == null) {
					otherMagnitudes = refMagnitudes;
				} else {
					otherMagnitudes = entry.getValue();
				}

				// draw the pixels
				for (int i = 0; i < refMagnitudes.length(); i++) {
					Color color = Color.black;
					// actual energy at frame.frequencyEstimates[i];
					float centsStartingPoint = i * 150;
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
						int greyValue = (refMagnitudes.get(i) == otherMagnitudes.get(i)) ? 255 : 0;
						color = new Color(greyValue, greyValue, greyValue);
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint),
								(int) Math.round(frameDurationInMS), (int) Math.ceil(150));

					}
				}

				float hammingDistance = Hamming.d(refMagnitudes, otherMagnitudes) * 150 + 75;
				graphics.setColor(Color.red);
				graphics.setStroke(new BasicStroke(75));
				graphics.drawLine((int) Math.round(timeStart * 1000 + frameDurationInMS/2.0f ) + 37, Math.round(hammingDistance),
						(int) Math.round(timeStart * 1000 + frameDurationInMS/2.0f)  + (int) Math.round(frameDurationInMS) + 37,
						Math.round(hammingDistance));
			}
		} else {
			
			Map<Float, BitSet> magnitudesSubMap = fileInfo.fingerprints.subMap(cs.getMin(Axis.X) / 1000.0f + shift / 1000.0f,
					cs.getMax(Axis.X) / 1000.0f + shift / 1000.0f  );

			for (Map.Entry<Float, BitSet> frameEntry : magnitudesSubMap.entrySet()) {
				double timeStart = frameEntry.getKey();// in seconds
				BitSet magnitudes = frameEntry.getValue();

				// draw the pixels
				for (int i = 0; i < magnitudes.length(); i++) {
					Color color = Color.black;

					// actual energy at frame.frequencyEstimates[i];

					float centsStartingPoint = i * 150;
					// only draw the visible frequency range
					if (centsStartingPoint >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(Axis.Y)) {
						int greyValue = magnitudes.get(i) ? 255 : 0;
						color = new Color(greyValue, greyValue, greyValue);
						graphics.setColor(color);
						graphics.fillRect((int) Math.round(timeStart * 1000 - shift + frameDurationInMS/2.0f ), Math.round(centsStartingPoint),
								(int) Math.round(frameDurationInMS), (int) Math.ceil(150));
					}
				}
			}
		}
		graphics.setStroke(stroke);
		graphics.setColor(Color.black);
	}

	@Override
	public String getName() {
		return "Chroma Print Audio Info Layer";
	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (graphics != null) {

		}
	}

}
