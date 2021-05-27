package be.panako.strategy.olaf;

public class OlafMatch {
	
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
	
	
	public int Δt() {
		return matchTime - queryTime;
	}
	
	public String toString() {
		return String.format("%d %d %d %d", identifier, matchTime, queryTime, Δt());
	}
}