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

package be.panako.ui.syncsink;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;

import be.panako.util.Config;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;

public class StreamLayer implements Layer, MouseListener,MouseMotionListener{
	
	private final int index;
	private final Color color;
	private String description;
	private final boolean isReference;
	private final CoordinateSystem cs;
	private final List<Float> startTimes;//in ms
	private final List<Float> stopTimes;//in ms
	private final float streamDuration;//duration in ms
	private float guessedStartTimeOfStream;
	private Graphics2D graphics;
	private final List<File> dataFiles;
	
	public StreamLayer(CoordinateSystem cs,int index, Color color,String description, boolean isReference,float streamDuration){
		this.index = index;
		this.color = color;
		this.description = description;
		this.isReference = isReference;
		this.cs = cs;
		this.startTimes = new ArrayList<Float>();
		this.stopTimes = new ArrayList<Float>();
		this.streamDuration = streamDuration;
		this.dataFiles = new ArrayList<File>();
	}
	
	public void addInterval(float startTimeInReference, float stopTimeInReference,float startTimeInResource, float stopTimeInResource){
		startTimes.add(startTimeInReference);
		stopTimes.add(stopTimeInReference);
		guessedStartTimeOfStream = (startTimeInReference - startTimeInResource);
	}
	
	

	@Override
	public void draw(Graphics2D graphics) {
		this.graphics = graphics;
		float spacer = LayerUtilities.pixelsToUnits(graphics, 20, false);
		float heightOfABlock = LayerUtilities.pixelsToUnits(graphics, 30, false);
		
		int verticalOffsetOffset = -1 * (Math.round((index + 1) * spacer + index * heightOfABlock));
		
		//draw dotted lines
		if(isReference){
			int startTime = Math.round(startTimes.get(0));
			int stopTime = Math.round(stopTimes.get(0));
			
			int grayScale = 80;
			Color lightGray = new Color(grayScale,grayScale,grayScale,grayScale);
			graphics.setColor(lightGray);
			int maxY = Math.round(cs.getMax(Axis.Y));
			int minY = Math.round(cs.getMin(Axis.Y));
			graphics.drawLine(startTime, minY, startTime, maxY);
			graphics.drawLine(stopTime, minY, stopTime, maxY);
		}else{
			int startTime = Math.round(guessedStartTimeOfStream);
			int stopTime = Math.round(guessedStartTimeOfStream+streamDuration);
			
			Color backgroundColor = Color.LIGHT_GRAY;
			graphics.setColor(backgroundColor);
			//graphics.fillRect(startTime, verticalOffsetOffset, stopTime-startTime, Math.round(heightOfABlock));
			graphics.drawRect(startTime, verticalOffsetOffset, stopTime-startTime, Math.round(heightOfABlock));
			
			graphics.setColor(color);
			float verticalTextPosition = verticalOffsetOffset + heightOfABlock/2.0f;
			LayerUtilities.drawString(graphics, description, (stopTime+startTime)/2.0f, verticalTextPosition , true, true, null);
		}
		
		
		
		for(int i = 0 ; i < stopTimes.size() ; i++){
			
			int startTime = Math.round(startTimes.get(i));
			int stopTime = Math.round(stopTimes.get(i));
			
			Color backgroundColor = getBackgroundColor();
			graphics.setColor(backgroundColor);
			graphics.fillRect(startTime, verticalOffsetOffset, stopTime-startTime, Math.round(heightOfABlock));
			
			//a block 
			graphics.setColor(color);
			graphics.drawRect(startTime, verticalOffsetOffset, stopTime-startTime, Math.round(heightOfABlock));
		}
		
		int startTime = Math.round(guessedStartTimeOfStream);
		int stopTime = Math.round(guessedStartTimeOfStream+streamDuration);

		
		graphics.setColor(color);
		float verticalTextPosition = verticalOffsetOffset + heightOfABlock/2.0f;
		LayerUtilities.drawString(graphics, description, (stopTime+startTime)/2.0f, verticalTextPosition , true, true, null);

	
	}
	
	private Color getBackgroundColor(){
		float[] components = new float[3];
		color.getColorComponents(components);
		Color backgroundColor = new Color(ColorSpace.getInstance(ColorSpace.CS_sRGB),components , 0.13f);
		return backgroundColor;
	}

	@Override
	public String getName() {
		return "StreamLayer";
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(graphics!=null){
			float spacer = LayerUtilities.pixelsToUnits(graphics, 20, false);
			float heightOfABlock = LayerUtilities.pixelsToUnits(graphics, 30, false);		
			int verticalOffsetOffset = -1 * (Math.round((index + 1) * spacer + index * heightOfABlock));
			int startHeight = verticalOffsetOffset ;
			int stopHeight = verticalOffsetOffset + Math.round(heightOfABlock); 
	
			Point2D pointInUnits = LayerUtilities.pixelsToUnits(graphics, e.getX(), e.getY());
			int startTime = Math.round(guessedStartTimeOfStream);
			int stopTime = Math.round(guessedStartTimeOfStream+streamDuration);
			if(pointInUnits.getX() >= startTime && pointInUnits.getX() <= stopTime && pointInUnits.getY() >= startHeight &&  pointInUnits.getY() <= stopHeight){
				//System.out.println("Click in layer " + index);
				
				JFileChooser chooser =  new JFileChooser(new File(Config.getPreference("SYNC_DIR")));
				chooser.setDialogTitle("Choose corresponding data file.");
			    int returnVal = chooser.showOpenDialog(null);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	File file = chooser.getSelectedFile();
			    	Config.setPreference("SYNC_DIR", chooser.getSelectedFile().getPath());
			    	this.dataFiles.add(file);
			    	this.description = description + " + " + file.getName();
			    	graphics.dispose();
			    }
			}
		}
	}
	
	public List<File> getDataFiles(){
		return this.dataFiles;
	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if(graphics!=null){
			float spacer = LayerUtilities.pixelsToUnits(graphics, 20, false);
			float heightOfABlock = LayerUtilities.pixelsToUnits(graphics, 30, false);		
			int verticalOffsetOffset = -1 * (Math.round((index + 1) * spacer + index * heightOfABlock));
			int startHeight = verticalOffsetOffset ;
			int stopHeight = verticalOffsetOffset + Math.round(heightOfABlock); 
	
			Point2D pointInUnits = LayerUtilities.pixelsToUnits(graphics, e.getX(), e.getY());
			int startTime = Math.round(guessedStartTimeOfStream);
			int stopTime = Math.round(guessedStartTimeOfStream+streamDuration);
			if(pointInUnits.getX() >= startTime && pointInUnits.getX() <= stopTime && pointInUnits.getY() >= startHeight &&  pointInUnits.getY() <= stopHeight){
				((JComponent)(e.getSource())).setCursor(new Cursor(Cursor.HAND_CURSOR));
				e.consume();
			}
		}
	}
	}
