package be.panako.ui.syncsink;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;
import be.panako.strategy.nfft.NFFTStreamSync;
import be.panako.strategy.nfft.NFFTSyncMatch;
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



public class SyncSinkFrame extends JFrame implements ViewPortChangedListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final CoordinateSystem cs;
	private final LinkedPanel linkedPanel;
	private int streamIndex = 0;
	private List<File> streamFiles;
	private List<NFFTSyncMatch> matches;
	private float maxDuration;
	
	
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
	
	
	public SyncSinkFrame(){
		super("SyncSink");
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
		
		matches = new ArrayList<NFFTSyncMatch>();
		
		this.streamFiles = new ArrayList<File>();
		this.add(linkedPanel,BorderLayout.CENTER);
		this.add(createButtonPanel(),BorderLayout.SOUTH);
	}
	
	private JComponent createButtonPanel(){
		
		JPanel audioSource = new JPanel();
		
		JButton addButton = new JButton("Add...");
		
		addButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Choose video, audio or data file with embedded audio.");
			    int returnVal = chooser.showOpenDialog(SyncSinkFrame.this);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	
			    	openFile(chooser.getSelectedFile());
			    }	
			}
		});
		
		audioSource.add(addButton);
		
		JButton syncButton = new JButton("Sync!");
		syncButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//float referenceFileDuration = getMediaDuration(streamFiles.get(0).getAbsolutePath());
				for(int i = 1 ; i < streamFiles.size() ; i++){
					//float otherFileDuration = getMediaDuration(streamFiles.get(i).getAbsolutePath());
					
					NFFTSyncMatch match = matches.get(i-1);
					float[] matchInfo = match.getMatch(0);
					
					float guessedStartTimeOfStream = (matchInfo[0] - matchInfo[2]);
					if(guessedStartTimeOfStream >= 0){
						//generate silence
						String command = "/opt/ffmpeg/ffmpeg -f lavfi -i aevalsrc=0:d="+guessedStartTimeOfStream+" -i  " + streamFiles.get(i) +  "  -filter_complex \"[0:0] [1:0] concat=n=2:v=0:a=1 [a]\" -map [a] synced.wav";
						System.out.println(command);
					}else{
						//cut the first part away
						String startString = String.format("%.3f", -1 * guessedStartTimeOfStream);
						
						String command = "/opt/ffmpeg/fmpeg -ss " + startString + " -i " + streamFiles.get(i) +  " synced.wav";
						System.out.println(command);
					}
				}
			}
		});
		
		
		JCheckBox outputOptions = new JCheckBox("Output only guaranteed synchronized files?");
		outputOptions.setToolTipText("If not checked then the assumption is that the offset stays the same for the whole file");
		
		audioSource.add(outputOptions);
		
		audioSource.add(syncButton);
		
		
		
		return audioSource;
	}
	
	public void openFile(File file){
		String fileName = file.getName();
		if(streamIndex==0){
    	 	streamFiles.add(file);
    		float duration = getMediaDuration(file.getAbsolutePath());
    		StreamLayer refLayer = new StreamLayer(cs,streamIndex,colorMap[streamIndex],fileName,true,duration*1000);
			refLayer.addInterval(0, duration*1000,0,0);
			maxDuration = duration;
			
			cs.setMax(Axis.X,maxDuration*1.1f);
			cs.setMin(Axis.X,-2000);
			
			cs.setMax(Axis.Y,30);
			cs.setMin(Axis.Y,-500);
			
			cs.setStartPoint(-2000, 30);
			cs.setEndPoint(1000*duration+2000,-500);
			
			linkedPanel.getViewPort().zoomToSelection();
			
			linkedPanel.addLayer(refLayer);
			
    	}else{
    		streamFiles.add(file);
    		NFFTSyncMatch match = sync();
    		
    		matches.add(match);
    		
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
    	
    		StreamLayer otherLayer = new StreamLayer(cs,streamIndex,colorMap[streamIndex],fileName,false,duration*1000);
    		
    		for(int i = 0 ; i < match.getNumberOfMatches();i++){
    			float[] matchInfo = match.getMatch(i);
    			float startTimeInRef = matchInfo[0];
    			float stopTimeInRef = matchInfo[1];
    			otherLayer.addInterval(startTimeInRef*1000,stopTimeInRef*1000,matchInfo[2]*1000,matchInfo[3]*1000);
    		}
    		
    		
    		
			linkedPanel.addLayer(otherLayer);
			linkedPanel.getViewPort().zoomToSelection();
			
    	}
    	streamIndex++;
	}
	
	double duration = 0;
	private float getMediaDuration(String absoluteFileName){
		
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(absoluteFileName, 44100, 1024, 0);
		
		adp.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				duration = audioEvent.getTimeStamp();
				return true;
			}
		});
		adp.run();
		return (float) duration;
	}
	
	private NFFTSyncMatch sync(){
		Config.set(Key.valueOf("NFFT_SAMPLE_RATE"),"8000");
		Config.set(Key.valueOf("NFFT_SIZE"),"512");
		Config.set(Key.valueOf("NFFT_STEP_SIZE"),"128");
		Config.set(Key.valueOf("STRATEGY"),"NFFT");
		Config.set(Key.valueOf("SYNC_MIN_ALIGNED_MATCHES"),"6");
		
		Strategy strategy = Strategy.getInstance();
		NFFTStrategy strat = (NFFTStrategy) strategy;
		String reference = streamFiles.get(0).getAbsolutePath();
		String[] others = {streamFiles.get(streamIndex).getAbsolutePath()};
		NFFTStreamSync sync = strat.sync(reference, others);
		//others is only one, so get the first Match list:
		NFFTSyncMatch match = sync.getMatches().get(0);
		match.removeOverlappingMatchesWithLowerScores();
		match.removeMatchesWithLowerScores(0.5f);
		return match;
	}

	@Override
	public void viewPortChanged(ViewPort newViewPort) {
		this.repaint();
	}
	
	

}
