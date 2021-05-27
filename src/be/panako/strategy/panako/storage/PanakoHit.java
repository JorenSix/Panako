package be.panako.strategy.panako;

public class PanakoStorageHit{
	public final long originalHash;
	public final long matchedNearHash;
	
	public final int t;
	public final int f;
	public final int resourceID;
	
	public PanakoStorageHit(long originalHash, long matchedNearHash,long t, long resourceID, long f) {
		this.originalHash = originalHash;
		this.matchedNearHash = matchedNearHash;
		this.t=(int)t;
		this.f =(int)f;
		this.resourceID=(int)resourceID;
	}
	
}