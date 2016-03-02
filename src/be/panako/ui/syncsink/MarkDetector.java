package be.panako.ui.syncsink;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

final class MarkDetector implements AudioProcessor {
	double[] dbVals = new double[5];
	List<Double> changeEvents = new ArrayList<Double>();
	int frameCounter = 0;
	
	private final double dbDelta;//the loudness change (in dB) to look for
	private final double markerDelta;//the exact time between loudness changes (in ms)
	private final double minDiff;//the max time allowed between loudness changes (in ms)
	private final double maxDiff;//the min time allowed between loudness changes (in ms)
	private final double minDistance;//minimum distance between markers
	private final List<Double> markers;//a list of marker positions (in seconds)
	
	
	public MarkDetector(double markerDelta, double errorMargin, double dbDelta){
		this.markerDelta = markerDelta;
		this.minDistance = markerDelta;
		
		this.dbDelta = dbDelta;
		
		this.minDiff = markerDelta-errorMargin;
		this.maxDiff = markerDelta+errorMargin;
		
		markers= new ArrayList<Double>();
	}

	public void processingFinished() {
		double prevMarker=0;
		for(int i = 0 ; i < changeEvents.size() ; i++){
			for(int j = i+1 ; j < changeEvents.size() ; j++){
				double diffIJ =  (changeEvents.get(j) - changeEvents.get(i))*1000;
				if(diffIJ > maxDiff){
					j = changeEvents.size();
				} else if(diffIJ > minDiff &&  diffIJ < maxDiff){
					for(int k = j+1 ; k < changeEvents.size() ; k++){
						double diffJk = (changeEvents.get(k) - changeEvents.get(j))*1000;
						if(diffJk > maxDiff){
							k = changeEvents.size();
						}else if(diffJk > minDiff && diffJk < maxDiff){
							if((changeEvents.get(i)-prevMarker) * 1000 > minDistance){
								markers.add(changeEvents.get(i));
								prevMarker= changeEvents.get(i);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @return a copy of the found markers (in seconds)
	 */
	public List<Double> getMarkers(){
		return new ArrayList<Double>(markers);
	}

	public boolean process(AudioEvent audioEvent) {
		double currentDBVal = audioEvent.getdBSPL();
		
		for(int i = 1 ; i <dbVals.length;i++){
			dbVals[i-1]=dbVals[i];
		}
		dbVals[4]=currentDBVal;
		double delta = Math.abs(dbVals[0]-dbVals[4]);
		if(delta>dbDelta && frameCounter > 5){
			changeEvents.add(audioEvent.getTimeStamp());
		}
		frameCounter++;
		return true;
	}
}