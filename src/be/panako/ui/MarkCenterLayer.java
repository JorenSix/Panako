/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2021 - Joren Six / IPEM                             *
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
