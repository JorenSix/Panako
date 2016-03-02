/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2015 - Joren Six / IPEM                             *
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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;


public class SyncSinkMarkerFrame extends JFrame implements ViewPortChangedListener{
	
	private static final long serialVersionUID = 1L;
	private final static Logger LOG =  Logger.getLogger(SyncSinkMarkerFrame.class.getName());
	private final CoordinateSystem cs;
	private final LinkedPanel linkedPanel;
	private JLabel statusBar;
	private final List<File> streamFiles;
	private final List<Double> markerPositions;
	private final List<StreamLayer> streamLayers;
	private final ResetCursorLayer resetCursorLayer = new ResetCursorLayer();
	private float maxDuration;
	
	private JButton syncButton;
	private JButton clearButton;
	private final JTextArea logTextField;
	
	private final Color[] colorMap =    {   
			new Color(0xFFFFB300), //Vivid Yellow
		    new Color(0xFF803E75), //Strong Purple
		    new Color(0xFFFF6800), //Vivid Orange
		    new Color(0xFFA6BDD7), //Very Light Blue
		    new Color(0xFFC10020), //Vivid Red
		    new Color(0xFFCEA262), //Grayish Yellow
		    new Color(0xFF817066), //Medium Gray
		    
		    //The following will not be good for people with defective color vision
		    new Color(0xFF007D34), //Vivid Green
		    new Color(0xFFF6768E), //Strong Purplish Pink
		    new Color(0xFF00538A), //Strong Blue
		    new Color(0xFFFF7A5C), //Strong Yellowish Pink
		    new Color(0xFF53377A), //Strong Violet
		    new Color(0xFFFF8E00), //Vivid Orange Yellow
		    new Color(0xFFB32851), //Strong Purplish Red
		    new Color(0xFFF4C800), //Vivid Greenish Yellow
		    new Color(0xFF7F180D), //Strong Reddish Brown
		    new Color(0xFF93AA00), //Vivid Yellowish Green
		    new Color(0xFF593315), //Deep Yellowish Brown
		    new Color(0xFFF13A13), //Vivid Reddish Orange
		    new Color(0xFF232C16) //Dark Olive Green}
	};
	
	
	public SyncSinkMarkerFrame(){
		super("SyncSink");
		
		markerPositions = new ArrayList<Double>();
		logTextField = new JTextArea();
		logTextField.setEditable(false);
		DefaultCaret caret = (DefaultCaret)logTextField.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		

		StringBuilder sb = new StringBuilder();
		sb.append("----------------------------------------\n");
		sb.append("Configuration currently in use: \n");
		for(Key key : Key.values()){
			sb.append("\t");
			sb.append(key.name());
			sb.append("=");
			sb.append(Config.get(key));
			sb.append("\n");
		}
		sb.append("----------------------------------------\n");
		logTextField.setText(sb.toString());
		
		
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		cs = new CoordinateSystem(AxisUnit.OCCURENCES, 0, 1000);
		
		linkedPanel = new LinkedPanel(cs);
		linkedPanel.addLayer(new BackgroundLayer(cs));
		linkedPanel.addLayer(new ZoomMouseListenerLayer());
		linkedPanel.addLayer(new DragMouseListenerLayer(cs));
		linkedPanel.addLayer(new BackgroundLayer(cs));
		linkedPanel.addLayer(new TimeAxisLayer(cs));
		linkedPanel.addLayer(new SelectionLayer(cs));
		linkedPanel.getViewPort().addViewPortChangedListener(this);
		
		
		this.streamLayers = new ArrayList<StreamLayer>();
		this.streamFiles = new ArrayList<File>();
		
		JTabbedPane tabbedPane = new JTabbedPane();

		
		tabbedPane.addTab("Timebox plot", null, linkedPanel,"Timebox plots");
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

		
		tabbedPane.addTab("Messages", null, new JScrollPane(logTextField),"Logs messages");
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		
		tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
		tabbedPane.setBorder(new EmptyBorder(0,0,0,0));
		
		this.add(tabbedPane,BorderLayout.CENTER);
		this.add(createStatusBarPanel(),BorderLayout.SOUTH);
		
		new FileDrop(null, tabbedPane, /*dragBorder,*/ new FileDrop.Listener(){   
			public void filesDropped( java.io.File[] files ){   
				for( int i = 0; i < files.length; i++) {   
					final File fileToAdd = files[i];
					new Thread(new Runnable(){
						@Override
						public void run() {
							logMessage("Adding " + fileToAdd.getPath()  + "...");							
		                	openFile(fileToAdd,streamFiles.size());
		                	logMessage("Added " + fileToAdd.getPath()  + ".");
						}}).start();
					try {
						Thread.sleep(60);
					} catch (InterruptedException e) {
					}
                }
			}
        });
	}
	
	public void logMessage(String lineToLog){
		statusBar.setText(lineToLog);
		logTextField.setText(logTextField.getText() + "\n" + lineToLog);
		LOG.info(lineToLog);
	}
	
	private JComponent createStatusBarPanel(){
		statusBar = new JLabel();
		logMessage("Use drag and drop to synchronize audio and video files. Start with the reference file.");
		statusBar.setEnabled(false);
		Border paddingBorder = BorderFactory.createEmptyBorder(0,10,0,0);
		statusBar.setBorder(paddingBorder);
		
		syncButton = new JButton("Sync!");
		syncButton.setEnabled(false);
		syncButton.setMargin(new Insets(2,2,2,2));
		syncButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				synchronizeMedia();
			}
		});
		
		clearButton = new JButton("Clear");
		clearButton.setEnabled(false);
		clearButton.setMargin(new Insets(2,2,2,2));
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});
		
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 10));
		buttonPanel.add(statusBar,BorderLayout.CENTER);
		buttonPanel.add(syncButton,BorderLayout.EAST);
		buttonPanel.add(clearButton,BorderLayout.WEST);
		
		return buttonPanel;
	}
	
	private void synchronizeMedia(){
		//float referenceFileDuration = getMediaDuration(streamFiles.get(0).getAbsolutePath());
		String commandFile = streamFiles.get(0).getParent() + File.separator + "sync_ffmpeg_commands.bash";
		for(int i = 0 ; i < streamFiles.size() ; i++){
			//float otherFileDuration = getMediaDuration(streamFiles.get(i).getAbsolutePath());
			
			
			boolean isVideo = streamFiles.get(i).getName().matches("(?i).*(mpg|avi|mp4|mkv|mpeg)");
			
			float guessedStartTimeOfStream = (float) (-markerPositions.get(i));
			String command;
			if(guessedStartTimeOfStream >= 0){
				//generate silence				
				if(isVideo){
					//String syncedmediaFile = "synced_" + streamFiles.get(i).getName();
					command = "command to add black frames here";
					logMessage("ffmpeg sync command: " + command);
				}else{
					String syncedmediaFile = "synced_" + streamFiles.get(i).getName();
					command = "ffmpeg -f lavfi -i aevalsrc=0:d="+guessedStartTimeOfStream+" -i  \"" + streamFiles.get(i) +  "\"  -filter_complex \"[0:0] [1:0] concat=n=2:v=0:a=1 [a]\" -map [a] \"" + syncedmediaFile + "\"";
					logMessage("ffmpeg sync command: " + command);
				}
			}else{
				//cut the first part away
				String startString = String.format("%.3f", -1 * guessedStartTimeOfStream);
				String syncedmediaFile = "synced_" + streamFiles.get(i).getName();
				if(isVideo){
					command = "ffmpeg -ss " + startString + " -i \"" + streamFiles.get(i) +  "\" \"" + syncedmediaFile + "\"";
					logMessage("ffmpeg sync command: " + command);
				}else{
					command = "ffmpeg -ss " + startString + " -i \"" + streamFiles.get(i) +  "\" \"" + syncedmediaFile + "\"";
					logMessage("ffmpeg sync command: " + command);
				}
			}
			
			logMessage("Wrinting to command file: " +  commandFile);
			logMessage("Wrinting command: " +  command);
			appendToCommandFile(commandFile, command);
			logMessage("Appending command file: " +  commandFile);
			for(File dataFile : streamLayers.get(i).getDataFiles()){
				File shiftedCSVFile = new File("synced_" + dataFile.getName());
				try {
					modifyCSVFile(dataFile, shiftedCSVFile, guessedStartTimeOfStream);
					logMessage("Synced CSV file original:" + dataFile.getAbsolutePath() + " synced: " + shiftedCSVFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void openFile(File file,int streamIndex){
		String fileName = file.getName();
		LOG.info("Opening file: " + file.getName());
		if(streamIndex==0){
    	 	streamFiles.add(file);
    		float duration = getMediaDuration(file.getAbsolutePath());
    		float marker = (float) findMarker(0);
    		
    		StreamLayer refLayer = new StreamLayer(cs,streamIndex,colorMap[streamIndex],fileName,false,duration*1000,this.streamFiles);
    		
			refLayer.addInterval(0-marker*1000, duration*1000-marker*1000,0,0);
			maxDuration = duration;
			
			cs.setMax(Axis.X,maxDuration*1.1f);
			cs.setMin(Axis.X,-2000);
			
			cs.setMax(Axis.Y,30);
			cs.setMin(Axis.Y,-500);
			
			cs.setStartPoint(-2000, 30);
			cs.setEndPoint(1000*duration+2000,-500);
			
			linkedPanel.getViewPort().zoomToSelection();
			
			linkedPanel.addLayer(refLayer);
			streamLayers.add(refLayer);
			linkedPanel.removeLayer(resetCursorLayer);
			linkedPanel.addLayer(resetCursorLayer);
			clearButton.setEnabled(true);
    	}else{
    		streamFiles.add(file);
    		
    		float duration = getMediaDuration(file.getAbsolutePath());
    		if(duration > maxDuration){
    			maxDuration = duration;
    		}
    		
    		cs.setMax(Axis.X,maxDuration*1.1f);
			cs.setMin(Axis.X,-2000);
			cs.setMax(Axis.Y,30);
			cs.setMin(Axis.Y,-500);
    		
    		cs.setStartPoint(-2000, 30);
			cs.setEndPoint(1000*maxDuration+2000,-500);				
    	
    		StreamLayer otherLayer = new StreamLayer(cs,streamIndex,colorMap[streamIndex%colorMap.length],fileName,false,duration*1000,this.streamFiles);
    		
    		float marker = (float) findMarker(streamFiles.size()-1);
    		
    		
    		otherLayer.addInterval(0-marker*1000, duration*1000-marker*1000,0,0);
    		
    		logMessage(String.format("Determined offset of %s with respect to reference audio of %.04f ",fileName,marker-markerPositions.get(0)));
    	
			linkedPanel.addLayer(otherLayer);
			streamLayers.add(otherLayer);
			linkedPanel.getViewPort().zoomToSelection();
			syncButton.setEnabled(true);
			
			linkedPanel.removeLayer(resetCursorLayer);
			linkedPanel.addLayer(resetCursorLayer);
    	}
    	streamIndex++;
	}
	
	private void clear(){
		for(StreamLayer layerToRemove : streamLayers){
			linkedPanel.removeLayer(layerToRemove);
		}
		streamLayers.clear();
		streamFiles.clear();
		clearButton.setEnabled(false);
		syncButton.setEnabled(false);
		markerPositions.clear();
		this.statusBar.setText("Use drag and drop to synchronize audio and video files. Start with the reference file.");
		this.repaint();
	}

	private float getMediaDuration(String absoluteFileName){
		
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(absoluteFileName, 44100, 1024, 0);
		//bit hackish...
		final double[] duration = {0.0};
		adp.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				duration[0] = audioEvent.getTimeStamp();
				return true;
			}
		});
		adp.run();
		LOG.info(String.format("Duration of file %s is %.02fs",absoluteFileName,duration[0]));
		return (float) duration[0];
	}
	
	private void appendToCommandFile(String commandFile,String command){
		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(commandFile, true)));
		    out.println(command);
		    out.close();
		} catch (IOException e) {
		    //exception handling left as an exercise for the reader
		}
	}
	
	
	public void modifyCSVFile(File csvFile,File shiftedCSVFile,double shift) throws IOException{
		
		if (!csvFile.exists()) {
			throw new IllegalArgumentException("File '" + csvFile + "' does not exist");
		}
		
		FileReader fileReader = new FileReader(csvFile);
		BufferedReader in = new BufferedReader(fileReader);
		
		FileWriter fileWriter = new FileWriter(shiftedCSVFile);
		BufferedWriter out = new BufferedWriter(fileWriter);
		
		String inputLine = in.readLine();	
		while (inputLine != null) {
			final String[] row = inputLine.split(",");
			if (!inputLine.trim().isEmpty() && row.length > 1) {
				double timeInSeconds = Double.valueOf(row[0]);
				double shiftedTime = timeInSeconds + shift;
				String outputLine = String.format("%.4f" , shiftedTime)  + inputLine.substring(row[0].length(), inputLine.length()) + System.lineSeparator();
				out.write(outputLine);
			}
			inputLine = in.readLine();
		}
		out.close();
		in.close();
	}
	
	float bufferTime;
	float maxTime;
	private double findMarker(int streamIndex){
		double match = 0.0;
		
		double loudnessDelta = Config.getFloat(Key.SYNC_MARKER_LOUDNESS_DELTA);
		double errorAllowed = Config.getFloat(Key.SYNC_MARKER_TIME_ERROR_ALLOWED);
		MarkDetector d = new MarkDetector(300, errorAllowed, loudnessDelta);
		AudioDispatcher ref = AudioDispatcherFactory.fromPipe(streamFiles.get(streamIndex).getAbsolutePath(),10000, 10,0);
		ref.addAudioProcessor(d);
		ref.run();
		
		List<Double> markers = d.getMarkers();
		if(markers.size()>0){
			match = markers.get(0);
		}
		
		markerPositions.add(match);
		logMessage("Marker found at  " + match  + "s");
		return match;
	}
	

	@Override
	public void viewPortChanged(ViewPort newViewPort) {
		this.repaint();
	}
	
	

}
