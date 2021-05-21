package panako.strategy.gaborator;

public class GaboratorResourceMetadata {
	public int numFingerprints;
	public double duration;
	public String path;
	int identifier;
	
	public double printsPerSecond() {
		return numFingerprints / duration;
	}
}
