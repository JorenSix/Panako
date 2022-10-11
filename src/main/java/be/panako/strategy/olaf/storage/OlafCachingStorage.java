package be.panako.strategy.olaf.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a caching front for a storage engine.
 *
 * To save time on feature extraction a cache can store the extracted features (the fingerprints)
 * and a store often stores an inverted index: the extracted features in an efficient way to query the features.
 *
 * During a query operation the cache is first checked for a match, only if no match is found a feature extraction
 * takes place.
 *
 * The query operations are only executed on the inverted index since it is
 */
public class OlafCachingStorage implements OlafStorage{

    private final OlafStorage cachingIndex;
    private final OlafStorage invertedIndex;

    /**
     * Create a new caching storage front.
     * @param cachingIndex The underlying cache (straight caching index).
     * @param invertedIndex The actual inverted index storage (key value store).
     */
    public OlafCachingStorage(OlafStorage cachingIndex, OlafStorage invertedIndex){
        this.cachingIndex = cachingIndex;
        this.invertedIndex = invertedIndex;
    }

    @Override
    public void storeMetadata(long resourceID, String resourcePath, float duration, int numberOfFingerprints) {
        cachingIndex.storeMetadata(resourceID,resourcePath,duration,numberOfFingerprints);
        invertedIndex.storeMetadata(resourceID,resourcePath,duration,numberOfFingerprints);
    }

    @Override
    public void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1) {
        cachingIndex.addToStoreQueue(fingerprintHash,resourceIdentifier,t1);
        invertedIndex.addToStoreQueue(fingerprintHash,resourceIdentifier,t1);
    }

    @Override
    public void processStoreQueue() {
        cachingIndex.processStoreQueue();
        invertedIndex.processStoreQueue();
    }

    @Override
    public void clearStoreQueue() {
        cachingIndex.clearStoreQueue();
        invertedIndex.clearStoreQueue();
    }

    @Override
    public void printStatistics(boolean printDetailedStats) {
        invertedIndex.printStatistics(printDetailedStats);
    }

    @Override
    public OlafResourceMetadata getMetadata(long identifier) {
        return invertedIndex.getMetadata(identifier);
    }

    @Override
    public void addToQueryQueue(long queryHash) {
        //it does not make sense to use the non optimized caching index for query tasks
        invertedIndex.addToQueryQueue(queryHash);
    }

    @Override
    public void processQueryQueue(Map<Long, List<OlafHit>> matchAccumulator, int range, Set<Integer> resourcesToAvoid) {
        //it does not make sense to use the non optimized caching index for query tasks
        invertedIndex.processQueryQueue(matchAccumulator,range,resourcesToAvoid);
    }

    @Override
    public void addToDeleteQueue(long fingerprintHash, int resourceIdentifier, int t1) {
        cachingIndex.addToDeleteQueue(fingerprintHash,resourceIdentifier,t1);
        invertedIndex.addToDeleteQueue(fingerprintHash,resourceIdentifier,t1);
    }

    @Override
    public void processDeleteQueue() {
        cachingIndex.processDeleteQueue();
        invertedIndex.processDeleteQueue();
    }

    @Override
    public void deleteMetadata(long resourceID) {
        cachingIndex.deleteMetadata(resourceID);
        invertedIndex.deleteMetadata(resourceID);
    }

    @Override
    public void clear() {
        cachingIndex.clear();
        invertedIndex.clear();
    }
}
