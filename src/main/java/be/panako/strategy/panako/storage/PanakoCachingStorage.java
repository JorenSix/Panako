package be.panako.strategy.panako.storage;

import java.util.HashSet;
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
public class PanakoCachingStorage implements PanakoStorage{

    private final PanakoStorage cachingIndex;
    private final PanakoStorage invertedIndex;

    /**
     * Create a new caching storage front.
     * @param cachingIndex The underlying cache (straight caching index).
     * @param invertedIndex The actual inverted index storage (key value store).
     */
    public PanakoCachingStorage(PanakoStorage cachingIndex, PanakoStorage invertedIndex){
        this.cachingIndex = cachingIndex;
        this.invertedIndex = invertedIndex;
    }

    @Override
    public void storeMetadata(long resourceID, String resourcePath, float duration, int fingerprints) {
        cachingIndex.storeMetadata(resourceID,resourcePath,duration,fingerprints);
        invertedIndex.storeMetadata(resourceID,resourcePath,duration,fingerprints);
    }

    @Override
    public void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1, int f1) {
        cachingIndex.addToStoreQueue(fingerprintHash,resourceIdentifier,t1,f1);
        invertedIndex.addToStoreQueue(fingerprintHash,resourceIdentifier,t1,f1);
    }

    @Override
    public void processStoreQueue() {
        cachingIndex.processStoreQueue();
        invertedIndex.processStoreQueue();
    }

    @Override
    public PanakoResourceMetadata getMetadata(long identifier) {
        return invertedIndex.getMetadata(identifier);
    }

    @Override
    public void printStatistics(boolean detailedStats) {
        invertedIndex.printStatistics(detailedStats);
    }

    @Override
    public void deleteMetadata(long resourceID) {
        cachingIndex.processStoreQueue();
        invertedIndex.processStoreQueue();
    }

    @Override
    public void addToQueryQueue(long queryHash) {
        //it does not make sense to use the non optimized caching index for query tasks
        invertedIndex.addToQueryQueue(queryHash);
    }

    @Override
    public void processQueryQueue(Map<Long, List<PanakoHit>> matchAccumulator, int range) {
        //it does not make sense to use the non optimized caching index for query tasks
        processQueryQueue(matchAccumulator, range, new HashSet<Integer>());
    }

    @Override
    public void processQueryQueue(Map<Long, List<PanakoHit>> matchAccumulator, int range, Set<Integer> resourcesToAvoid) {
        invertedIndex.processQueryQueue(matchAccumulator,range,resourcesToAvoid);
    }

    @Override
    public void addToDeleteQueue(long fingerprintHash, int resourceIdentifier, int t1, int f1) {
        cachingIndex.addToDeleteQueue(fingerprintHash,resourceIdentifier,t1,f1);
        invertedIndex.addToDeleteQueue(fingerprintHash,resourceIdentifier,t1,f1);
    }

    @Override
    public void processDeleteQueue() {
        cachingIndex.processDeleteQueue();
        invertedIndex.processDeleteQueue();
    }

    @Override
    public void clear() {
        cachingIndex.clear();
        invertedIndex.clear();
    }
}
