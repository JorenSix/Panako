package be.panako.strategy.panako;

public class PanakoMatch {
	
	public long matchedNearHash;

	public long originalHash;

	/**
	 * The match audio identifier
	 */
	public int identifier;
	
	/**
	 * Time in blocks in the original, matched audio.
	 */
	public int matchTime;
	
	/**
	 * Time in blocks in the query.
	 */
	public int queryTime;
	
	/**
	 * Frequency bin in the original, matched audio.
	 */
	public int matchF1;
	
	/**
	 * Frequency bin the query.
	 */
	public int queryF1;
	
	public int Δt() {
		return matchTime - queryTime;
	}
	
	public String toString() {
		return String.format("%d %d %d %d", identifier, matchTime, queryTime, Δt());
	}
}