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
import java.util.HashSet;
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

import be.panako.strategy.cteq.CteQFingerprint;
import be.panako.ui.CteQFingerprintLayer.LandmarkSelectionHandler;
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
import be.tarsos.dsp.util.PitchConverter;




public class CteQFingerprintBrowser extends JFrame implements ViewPortChangedListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4000269621209901229L;	
	
	private final List<LinkedPanel> panels;
	private boolean drawing = false;
	
	private final List<String> audioFiles;
	
	private final List<CteQFingerprintLayer> fingerprintLayerPerPanel;
	
	private final JPanel featurePane;
	private final JTextArea fingerprintInfo;
	

	/**
	 * The default minimum pitch, in absolute cents (+-66 Hz)
	 */
	private int minimumPitch = Config.getInt(Key.CTEQ_MIN_FREQ); //
	/**
	 * The default maximum pitch, in absolute cents (+-4200 Hz)
	 */
	private int maxPitch = Config.getInt(Key.CTEQ_MAX_FREQ);
	/**
	 * The default number of bins per octave.
	 */
	private int binsPerOctave = Config.getInt(Key.CTEQ_BINS_PER_OCTAVE);
	
	/**
	 * The default increment in samples.
	 */
	private int increment = Config.getInt(Key.CTEQ_STEP_SIZE);
	
	public CteQFingerprintBrowser(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Constant Q Landmark Visualizer");
		
		fingerprintLayerPerPanel = new ArrayList<CteQFingerprintLayer>();

		panels = new ArrayList<LinkedPanel>();
		audioFiles = new ArrayList<String>();
		featurePane = new JPanel(new GridLayout(0,1));
		
		JPanel subPanel = new JPanel();
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
		subPanel.add(createConstantQParameterPanel());
		subPanel.add(createButtonPanel());
		JPanel subSubPanel = new JPanel(new BorderLayout());
		subSubPanel.add(subPanel,BorderLayout.NORTH);
		
		fingerprintInfo = new JTextArea();
		fingerprintInfo.setBackground(UIManager.getColor("Panel.background" ));
		fingerprintInfo.setFont(new Font("Monospaced",Font.PLAIN,12));
		JScrollPane pane = new JScrollPane(fingerprintInfo);
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
			minValue = 3500;
			maxValue = 11900;
		}
		return new CoordinateSystem(yUnits, minValue, maxValue);
	}
	
	
	private void resetAudioFiles(){
		List<String> files = new ArrayList<String>(audioFiles);
		audioFiles.clear();
		fingerprintLayerPerPanel.clear();
		initializeFeaturePane();
		for(String file : files){
			addAudio(file);
		}
	}
	
	private void addAudio(final String audioFile){
		
		audioFiles.add(audioFile);
		
		final LinkedPanel newFrequencyDomainPanel;
		LinkedPanel prevFrequencyDomainPanel = panels.get(0);
		CoordinateSystem cs = prevFrequencyDomainPanel.getCoordinateSystem();
		newFrequencyDomainPanel = new LinkedPanel(cs);
		newFrequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		newFrequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		newFrequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		LandmarkSelectionHandler handler = new LandmarkSelectionHandler() {
			
			@Override
			public void selected(List<CteQFingerprint> fingerprints) {
				if(fingerprints.isEmpty())
					return;
				
				String fileName = new File(audioFile).getName();
				String explanation= fileName + "\n";
				for(CteQFingerprint fingerprint: fingerprints){
					explanation += fingerprint.explainHash();
				}
				String current = fingerprintInfo.getText();
				if(!current.startsWith(explanation)){
					fingerprintInfo.setText(explanation + "\n" + current);
				}
				fingerprintInfo.setCaretPosition(0);
				
			}
		};
		LayerFinishedHandler finishedHandler = new LayerFinishedHandler() {
			@Override
			public void LayerFinished(CteQFingerprintLayer layer) {
				newFrequencyDomainPanel.revalidate();
				newFrequencyDomainPanel.repaint();
				fingerprintLayerPerPanel.add(layer);
				
				if(fingerprintLayerPerPanel.size() > 0){
					for(int i = 1; i < fingerprintLayerPerPanel.size();i++){
						HashSet<CteQFingerprint> intersection =  new HashSet<CteQFingerprint>(fingerprintLayerPerPanel.get(0).getLandmarkTrios());
						intersection.retainAll(fingerprintLayerPerPanel.get(i).getLandmarkTrios());
						fingerprintLayerPerPanel.get(i).setComparableTrios(new ArrayList<CteQFingerprint>(intersection));
					}
				}
			}
		};
		newFrequencyDomainPanel.addLayer(new CteQFingerprintLayer(cs, new File(audioFile), increment, minimumPitch, maxPitch, binsPerOctave,handler,finishedHandler));
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
		parameterPanel.setBorder(new TitledBorder("Constant-Q Algorithm Parameters"));
		
		
		final JLabel label = new JLabel(String.format("Mimum pitch : %.2f (Midi units) %.2f (Hz)",minimumPitch/100.0,PitchConverter.absoluteCentToHertz(minimumPitch)));
		final JSlider minFrequency = new JSlider(2400,12800);
		minFrequency.setValue(minimumPitch);
		minFrequency.setPaintLabels(false);
		minFrequency.setPaintTicks(false);
		minFrequency.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int newPitch = minFrequency.getValue();
				label.setText(String.format("Mimum pitch : %.2f (Midi units) %.2f (Hz)",newPitch/100.0,PitchConverter.absoluteCentToHertz(newPitch)));
				if(!minFrequency.getValueIsAdjusting()){
					minimumPitch = newPitch;
					resetAudioFiles();
				}
			}
		});
		JPanel subPanel;
		subPanel = new JPanel(new BorderLayout());
		subPanel.add(label,BorderLayout.NORTH);
		subPanel.add(minFrequency,BorderLayout.CENTER);
		parameterPanel.add(subPanel);
		
	
		final JLabel maxLabel = new JLabel(String.format("Maximum pitch : %.2f (Midi units) %.2f (Hz)",maxPitch/100.0,PitchConverter.absoluteCentToHertz(maxPitch)));
		final JSlider maxFrequency = new JSlider(2400,12800);
		maxFrequency.setValue(maxPitch);
		maxFrequency.setPaintLabels(false);
		maxFrequency.setPaintTicks(false);
		maxFrequency.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int newPitch = maxFrequency.getValue();
				maxLabel.setText(String.format("Maximum pitch : %.2f (Midi units) %.2f (Hz)",newPitch/100.0,PitchConverter.absoluteCentToHertz(newPitch)));
				if(!maxFrequency.getValueIsAdjusting()){
					maxPitch = newPitch;
					resetAudioFiles();
				}
			}
		});
		
		subPanel = new JPanel(new BorderLayout());
		subPanel.add(maxLabel,BorderLayout.NORTH);
		subPanel.add(maxFrequency,BorderLayout.CENTER);
		parameterPanel.add(subPanel);
	
		final JLabel binsLabel = new JLabel(String.format("Number of bins per octave: %d ",binsPerOctave));
		final JSlider binsPerOctaveSlider = new JSlider(2,256);
		binsPerOctaveSlider.setValue(binsPerOctave);
		binsPerOctaveSlider.setPaintLabels(false);
		binsPerOctaveSlider.setPaintTicks(false);
		binsPerOctaveSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int newBins = binsPerOctaveSlider.getValue();
				binsLabel.setText(String.format("Number of bins per octave: %d ",newBins));
				if(!binsPerOctaveSlider.getValueIsAdjusting()){
					binsPerOctave = newBins;
					resetAudioFiles();
				}
			}
		});
		
		subPanel = new JPanel(new BorderLayout());
		subPanel.add(binsLabel,BorderLayout.NORTH);
		subPanel.add(binsPerOctaveSlider,BorderLayout.CENTER);
		parameterPanel.add(subPanel);
		
		
		final JLabel incrementLabel = new JLabel(String.format("Increment, in frames: %d ",increment));
		final JSlider incrementSlider = new JSlider(128,4096*4);
		incrementSlider.setValue(increment);
		incrementSlider.setPaintLabels(false);
		incrementSlider.setPaintTicks(false);
		incrementSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				int newIncrement = incrementSlider.getValue();
				incrementLabel.setText(String.format(String.format("Increment, in frames: %d ",newIncrement)));
				if(!incrementSlider.getValueIsAdjusting()){
					increment = newIncrement;
					resetAudioFiles();
				}
			}
		});
		
		subPanel = new JPanel(new BorderLayout());
		subPanel.add(incrementLabel,BorderLayout.NORTH);
		subPanel.add(incrementSlider,BorderLayout.CENTER);
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
				int returnVal = fileChooser.showOpenDialog(CteQFingerprintBrowser.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                String audioFile = file.getAbsolutePath();
	                setTitle("Fingerprints for: " + file.getName());
	                addAudio(audioFile);
	            } else {
	                //canceled
	            }
			}			
		});
		
		
			
		final JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				audioFiles.clear();
				panels.clear();
				fingerprintLayerPerPanel.clear();
				featurePane.removeAll();
				featurePane.revalidate();
				featurePane.repaint();
				resetAudioFiles();
				CteQFingerprintBrowser.this.repaint();
				System.out.println("clear");
			}});
		
		final JButton recalculateButton = new JButton("Recalculate");
		recalculateButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				resetAudioFiles();
			}});
		fileChooserPanel.add(clearButton);
		fileChooserPanel.add(recalculateButton);
		fileChooserPanel.add(chooseFileButton);
		
		
		
		return fileChooserPanel;
	}
}
