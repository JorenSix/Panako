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



package be.panako.strategy.cteq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.PitchUnit;
import be.panako.util.StopWatch;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;

/**
 * Extracts fingerprints from the spectrum and connects them.
 * 
 * @author Joren Six
 *
 */
public class CteQFingerprintProcessor implements AudioProcessor {
	
	private final static Logger LOG = Logger.getLogger(CteQFingerprintProcessor.class.getName());
	
	private final ConstantQ constantQ;

	private ArrayList<float[]> spectralInfo;
	
	private final ArrayList<CteQEventPoint> eventPoints;
	
	private final Set<CteQFingerprint> fingerprints;
	
	//How close can event points be to each other?
	private double deltaT = 0.8;//seconds
	private double deltaF = 500;//cents (1 maximum each third of an octave)
	private int deltaTInSteps;
	private int deltaFInBins;
	
	/**
	 * The factors influencing the number of fingerprints returned are: This
	 * defines the width of the gaussian used in the kernel which makes a
	 * gaussian mixture of found peaks. A larger value results in fewer peaks.
	 */
	int spreadingWidth = 30;// frequency bins

	/**
	 * The decay rate defines how much the threshold lowers each frame. A large
	 * value, closer to one, results in fewer peaks
	 */
	float decayRate = 0.99f;
					
	//How far can a pair of event points be separated
	private double maxEventPointDeltaT = 2.0;// seconds
	private double maxEventPointDeltaF = Config.getInt(Key.CTEQ_EVENT_POINT_FREQUENCY_DELTA_MAX);// cents
	private int maxEventPointDeltaTInSteps ;
	private int maxEventPointDeltaFInBins;
	
	//how much analysis frames fit in one second? E.g. for 44100Hz and 1024: 
	private final int timeStepsPerSecond;
	
	//Keep only x eventPoints each second.
	private final int eventPointsPerSecond;
	
	//Limits the number of fingerprints constructed with each event point
	private final int maxFingerprintsPerEventPoint;
			
	//if this is true, the original spectrogram is copied and kept intact, e.g. for visualization.
	//set to false when processing datasets.
	private boolean keepOriginalSpectrogram;
	
	/**
	 * The default increment in samples.
	 */
	 private final int hopSize;//1024+512/44.1kHz => each 34.8ms 
	 
	 private final int sampleRate = Config.getInt(Key.CTEQ_SAMPLE_RATE);
	 
	 /**
	 * Contains the kernel used to convolve with in spread.
	 */
	final float[] kernel;
	 
	 public CteQFingerprintProcessor(int eventPointsPerSecond,int branchingFactor){
		 this(eventPointsPerSecond,branchingFactor,false);
	 }
	public CteQFingerprintProcessor(int eventPointsPerSecond,int branchingFactor,boolean keepOriginalSpectrogram){
		this(eventPointsPerSecond,branchingFactor,keepOriginalSpectrogram,Config.getInt(Key.CTEQ_STEP_SIZE),Config.getInt(Key.CTEQ_BINS_PER_OCTAVE),Config.getInt(Key.CTEQ_MIN_FREQ),Config.getInt(Key.CTEQ_MAX_FREQ));
	}
	
	public CteQFingerprintProcessor(int eventPointsPerSecond,int branchingFactor,boolean keepOriginalSpectrogram,int hopsize, int binsPerOctave,float  minFreqInCents, float maxFreqInCents){
		this.maxFingerprintsPerEventPoint = branchingFactor;
		this.hopSize = hopsize;
		float minFreqInHerz = (float)  PitchUnit.HERTZ.convert(minFreqInCents,PitchUnit.ABSOLUTE_CENTS);
		float maxFreqInHertz = (float) PitchUnit.HERTZ.convert(maxFreqInCents,PitchUnit.ABSOLUTE_CENTS);
			
		constantQ = new ConstantQ(sampleRate, minFreqInHerz, maxFreqInHertz, binsPerOctave);
		spectralInfo =  new ArrayList<float[]>();
		eventPoints = new ArrayList<CteQEventPoint>();
		
		//initialize the spreading kernel.
		int w = 4 * spreadingWidth;
		kernel = new float[w*2+1];
		for(int i = -w ; i <= w;i++){
			float t = i/(float)spreadingWidth;
			kernel[i+w] = (float)Math.exp(-0.5 * t * t);			
		}
	
		fingerprints = new HashSet<CteQFingerprint>();
	
		timeStepsPerSecond = (int) (sampleRate/hopSize);
		
		deltaTInSteps = (int) (deltaT * sampleRate / hopSize);
		deltaFInBins = (int) (constantQ.getBinsPerOctave() * deltaF/1200.0);
		
		maxEventPointDeltaTInSteps = (int) (maxEventPointDeltaT * sampleRate / hopSize);
		maxEventPointDeltaFInBins = (int) (constantQ.getBinsPerOctave() * maxEventPointDeltaF/1200.0);
		
		this.keepOriginalSpectrogram = keepOriginalSpectrogram;
		this.eventPointsPerSecond = eventPointsPerSecond;
	}

	
	@Override
	public boolean process(AudioEvent audioEvent) {
		constantQ.process(audioEvent);
		spectralInfo.add(constantQ.getMagnitudes().clone());
		return true;
	}

	@Override
	public void processingFinished() {
		StopWatch w = new StopWatch();
		
		//Spectrogram cleanup is inspired by  
		//  Dan Ellis's Robust Landmark-Based Audio Fingerprinting Matlab Implementation
		//  http://labrosa.ee.columbia.edu/matlab/fingerprint/
		//Replace zero values with a very small value to
		//prevent log(0)
		removeZeroValues();
		
		//Calculate the natural logarithm for each value, 
		//to make energy differences more like human
		//loudness perception.
		log();
		
		// calculate the mean of all values
		float mean = mean();
		
		// make it zero mean, so the start-up transients for the filter are
		// minimized
		add(-mean);
		
		//filter for LFO's
		filter(-0.98f);
		
		
		boolean useThresholding = true;
				
		if(useThresholding){
			//Find event points via thresholding
		    // The algorithm to find event points via thresholding is inspired by  
			// Dan Ellis's Robust Landmark-Based Audio Fingerprinting Matlab Implementation
			//  http://labrosa.ee.columbia.edu/matlab/fingerprint/
			findEventPointsViaThresholding();	
		}else{
			//Find all event points (tile based algorithm)
			
			findEventPointsViaTiling();
		}
		
		//Keep only a limited amount of event points
		filterEventPoints();
		
		//refine event point locations by blurring
		refineEventPointLocations();
		
		//discard spectrogram
		if(!keepOriginalSpectrogram)
			spectralInfo.clear();
		
		//Combine event points into fingerprints
	
		
		packEventPointsIntoFingerprints();
		
		LOG.fine(String.format("Extracted fingerprints from spectrogram in %s",w.formattedToString()));
	}

	
	
	public int getFFTlength(){
		return constantQ.getFFTlength();
	}
	
	public ConstantQ getProcessor(){
		return constantQ;
	}
	
	private void refineEventPointLocations() {
		for(CteQEventPoint eventPoint : eventPoints){
			//Now we have the local maximums in spectralInfo, but to handle bin boundaries well,
			// we need to look at the surrounding bins. If they contain a lot of energy too, then
			// we should take the average of the surrounding bins and relocate the to where
			// a new maximum appears. For this we use a simple box blur of the center pixel and some 
			// surrounding pixels. The offsets indicate how much time and frequency bin index change
			int[] offsets = refineLocation(eventPoint.t,eventPoint.f);
			eventPoint.t = eventPoint.t + offsets[0];
			eventPoint.f = eventPoint.f + offsets[1];
			
		}
	}

	private void findEventPointsViaTiling() {
		List<float[]> spectralInfo; 
		
		//copy the original spectrum
		spectralInfo = new ArrayList<float[]>();
		for(float[] spectrum:this.spectralInfo){
			spectralInfo.add(spectrum.clone());
		}
		
		
		//Mark the local maximums. 
		//Sets everything else to zero
		for(float[] spectrum:spectralInfo){
			localMaximum(spectrum);
		}
	
		//Find peaks within a local neighborhood. For every local maximum (t,f) in the spectrogram
		//Points in the region from (t-delta t,f - delta f) to (t+delta t, f + delta f) are compared and the maximum is kept.
		//TODO: include an absolute threshold to avoid very low energy event points
		for(int i = 0 ; i < spectralInfo.size() ; i++){
			for(int j = 0 ; j < spectralInfo.get(i).length; j ++){
				if(spectralInfo.get(i)[j] != 0){
					//look in the neighborhood and try to set as many fields as possible to 0
					//to avoid extra comparisons
					int minTimeIndex = Math.max(0, i-deltaTInSteps/2);
					int maxTimeIndex = Math.min(spectralInfo.size(), i+deltaTInSteps/2);
					int minPitchIndex = Math.max(0, j-deltaFInBins/2);
					int maxPitchIndex = Math.min(spectralInfo.get(i).length, j+deltaFInBins/2);
					
					int tileMaxTimeIndex = i;
					int tileMaxPitchIndex = j;
					float tileMaxValue  = spectralInfo.get(i)[j];
					for(int timeIndex = minTimeIndex ; timeIndex < maxTimeIndex ; timeIndex++){
						for(int pitchIndex = minPitchIndex ; pitchIndex < maxPitchIndex; pitchIndex++){
							float value = spectralInfo.get(timeIndex)[pitchIndex];
							if(value > tileMaxValue ){
								tileMaxValue = value;
								spectralInfo.get(tileMaxTimeIndex)[tileMaxPitchIndex]=0;
								tileMaxPitchIndex = pitchIndex;
								tileMaxTimeIndex = timeIndex;
							} else if(value != 0 && value !=tileMaxValue){
								spectralInfo.get(timeIndex)[pitchIndex]=0;
							}
						}
					}
				}
			}
		}
		
			
		//Currently the spectrogram only contains points that are maximum in their neighborhood.
		//These are the event points that need to be paired to form a hash of sorts.
		for(int i = 0 ; i < spectralInfo.size() ; i++){
			for(int j = 0 ; j < spectralInfo.get(i).length; j ++){
				if(spectralInfo.get(i)[j] != 0){
					float contrast = getContrast(i,j);
					eventPoints.add(new CteQEventPoint(i,j,spectralInfo.get(i)[j],contrast));
				}
			}
		}
	}
	
	public void findEventPointsViaThresholding(){
		//the number of bins in the spectrum
		int numberOfBins = spectralInfo.get(0).length;
		
		int maxEventPoints = (int) (spectralInfo.size() * 1536 * eventPointsPerSecond/44100 * 3.5);
		
		ArrayList<CteQEventPoint> foundEventPoints = new ArrayList<CteQEventPoint>();
		int maxEventPointsPerAnalysisFrame = 3;
		
		//Stores the energy thresholds to filter for event points.
		float[] energyThresholds = new float[numberOfBins];
		
		//Initialize the thresholds using the first ten (+-300ms) frames
		for(int i = 0 ; i < 40 ; i ++){
			for(int j = 0 ; j < numberOfBins ; j++){
				energyThresholds[j] = Math.max(spectralInfo.get(i)[j],energyThresholds[j]);
			}
		}
		
		//Replace the points where the energy is at its maximum
		//with a gaussian mixture.
		energyThresholds = spread(energyThresholds);
		
		for(int i = 0; i < spectralInfo.size() - 1 ; i++){
			float[] energyCurrentFrame = spectralInfo.get(i);
			float[] energyDiffCurrentAndThreshold = new float[numberOfBins];
			for(int j = 0 ; j < numberOfBins ; j++){
				energyDiffCurrentAndThreshold[j] =  Math.max(0, energyCurrentFrame[j]-energyThresholds[j]);
			}
			//find local maxima in the differences between the energy in the 
			//current frame and the threshold.
			localMaximum(energyDiffCurrentAndThreshold);
			Integer[] indexes = indexesSort(energyDiffCurrentAndThreshold);
			
			int eventPointsFoundInAnalysisFrame = 0;
			for(int j = 0 ; j < energyDiffCurrentAndThreshold.length ; j ++){
				int originalIndex = indexes[j];
				//Compare peak with threshold
				if(foundEventPoints.size() < maxEventPoints &&
						energyDiffCurrentAndThreshold[originalIndex] > 0 
						&& eventPointsFoundInAnalysisFrame < maxEventPointsPerAnalysisFrame 
						&& energyCurrentFrame[originalIndex] > energyThresholds[originalIndex] ){
					
					foundEventPoints.add(new CteQEventPoint(i,originalIndex,spectralInfo.get(i)[originalIndex],0));
					eventPointsFoundInAnalysisFrame++;
					
					for(int k = 0; k < energyThresholds.length ; k++){
						float val = (k - originalIndex)/((float)spreadingWidth);
						float eew = (float) Math.exp(-0.5 * val * val);
						energyThresholds[k] = Math.max(energyThresholds[k],energyCurrentFrame[originalIndex]*eew);
					}
				}
			}
			for(int k = 0; k < energyThresholds.length ; k++){
				energyThresholds[k] = decayRate * energyThresholds[k];
			}
		}
		
		float[] energyLastFrame = spectralInfo.get(spectralInfo.size()-1).clone();
		energyThresholds = spread(energyLastFrame);
		
		int eventPointIndex = foundEventPoints.size()-1;
		List<CteQEventPoint> actuallyFoundEventPoints = new ArrayList<CteQEventPoint>();
		for(int i = spectralInfo.size() -1;i >=0; i--){
			while(eventPointIndex >=0 && foundEventPoints.get(eventPointIndex).t == i){
				float f = foundEventPoints.get(eventPointIndex).f;
				float energyValue = spectralInfo.get(i)[(int)f];
				if(energyValue >= energyThresholds[(int)f]){
					CteQEventPoint originalEventPoint = foundEventPoints.get(eventPointIndex);
					CteQEventPoint newEventPoint = new CteQEventPoint(originalEventPoint.t, originalEventPoint.f, originalEventPoint.energy, getContrast(originalEventPoint.t, originalEventPoint.f));
					actuallyFoundEventPoints.add(newEventPoint);
					
					for(int k = 0; k < energyThresholds.length ; k++){
						float val = (k - f)/((float)spreadingWidth);
						float eew = (float) Math.exp(-0.5 * val * val);
						energyThresholds[k] = Math.max(energyThresholds[k],energyValue * eew);
					}
				}
				eventPointIndex --;
			}
			for(int k = 0; k < energyThresholds.length ; k++){
				energyThresholds[k] = decayRate * energyThresholds[k];
			}
		}
		
		
		
		this.eventPoints.addAll(actuallyFoundEventPoints);
		sortEventPoints(this.eventPoints);
	}
	
	private void sortEventPoints(List<CteQEventPoint> eventPoints){
		Collections.sort(eventPoints, new Comparator<CteQEventPoint>(){
			@Override
			public int compare(CteQEventPoint o1, CteQEventPoint o2) {
				Integer t1 = o1.t;
				Integer t2 = o2.t;
				if(t1==t2){
					Integer f1 = o1.f;
					Integer f2 = o2.f;
					return f1.compareTo(f2);
				}else{
					return t1.compareTo(t2);
				}
			}});
	}
	
	private Integer[] indexesSort(final float[] arrayToSort){
		Integer[] indexes = new Integer[arrayToSort.length];
		for(int i = 0 ; i < arrayToSort.length;i++){
			indexes[i] = i;
		}
		Arrays.sort(indexes,new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				float one = arrayToSort[o2];
				float other = arrayToSort[o1];
				int value = 1;
				if(one == other){
					value = 0;
				} else if (one < other){
					value = -1;
				}
				return value;
			}
		});
		return indexes;
	}
	
	private float getContrast(int i,int j){
		float contrast = 0.0f;
		float eventPointEnergy = spectralInfo.get(i)[j];
		int neighbours = 3;
		int count = 0;
		for(int row = i - neighbours; row <= i + neighbours ; row ++){
			int actualRow = row; 
			if(actualRow < 0){
				actualRow = 0;
			}
			if(actualRow >= spectralInfo.size()){
				actualRow = spectralInfo.size()-1;
			}
			for(int col = j - neighbours ; col <= j + neighbours ; col ++){
				int actualCol = col;
				if(actualCol < 0){
					actualCol = 0;
				}
				if(actualCol >= spectralInfo.get(actualRow).length){
					actualCol = spectralInfo.get(actualRow).length-1;
				}
				if(!(actualRow == i && actualCol == j)){
					float neighborEnergy = spectralInfo.get(actualRow)[actualCol];
					contrast += Math.abs(eventPointEnergy-neighborEnergy);
					count++;
				}
			}
		}
		return contrast/(float)count +  2 * eventPointEnergy;
	}
	
	private int[] refineLocation(int i, int j) {
		//take a 3x3 kernel and use the average as a value (1/9=0.111).
		float kernel[][] = {
		        {2,  5, 2}, 
		        {5, 15, 5}, 
		        {2,  5, 2}, 
		};
	
		
		int neighbourhoudSize=7;// size of the neighbors to blur
		int halfNeighbourhoodSize = neighbourhoudSize/2;
		
		float sigma = 1.0f;
		int kernelWidth = 5;
		
		//create a gaussian kernel
		//check in octave with:
		// m = 5 ; n = 5 ; sigma = 1
		// [h1, h2] = meshgrid(-(m-1)/2:(m-1)/2, -(n-1)/2:(n-1)/2);
		// hg = exp(- (h1.^2+h2.^2) / (2*sigma^2));
		// h = hg ./ sum(hg(:));
		// h
		kernel = new float[kernelWidth][kernelWidth];
		int halfKernelRowLength = kernel.length/2;
		int halfKernelColLength = kernel[0].length/2;
		for (int x = 0; x < kernelWidth; ++x){ 
			 for (int y = 0; y < kernelWidth; ++y){
				 kernel[x][y] = (float) (Math.exp( -0.5 * ( Math.pow((x-halfKernelRowLength)/sigma, 2.0) + Math.pow((y-halfKernelColLength)/sigma, 2.0))) / (2*Math.PI * sigma * sigma)); 
			 }
		}
				
		
		
		float[][] result = new float[neighbourhoudSize][neighbourhoudSize];
		
		for(int row = 0; row < result.length; row++){
			for(int col = 0; col < result[row].length; col++){
				for(int kernelRow = 0; kernelRow < kernel.length;kernelRow++){
					//the actual row in the spectrum
					int actualRowIndex = row + i - halfNeighbourhoodSize - halfKernelRowLength + kernelRow;
					if(actualRowIndex < 0){
						actualRowIndex = 0;
					}
					if(actualRowIndex >= spectralInfo.size()){
						actualRowIndex = spectralInfo.size()-1;
					}
					for(int kernelCol = 0; kernelCol < kernel[kernelRow].length;kernelCol++){
						int actualColIndex = col + j - halfNeighbourhoodSize - halfKernelColLength + kernelCol;
						if(actualColIndex < 0){
							actualColIndex = 0;
						}
						if(actualColIndex >= spectralInfo.get(actualRowIndex).length){
							actualColIndex = spectralInfo.get(actualRowIndex).length-1;
						}
						float factor = kernel[kernelRow][kernelCol];
						result[row][col] = result[row][col] + factor * spectralInfo.get(actualRowIndex)[actualColIndex];
					}
				}
			}	
		}
		
		//find the maximum in the new result
		int maxRow=-1;
		int maxCol=-1;
		float maxValue = -10000;
		for(int row = 0; row < result.length; row++){
			for(int col = 0; col < result[row].length; col++){
				if(result[row][col] > maxValue){
					maxValue = result[row][col];
					maxRow = row;
					maxCol = col;
				}
			}
		}
		
		//System.out.println(String.format("expected (%d,%d) actual (%d,%d)",halfNeighbourhoodSize,halfNeighbourhoodSize,maxRow,maxCol));
		
		int[] offsets = {maxRow - halfNeighbourhoodSize,maxCol - halfNeighbourhoodSize};
		return offsets;
	}

	private void filterEventPoints() {
		//only max 3 event points for an analysis frame,
		//keep the ones with the highest contrast.
		int maxEventPointsPerAnalysisFrame = 3;
		int removedEventPoints = 0;
		int originalSize = eventPoints.size();
		Hashtable<Integer,TreeMap<Float,CteQEventPoint>> eventPointsPerAnalysisFrame = new Hashtable<Integer,TreeMap<Float,CteQEventPoint>>(); 
		for(int i = 0 ; i < eventPoints.size() ; i++){
			int frame = eventPoints.get(i).t;
			if(!eventPointsPerAnalysisFrame.containsKey(frame)){
				eventPointsPerAnalysisFrame.put(frame, new TreeMap<Float,CteQEventPoint>());
			}
			eventPointsPerAnalysisFrame.get(frame).put(eventPoints.get(i).contrast, eventPoints.get(i));
		}
		for(Integer frame: eventPointsPerAnalysisFrame.keySet()){
			TreeMap<Float,CteQEventPoint> ladmarksForFrame = eventPointsPerAnalysisFrame.get(frame);
			while(ladmarksForFrame.size()>maxEventPointsPerAnalysisFrame){
				eventPoints.remove(ladmarksForFrame.firstEntry().getValue());
				ladmarksForFrame.remove(ladmarksForFrame.firstKey());
				removedEventPoints++;
			}
		}
		
		//A tree with event points: the one with the lowest contrast with neighbors is at the top 
		TreeMap<Float,CteQEventPoint> timeTree = new TreeMap<Float, CteQEventPoint>();
		for(int i = 0 ; i < eventPoints.size() ; i++){
			timeTree.put(eventPoints.get(i).t + Math.abs(eventPoints.get(i).energy/1000.0f), eventPoints.get(i));
		}
		
		//keep only one event point per tile
		for(int i = 0 ; i > 0 && i < eventPoints.size() ; i++){
			CteQEventPoint current = eventPoints.get(i);
			float fromKey = current.t - deltaTInSteps/2.0f;
			float toKey = current.t + deltaTInSteps/2.0f + 0.0000001f;
			Map<Float,CteQEventPoint> subMap = timeTree.subMap(fromKey, toKey);
			
			if(!subMap.isEmpty()){
				int minPitchIndex = Math.max(0, current.f-deltaFInBins/2);
				int maxPitchIndex = Math.min(spectralInfo.get(i).length, current.f+deltaFInBins/2);
				TreeMap<Float,CteQEventPoint> contrastTree = new TreeMap<Float, CteQEventPoint>();
				contrastTree.put(current.contrast, current);
				for(CteQEventPoint other : subMap.values()){
					if(other.f >= minPitchIndex && other.f <= maxPitchIndex){
						contrastTree.put(other.contrast, other);
					}
				}
				while(contrastTree.size()>1){
					CteQEventPoint eventPointToRemove = contrastTree.firstEntry().getValue();
					timeTree.remove(eventPointToRemove.t + Math.abs(eventPointToRemove.energy/1000.0f));
					eventPoints.remove(eventPointToRemove);
					removedEventPoints++;
					contrastTree.remove(contrastTree.firstKey());
					if(eventPointToRemove.t <= current.t){
						i--;
					}
				}
			}
			LOG.fine(String.format("Removed %d from %d detected peaks.", removedEventPoints,originalSize));
		}
		
		//keep only a limited number of event points per time window
		//A tree with event points: the one with the lowest contrast with neighbors is at the top 
		TreeMap<Float,CteQEventPoint> tree = new TreeMap<Float, CteQEventPoint>();
		float timeWindow = 3;//seconds
		for(int i = 0 ; i < eventPoints.size() ; i++){
			CteQEventPoint eventPoint = eventPoints.get(i);		
			int currentTimeMinimum = (int) (eventPoint.t - timeStepsPerSecond*timeWindow);			
			//remove old event points (more than one second ago) from the tree
			Iterator<Map.Entry<Float, CteQEventPoint>> it = tree.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Float, CteQEventPoint> entry = it.next();
				int time = (int) entry.getValue().t;
				if(time < currentTimeMinimum){
					it.remove();
				}
			}
			
			//put the current event point in the tree
			tree.put(eventPoint.contrast, eventPoint);
			//delete the event point entry with the lowest contrast
			while(tree.size()>eventPointsPerSecond * 3 * timeWindow){
				eventPoints.remove(tree.firstEntry().getValue());
				removedEventPoints++;
				tree.remove(tree.firstKey());
				i--;
			}
		}		

	}
	
	private void packEventPointsIntoFingerprints(){
		int minTimeDifference = 7;//time steps
		//Pack the event points into fingerprints
		for(int i = 0; i < eventPoints.size();i++){
			int t1 = eventPoints.get(i).t;
			int f1 = eventPoints.get(i).f;
			int maxtFirstLevel = t1 + maxEventPointDeltaTInSteps;
			int maxfFirstLevel = f1 + maxEventPointDeltaFInBins;
			int minfFirstLevel = f1 - maxEventPointDeltaFInBins;
			
			//A list of fingerprints Per Event Point, ordered by energy of the combined event points
			TreeMap<Float,CteQFingerprint> fingerprintsPerEventPoint = new TreeMap<Float,CteQFingerprint>();
			
			for(int j = i + 1; j < eventPoints.size()  && eventPoints.get(j).t < maxtFirstLevel;j++){
				int t2 = eventPoints.get(j).t;
				int f2 = eventPoints.get(j).f;
				if(t1 != t2 && t2 > t1 + minTimeDifference && f2 > minfFirstLevel && f2 < maxfFirstLevel){
					int maxtScndLevel = t2 + maxEventPointDeltaTInSteps;
					int maxfScndLevel = f2 + maxEventPointDeltaFInBins;
					int minfScndLevel = f2 - maxEventPointDeltaFInBins;
					for(int k = j + 1; k < eventPoints.size() && eventPoints.get(k).t < maxtScndLevel ;k++){
						int f3 = eventPoints.get(k).f;
						int t3 = eventPoints.get(k).t;
						if(t2 != t3 && t3 > t2 + minTimeDifference && f3 > minfScndLevel && f3 < maxfScndLevel){
							float energy = eventPoints.get(k).contrast + eventPoints.get(j).contrast + eventPoints.get(i).contrast ;
							fingerprintsPerEventPoint.put(energy,new CteQFingerprint(t1, f1, t2, f2, t3, f3));
						}
					}
				}
			}

			
			if(fingerprintsPerEventPoint.size() >= maxFingerprintsPerEventPoint ){
				for(int s = 0 ; s < maxFingerprintsPerEventPoint ; s++){
					Entry<Float, CteQFingerprint> e = fingerprintsPerEventPoint.lastEntry();
					fingerprints.add(e.getValue());
					fingerprintsPerEventPoint.remove(e.getKey());
				}
			}else{
				fingerprints.addAll(fingerprintsPerEventPoint.values());	
			}
		}
	}
	
	
	
	
	/**
	 * Transforms an array so that only the local maxima keep their value.
	 * All other elements are set to zero. The algorithm works in place, so no
	 * new arrays are created.
	 * @param array the vector to check
	 */
	private void localMaximum(float[] array){		
		//stores true if the value at this index is larger or equal to the next, false otherwise.
		boolean currentLargerOrEqualThanNext = array[0]<array[1];
		boolean currentLargerOrEqualThanPrev = true;
		for(int i = 1 ; i < array.length - 1 ; i ++){
			boolean newCurrentLargerOrEqualThanPrev = array[i] >= array[i-1];
			if(!(!currentLargerOrEqualThanNext && currentLargerOrEqualThanPrev)){
				array[i-1]=0;
			}
			currentLargerOrEqualThanPrev = newCurrentLargerOrEqualThanPrev;
			currentLargerOrEqualThanNext = array[i+1] >= array[i];
		}
		if(!(!currentLargerOrEqualThanNext && currentLargerOrEqualThanPrev)){
			array[array.length-2]=0;
		}
		//makes sure the last value is no local max.
		array[array.length-1]=0;			
	}
	
	private void removeZeroValues(){
		float minValue = 5/1000000.0f;
		for(int i = 0 ; i < spectralInfo.size() ; i ++){
			for(int j = 0 ; j < spectralInfo.get(i).length ; j ++){
				if(spectralInfo.get(i)[j] < minValue){
					spectralInfo.get(i)[j] = minValue;
				}
			}
		}
	}
	
	private void log(){
		for(int i = 0 ; i < spectralInfo.size() ; i ++){
			for(int j = 0 ; j < spectralInfo.get(i).length ; j ++){
				spectralInfo.get(i)[j] = (float) Math.log(spectralInfo.get(i)[j]*spectralInfo.get(i)[j]);
			}
		}
	}
	
	private float mean(){
		float sum = 0;
		int count = 0;
		for(int i = 0 ; i < spectralInfo.size() ; i ++){
			for(int j = 0 ; j < spectralInfo.get(i).length ; j ++){
				sum += spectralInfo.get(i)[j];
				count++;
			}
		}
		return sum/(float) count;
	}
	
	private void add(float value){
		for(int i = 0 ; i < spectralInfo.size() ; i ++){
			for(int j = 0 ; j < spectralInfo.get(i).length ; j ++){
				spectralInfo.get(i)[j] = spectralInfo.get(i)[j] + value;
			}
		}
	}
	
	/**
	*  A high pass filter, applied in the log-magnitude
	* domain.  It blocks slowly-varying terms (like Automatic Gain Control), but also 
	* emphasizes onsets.  Placing the pole closer to the unit circle 
	* (i.e. making the -.8 closer to -1) reduces the onset emphasis.
	* 
	* <p>
	* The filter is implemented using the following equation:
	* <pre>
	*          N                   M
	*y(n) = - SUM c(k+1) y(n-k) + SUM d(k+1) x(n-k)  for 0&#60;=n&#60;length(x)
	*         k=1                 k=0
	*
	*a = [1, hpf_pole]
	*b = [1, -1]
	*c = [1/a[0], hpf_pole/a[0]] = a / a[0] = a
	*d = [1/a[0], -1/a[0]] = b / a[0] = b
	*
	*N = |a| - 1 = 2
	*M = |b| - 1 = 2
	*</pre>
	*
	 * @param spectralInfo log
	 * @param hpf_pole
	 */
	private void filter(float hpf_pole) {		
		for(int i = 0 ; i < spectralInfo.get(0).length;i++){
			float prevX = spectralInfo.get(0)[i];
			for(int j = 1 ; j < spectralInfo.size(); j++){
				float filteredSample = -hpf_pole * spectralInfo.get(j-1)[i] + spectralInfo.get(j)[i] + -prevX;
				prevX = spectralInfo.get(j)[i];
				spectralInfo.get(j)[i] = filteredSample;
			}
		}
	}
	
	
	private float[] spread(float[] values){
		localMaximum(values);
		float[] y = new float[values.length];
		int spos = Math.round((kernel.length-1)/2);
		float eeValue = 0;
		for(int i = 0 ; i < values.length ; i++){
			if(values[i]!=0){
				for(int j=0;j<y.length;j++){
					int kernelIndex = i-j+spos;
					if(kernelIndex >= 0 && kernelIndex < kernel.length){
						eeValue = kernel[kernelIndex];
						y[j]=Math.max(y[j],values[i]*eeValue);
					}		
				}
			}
		}
		return y;
	}

	public List<float[]> getSpectralInfo() {
		return spectralInfo;
	}

	public List<CteQEventPoint> getEventPoints() {
		return eventPoints;
	}
	
	public Set<CteQFingerprint> getFingerprints() {
		return fingerprints;
	}
	
	public ConstantQ getConstantQProcessor(){
		return constantQ;
	}

	public int getHopSize() {
		return hopSize;
	}

	/**
	 * Clear the event points, coupled fingerprints and the spectral info. Can be useful to
	 * reuse this object and skip the constant q initialization.
	 */
	public void clear() {
		fingerprints.clear();
		eventPoints.clear();
		spectralInfo.clear();
	}
}
