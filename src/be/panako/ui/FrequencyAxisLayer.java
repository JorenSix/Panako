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

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;


public class FrequencyAxisLayer implements Layer{
	CoordinateSystem cs;
	
	public FrequencyAxisLayer(CoordinateSystem cs) {
		this.cs = cs;
	}

	public void draw(Graphics2D graphics){
		
		//draw legend
		graphics.setColor(Color.black);
		int minX = Math.round(cs.getMin(Axis.X));
		int maxY = Math.round(cs.getMax(Axis.Y));
		
		int wideMarkWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,8, true));
		int smallMarkWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,4, true));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, true));	
		int textLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, false));
		
		//Every 100 and 1200 cents
		for(int i = (int) cs.getMin(Axis.Y) ; i < cs.getMax(Axis.Y) ; i++){
			if(i%1200 == 0){
				graphics.drawLine(minX, i, minX+wideMarkWidth,i);
				String text = String.valueOf(i);				
				LayerUtilities.drawString(graphics,text,minX+textOffset,i,false,true,null);
			} else if(i%100 == 0){			
				graphics.drawLine(minX, i, minX+smallMarkWidth,i);
			}
		}
		
		LayerUtilities.drawString(graphics,"Frequency (cents)",minX+textOffset,maxY-textLabelOffset,false,true,Color.white);
	}

	@Override
	public String getName() {
		return "Frequency Axis";
	}
}
