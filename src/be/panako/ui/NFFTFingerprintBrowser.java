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




package be.panako.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.panako.ui.syncsink.FileDrop;
import be.panako.util.Config;
import be.panako.util.Key;
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

public class NFFTFingerprintBrowser extends JFrame{

	private static final long serialVersionUID = 8131793763940515009L;	

	private NFFTAudioFileInfo referenceFile;	
	private final List<NFFTAudioFileInfo> otherFiles;
	private final List<Component> featurePanels;
	private final List<NFFTAudioInfoLayer> infoLayers;
	private final JPanel fingerprintPanel;
	
	private CoordinateSystem cs;
	
	public NFFTFingerprintBrowser(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("NFFT Fingerprint Visualizer");
		
		fingerprintPanel = new JPanel();
		fingerprintPanel.setLayout(new GridLayout(0,1));
		fingerprintPanel.add(emptyFeaturePanel());
		
		otherFiles = new ArrayList<NFFTAudioFileInfo>();
		featurePanels = new ArrayList<Component>();
		infoLayers = new ArrayList<NFFTAudioInfoLayer>();
		
		new FileDrop(null, fingerprintPanel, /*dragBorder,*/ new FileDrop.Listener(){   
			public void filesDropped( java.io.File[] files ){   
				for( int i = 0; i < files.length; i++) {   
					final File fileToAdd = files[i];
					new Thread(new Runnable(){
						@Override
						public void run() {					
		                	addAudio(fileToAdd.getAbsolutePath());
						}}).start();
                }
			}
        });
		
		this.add(fingerprintPanel,BorderLayout.CENTER);
		this.add(createButtonPanel(),BorderLayout.SOUTH);
	}
	
	private Component createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
			}
			
		});
		final JCheckBox drawFFTCheckBox = new JCheckBox("Draw FFT Magnitude?");
		drawFFTCheckBox.setSelected(true);
		drawFFTCheckBox.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				
				for(NFFTAudioInfoLayer layer : infoLayers){
					layer.drawFFT(drawFFTCheckBox.isSelected());
				}
				for(Component c : featurePanels){
					c.repaint();
				}				
			}
		});
		
		
		int maxPrintsPerPoint = Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT);
		final JLabel maxPrintsPerPointLabel = new JLabel("Max prints per point " + maxPrintsPerPoint);
		final JSlider maxPrintsPerPointSlider = new JSlider(1,20);
		maxPrintsPerPointSlider.setValue(maxPrintsPerPoint);
		maxPrintsPerPointSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = maxPrintsPerPointSlider.getValue();
				if(!maxPrintsPerPointSlider.getValueIsAdjusting()){
					Config.set(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT, ""+value);
					recalculate();
				}	
				maxPrintsPerPointLabel.setText("Max prints per point " + value);
			}
		});
		

		int minEventPointDistanceValue = Config.getInt(Key.NFFT_EVENT_POINT_MIN_DISTANCE);
		final JLabel minEventPointDistanceLabel = new JLabel("Min distance " + minEventPointDistanceValue);
		final JSlider minEventPointDistance = new JSlider(20, 2000);
		minEventPointDistance.setValue(minEventPointDistanceValue);
		minEventPointDistance.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = minEventPointDistance.getValue();
				if(!minEventPointDistance.getValueIsAdjusting()){
					Config.set(Key.NFFT_EVENT_POINT_MIN_DISTANCE, ""+value);
					recalculate();
				}	
				minEventPointDistanceLabel.setText("Min distance " + value);
			}
		});
		
		
		
		buttonPanel.add(clearButton);
		buttonPanel.add(maxPrintsPerPointLabel);
		buttonPanel.add(maxPrintsPerPointSlider);
		buttonPanel.add(minEventPointDistanceLabel);
		buttonPanel.add(minEventPointDistance);
		buttonPanel.add(drawFFTCheckBox);
		return buttonPanel;
	}
	
	protected void recalculate() {
		File ref = referenceFile.getFile();
		ArrayList<File> otherFilesLocationsCopy = new ArrayList<File>();
		for(NFFTAudioFileInfo info : otherFiles){
			otherFilesLocationsCopy.add(info.getFile());
		}
		clear();
		addAudio(ref.getAbsolutePath());
		for(File file : otherFilesLocationsCopy){
			addAudio(file.getAbsolutePath());
		}
	}

	private void clear() {
		otherFiles.clear();
		referenceFile = null;
		featurePanels.clear();

		// run ui stuff on ui thread.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fingerprintPanel.removeAll();
				fingerprintPanel.add(emptyFeaturePanel());
				//Validating a container means laying out its subcomponents:
				fingerprintPanel.validate();
			}
		});
	}
	
	private Component emptyFeaturePanel(){
		final CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, 3500, 11900);
		final LinkedPanel frequencyDomainPanel = new LinkedPanel(cs);
		frequencyDomainPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				frequencyDomainPanel.repaint();
			
			}
		});	
		frequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		frequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		frequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		frequencyDomainPanel.addLayer(new FrequencyAxisLayer(cs));
		frequencyDomainPanel.addLayer(new MarkCenterLayer(cs));
		frequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		frequencyDomainPanel.addLayer(new SelectionLayer(cs));
		return frequencyDomainPanel;
	}

	
	private Component createFeaturePanel(NFFTAudioFileInfo audioFileInfo) {
		
		final LinkedPanel frequencyDomainPanel = new LinkedPanel(cs);
		frequencyDomainPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				for(Component c : featurePanels){
					c.repaint();
				}
			}
		});
		NFFTAudioInfoLayer infoLayer = new NFFTAudioInfoLayer(cs,audioFileInfo);
		infoLayers.add(infoLayer);
		
		frequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		frequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		frequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		
		frequencyDomainPanel.addLayer(infoLayer);
		frequencyDomainPanel.addLayer(new MarkCenterLayer(cs));
		frequencyDomainPanel.addLayer(new FrequencyAxisLayer(cs));
		frequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		frequencyDomainPanel.addLayer(new SelectionLayer(cs));
		
		this.featurePanels.add(frequencyDomainPanel);
		return frequencyDomainPanel;
	}

	public void addAudio(String audioFile) {
		final Runnable uiRunnable;
		if(referenceFile == null){			
			cs = new CoordinateSystem(AxisUnit.FREQUENCY, 3500, 11900);
			//remove the empty feature panel
			referenceFile = new NFFTAudioFileInfo(new File(audioFile), null);
			referenceFile.extractInfoFromAudio(fingerprintPanel);
			final Component featurePanel = createFeaturePanel(referenceFile);
			uiRunnable = new Runnable() {
				@Override
				public void run() {
					fingerprintPanel.removeAll();
					fingerprintPanel.add(featurePanel);
					//Validating a container means laying out its subcomponents:
					fingerprintPanel.validate();
				}
			};
		}else{
			NFFTAudioFileInfo otherFileInfo = new NFFTAudioFileInfo(new File(audioFile), referenceFile);
			this.otherFiles.add(otherFileInfo);
			otherFileInfo.extractInfoFromAudio(fingerprintPanel);
			
			final Component featurePanel = createFeaturePanel(otherFileInfo);
			uiRunnable = new Runnable() {
				@Override
				public void run() {
					fingerprintPanel.add(featurePanel);
					//Validating a container means laying out its subcomponents:
					fingerprintPanel.validate();
				}
			};
		}
		// run ui stuff on ui thread.
		SwingUtilities.invokeLater(uiRunnable);
	}	

}
