package be.panako.strategy.olaf;

public class OlafResourceMetadata {
	public int numFingerprints;
	public double duration;
	public String path;
	int identifier;
	
	public double printsPerSecond() {
		return numFingerprints / duration;
	}
}
