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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;

public class FFTFingerprintBrowser extends JFrame implements ViewPortChangedListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4000269621209901229L;	
	
	private final List<LinkedPanel> panels;
	private boolean drawing = false;
	
	private final List<String> audioFiles;
	
	private final JTextArea landmarkInfo;
	private final JPanel featurePane;
	

	/**
	 * The number of landmarks Per Second.
	 */
	private int fingerprintsPerSecond = Config.getInt(Key.FFT_FINGERPRINTS_PER_SECOND_FOR_STORAGE);
	
	public FFTFingerprintBrowser(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("FFT Fingerprint Visualizer");
		

		panels = new ArrayList<LinkedPanel>();
		audioFiles = new ArrayList<String>();
		featurePane = new JPanel(new GridLayout(0,1));
		
		JPanel subPanel = new JPanel();
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
		subPanel.add(createConstantQParameterPanel());
		subPanel.add(createButtonPanel());
		JPanel subSubPanel = new JPanel(new BorderLayout());
		subSubPanel.add(subPanel,BorderLayout.NORTH);
		
		landmarkInfo = new JTextArea();
		landmarkInfo.setBackground(UIManager.getColor("Panel.background" ));
		landmarkInfo.setFont(new Font("Monospaced",Font.PLAIN,12));
		JScrollPane pane = new JScrollPane(landmarkInfo);
		pane.setBorder(new EmptyBorder(5, 5, 5, 5));
		subSubPanel.add(pane,BorderLayout.CENTER);

		initializeFeaturePane();
		this.add(subSubPanel,BorderLayout.WEST);
		this.add(featurePane,BorderLayout.CENTER);
	}
	
	private void initializeFeaturePane(){
		panels.clear();
		CoordinateSystem cs;
		cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		LinkedPanel emptyFrequencySpectrum = new LinkedPanel(cs);
		emptyFrequencySpectrum.addLayer(new ZoomMouseListenerLayer());
		emptyFrequencySpectrum.addLayer(new DragMouseListenerLayer(cs));
		emptyFrequencySpectrum.addLayer(new BackgroundLayer(cs));
		emptyFrequencySpectrum.addLayer(new FrequencyAxisLayer(cs));
		emptyFrequencySpectrum.addLayer(new TimeAxisLayer(cs));
		emptyFrequencySpectrum.getViewPort().addViewPortChangedListener(this);
		panels.add(emptyFrequencySpectrum);
		featurePane.add(emptyFrequencySpectrum);
	}
	
	public void viewPortChanged(ViewPort newViewPort) {
		if (!drawing) {
			drawing = true;
			for (LinkedPanel panel : panels) {
				panel.repaint();
			}
			drawing = false;
		}
	}

	private CoordinateSystem getCoordinateSystem(AxisUnit yUnits) {
		float minValue = -1000;
		float maxValue = 1000;
		if(yUnits == AxisUnit.FREQUENCY){
			minValue = 200;
			maxValue = 8000;
		}
		return new CoordinateSystem(yUnits, minValue, maxValue);
	}
	
	
	private void resetAudioFiles(){
		List<String> files = new ArrayList<String>(audioFiles);
		audioFiles.clear();
		initializeFeaturePane();
		for(String file : files){
			addAudio(file);
		}
	}
	
	private void addAudio(final String audioFile){
		
		audioFiles.add(audioFile);
		
		LinkedPanel prevFrequencyDomainPanel = (LinkedPanel) featurePane.getComponent(0);

		CoordinateSystem cs = prevFrequencyDomainPanel.getCoordinateSystem();
		final LinkedPanel newFrequencyDomainPanel;
		newFrequencyDomainPanel = new LinkedPanel(cs);
		newFrequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		newFrequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		newFrequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		newFrequencyDomainPanel.addLayer(new FFTFingerprintLayer(cs, new File(audioFile),fingerprintsPerSecond,new LayerFinishedHandler() {
			
			@Override
			public void LayerFinished(CteQFingerprintLayer layer) {
				newFrequencyDomainPanel.revalidate();
				newFrequencyDomainPanel.repaint();
			}
		}));
		newFrequencyDomainPanel.addLayer(new FrequencyAxisLayer(cs));
		newFrequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		newFrequencyDomainPanel.addLayer(new SelectionLayer(cs));
		LegendLayer l = new LegendLayer(cs, 300);
		l.addEntry(new File(audioFile).getName(), Color.BLUE);
		newFrequencyDomainPanel.addLayer(l);
		newFrequencyDomainPanel.getViewPort().addViewPortChangedListener(this);
		panels.add(newFrequencyDomainPanel);
		
		if(audioFiles.size() == 1){
			panels.remove(0);
		}
		featurePane.removeAll();
		for(LinkedPanel panel : panels){
			featurePane.add(panel);
		}
		featurePane.revalidate();
		featurePane.repaint();
	}
	
	private JComponent createConstantQParameterPanel(){
		JPanel parameterPanel = new JPanel(new GridLayout(0, 1));
		parameterPanel.setBorder(new TitledBorder("FFT Algorithm Parameters"));
		
		
		final JLabel label = new JLabel(String.format("Fingerprints Per Second : %d (Hz)",fingerprintsPerSecond));
		final JSlider fingerprintsPerSecondSlider = new JSlider(2,20);
		fingerprintsPerSecondSlider.setValue(fingerprintsPerSecond);
		fingerprintsPerSecondSlider.setPaintLabels(false);
		fingerprintsPerSecondSlider.setPaintTicks(false);
		fingerprintsPerSecondSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int newFingerprintsPerSecond = fingerprintsPerSecondSlider.getValue();
				label.setText(String.format("Landmarks Per Second : %d (Hz)",newFingerprintsPerSecond));
				if(!fingerprintsPerSecondSlider.getValueIsAdjusting()){
					fingerprintsPerSecond = newFingerprintsPerSecond;
					resetAudioFiles();
				}
			}
		});
		JPanel subPanel;		
		subPanel = new JPanel(new BorderLayout());
		subPanel.add(label,BorderLayout.NORTH);
		subPanel.add(fingerprintsPerSecondSlider,BorderLayout.CENTER);
		parameterPanel.add(subPanel);
	
		return parameterPanel;	
	}
	
	private JComponent createButtonPanel(){
		JPanel fileChooserPanel = new JPanel(new GridLayout(1,0));
		fileChooserPanel.setBorder(new TitledBorder("Actions"));
		
	   final JFileChooser fileChooser = new JFileChooser();
		
		final JButton chooseFileButton = new JButton("Open...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(FFTFingerprintBrowser.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                String audioFile = file.getAbsolutePath();
	                setTitle("Landmarks for: " + file.getName());
	                addAudio(audioFile);
	            } else {
	                //canceled
	            }
			}			
		});
		
		
		
		final JButton recalculateFileButton = new JButton("Recalculate");
		recalculateFileButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				resetAudioFiles();
			}});
		fileChooserPanel.add(recalculateFileButton);
		fileChooserPanel.add(chooseFileButton);
		
		return fileChooserPanel;
	}

}
