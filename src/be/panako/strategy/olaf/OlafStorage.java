package be.panako.strategy.olaf;

public interface OlafStorage {

	void storeMetadata(long resourceID, String resourcePath, float duration, int fingerprints);

	void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1);

	void processStoreQueue();

	OlafResourceMetadata getMetadata(long identifier);

}