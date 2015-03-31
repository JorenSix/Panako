package be.panako.strategy.nfft;

import java.util.ArrayList;
import java.util.List;

public class NFFTSyncMatch {
	
	private final List<float[]> matches = new ArrayList<float[]>();
	private final String reference;
	private final String matchingStream;
	
	
	public NFFTSyncMatch(String reference, String matchingStream){
		this.reference = reference;
		this.matchingStream = matchingStream;
	}
	
	
	/**
	 * Add a match to the list.
	 * @param startInReference In seconds.
	 * @param stopInReference In seconds.
	 * @param startInMatchingStream In seconds.
	 * @param stopInMatchinStream In seconds.
	 * @param score The number of matching fingerprints.
	 */
	public void addMatch(float startInReference, float stopInReference, float startInMatchingStream,float stopInMatchinStream,float score){
		float[] match = {startInReference,stopInReference,startInMatchingStream,stopInMatchinStream,score};
		matches.add(match);
	}
	
	/**
	 * returns an array with the following information
	 * {startInReference, float stopInReference, float startInMatchingStream,float stopInMatchinStream,float score}
	 * The score is the number of aligned matching fingerprints.
	 * @param i the match to return
	 * @return Return the match.
	 */
	public float[] getMatch(int i){
		return matches.get(i).clone();
	}
	
	public int getNumberOfMatches(){
		return matches.size();
	}
	
	public String getMatchingStream(){
		return matchingStream;
	}
	
	
	public String getReferenceFileName(){
		return reference;
	}
	
	
	/**
	 * Finds and removes overlapping matches with lower scores.
	 */
	public void removeOverlappingMatchesWithLowerScores(){
		
		List<float[]> matchesToRemove = new ArrayList<float[]>();
		//number of ms for which overlap is still counted as overlap
		double overlapRadius = 0.040;//40 ms
		for(int i = 0 ; i < matches.size() ; i++){
			double startInMatch = matches.get(i)[2];
			double stopInMatch = matches.get(i)[3];
			for(int j = i+1 ; j < matches.size() ; j++){
				double otherStartInMatch = matches.get(j)[2];
				double otherStopInMatch = matches.get(j)[3];
				boolean overlaps = startInMatch <= (otherStartInMatch - overlapRadius) && stopInMatch >= (otherStopInMatch + overlapRadius);
				boolean hasLowerScore = matches.get(j)[4] <= matches.get(i)[4];
				if(overlaps && hasLowerScore){
					matchesToRemove.add(matches.get(j));
				}
			}
		}
		
		for(float[] matchToRemove : matchesToRemove){
			for(int i = 0 ; i < matches.size() ; i++){
				if(matches.get(i) == matchToRemove){
					matches.remove(i);
					i--;
				}
			}
		}		
	}

	/**
	 * Removes matches with scores lower than x% of max score
	 * @param factor the factor to multiply the max score with. If it is 0.5 than everything under 50% of max score is removed.
	 */
	public void removeMatchesWithLowerScores(float factor) {
		List<float[]> matchesToRemove = new ArrayList<float[]>();
		//number of ms for which overlap is still counted as overlap
		float maxScore = 0;//40 ms
		for(int i = 0 ; i < matches.size() ; i++){
			maxScore = Math.max(maxScore, matches.get(i)[4]);
		}
		
		for(int i = 0 ; i < matches.size() ; i++){
			float score = matches.get(i)[4];
			float threshold = factor*maxScore;
			if(score < threshold){
				matchesToRemove.add(matches.get(i));
			}
		}
		
		for(float[] matchToRemove : matchesToRemove){
			for(int i = 0 ; i < matches.size() ; i++){
				if(matches.get(i) == matchToRemove){
					matches.remove(i);
					i--;
				}
			}
		}
		
	}
	
}
