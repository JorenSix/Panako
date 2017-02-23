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



package be.panako.strategy.chromaprint;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import be.panako.ui.FrequencyAxisLayer;
import be.panako.ui.syncsink.FileDrop;
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

public class ChromaPrintBrowser extends JFrame{

	private static final long serialVersionUID = 8131793763940515009L;	


	private String referenceFileLocation;
	ChromaPrintExtractor referenceFile;
	
	private final List<ChromaPrintExtractor> otherFiles;
	private final List<Component> featurePanels;
	private final List<ChromaPrintLayer> infoLayers;
	private final JPanel fingerprintPanel;
	
	
	private CoordinateSystem cs;
	
	public ChromaPrintBrowser(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("NFFT Fingerprint Visualizer");
		
		fingerprintPanel = new JPanel();
		fingerprintPanel.setLayout(new GridLayout(0,1));
		fingerprintPanel.add(emptyFeaturePanel());
		
		otherFiles = new ArrayList<ChromaPrintExtractor>();
		featurePanels = new ArrayList<Component>();
		infoLayers = new ArrayList<ChromaPrintLayer>();
		
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
		JButton clearButton = new JButton("Recalculate");
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});
		
		
		buttonPanel.add(clearButton);
		return buttonPanel;
	}
	
	private void clear() {
		otherFiles.clear();
		featurePanels.clear();

		// run ui stuff on ui thread.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fingerprintPanel.removeAll();
				fingerprintPanel.add(emptyFeaturePanel());
				//Validating a container means laying out its subcomponents:
				fingerprintPanel.validate();
				addAudio(referenceFileLocation);
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
		frequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		frequencyDomainPanel.addLayer(new SelectionLayer(cs));
		return frequencyDomainPanel;
	}

	
	private Component createFeaturePanel(ChromaPrintExtractor audioFileInfo) {
		
		final LinkedPanel frequencyDomainPanel = new LinkedPanel(cs);
		frequencyDomainPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				for(Component c : featurePanels){
					c.repaint();
				}
			}
		});
		ChromaPrintLayer infoLayer = new ChromaPrintLayer(cs,audioFileInfo);
		infoLayers.add(infoLayer);
		
		frequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		frequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		frequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		frequencyDomainPanel.addLayer(infoLayer);
		frequencyDomainPanel.addLayer(new FrequencyAxisLayer(cs));
		frequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		frequencyDomainPanel.addLayer(new SelectionLayer(cs));
		
		this.featurePanels.add(frequencyDomainPanel);
		return frequencyDomainPanel;
	}

	public void addAudio(String audioFile) {
		final Runnable uiRunnable;
				
		if(referenceFile == null){	
			cs = new CoordinateSystem(AxisUnit.FREQUENCY, 0, 12000);
			//remove the empty feature panel
			referenceFile = new ChromaPrintExtractor(audioFile,null);
			referenceFileLocation = audioFile;
			referenceFile.starExtraction();
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
			// run ui stuff on ui thread.
		
		}else{
			
			ChromaPrintExtractor otherFileInfo = new ChromaPrintExtractor(audioFile,referenceFile);
			this.otherFiles.add(otherFileInfo);
			otherFileInfo.starExtraction();
			
			System.out.println("Hamming distance between ref and file:" + Hamming.d(referenceFile.getHash(), otherFileInfo.getHash()));
			
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
		SwingUtilities.invokeLater(uiRunnable);
			
		
	}	
	
	public static void main(String... args){
		ChromaPrintBrowser b = new ChromaPrintBrowser();
		b.setSize(1024, 768);
		b.setVisible(true);
	}

}
