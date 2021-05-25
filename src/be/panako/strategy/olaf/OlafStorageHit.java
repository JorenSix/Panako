package be.panako.strategy.olaf;

public class OlafStorageHit{
	public final long originalHash;
	public final long matchedNearHash;
	
	public final int t;
	public final int resourceID;
	
	public OlafStorageHit(long originalHash, long matchedNearHash,long t, long resourceID) {
		this.originalHash = originalHash;
		this.matchedNearHash = matchedNearHash;
		this.t=(int)t;
		this.resourceID=(int)resourceID;
	}
	
}