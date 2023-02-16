package be.panako.tests;

import be.panako.cli.Panako;
import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.olaf.OlafStrategy;
import be.panako.strategy.olaf.storage.OlafHit;
import be.panako.strategy.olaf.storage.OlafResourceMetadata;
import be.panako.strategy.olaf.storage.OlafStorage;
import be.panako.strategy.olaf.storage.OlafStorageKV;

import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OlafStorageKVTest {

    List<File> references;
    List<File> queries;

    List<File> queries_ota;

    @BeforeEach
    void setUp() {
        references = TestData.referenceFiles();
        queries = TestData.queryFiles();
        queries_ota = TestData.overTheAirQueryFiles();
        Config config = Config.getInstance();
        String tempStoragePath = FileUtils.combine(FileUtils.temporaryDirectory(),"olaf_test");
        config.set(Key.OLAF_LMDB_FOLDER,tempStoragePath);
        config.set(Key.OLAF_STORAGE,"LMDB");
        config.set(Key.OLAF_CACHE_TO_FILE,"FALSE");
        config.set(Key.OLAF_USE_CACHED_PRINTS,"FALSE");
    }


    @Test
    void storeMetadata() {
        OlafStorage s = OlafStorageKV.getInstance();
        s.storeMetadata(10L,"/test/path",100,2000);
        OlafResourceMetadata metadata = s.getMetadata(10L);
        assertTrue(metadata.path.contentEquals("/test/path"));
        assertEquals(metadata.duration,100);
        assertEquals(metadata.numFingerprints,2000);
    }

    @Test
    void storeFingerprint() {
        OlafStorage s = OlafStorageKV.getInstance();
        //store two
        s.addToStoreQueue(10L,666,77);
        s.addToStoreQueue(200L,666,78);
        s.processStoreQueue();

        //delete one
        s.addToDeleteQueue(200L,666,78);
        s.processDeleteQueue();

        //query for the only remaining fingerprint
        s.addToQueryQueue(10);
        Map<Long, List<OlafHit>> matchAccumulator = new TreeMap<>();
        s.processQueryQueue(matchAccumulator,2,new HashSet<>());
        assertEquals(1 ,matchAccumulator.size(),"Expected only one match");
    }


    private void testMatching(List<File> queries){
        float maxStartDelta = 3.5f;
        Strategy s = new OlafStrategy();
        List<Integer> refIds = new ArrayList<>();
        for(File ref : references) {
            s.store(ref.getAbsolutePath(), ref.getName());
            refIds.add(TestData.getIdFromFileName(ref.getName()));
        }
        Random r = new Random(0L);
        Collections.shuffle(queries,r);

        for(File query : queries){

            String path = query.getAbsolutePath();
            String baseName = query.getName();

            Integer expectedId = TestData.getIdFromFileName(query.getName());
            boolean matchExpected = refIds.contains(expectedId);
            int expectedStart = TestData.getStartAndStop(query.getName())[0];

            s.query(path, 1, new HashSet<>(), new QueryResultHandler() {
                @Override
                public void handleQueryResult(QueryResult result) {
                    Panako.printQueryResult(result);
                    assertTrue(result.refIdentifier.equalsIgnoreCase(expectedId +""));
                    //1071559 is electronic music with exact repetition so it is impossible to determine start offset
                    if(1071559 != Integer.valueOf(result.refIdentifier) ) {
                        assertEquals(expectedStart, result.refStart, maxStartDelta, "Returned start should be close to actual start.");
                    }
                }

                @Override
                public void handleEmptyResult(QueryResult result) {
                    Panako.printQueryResult(result);
                    //1071559 is electronic music with exact repetition so it is impossible to determine start offset
                    if(1071559 != TestData.getIdFromFileName(baseName) ) {
                        assertTrue(!matchExpected,"Unexpected empty match for " + baseName);
                    }

                }
            });

            System.out.println("Correctly matched to index: " + baseName);
        }
    }

    @Test
    void testMatchingPlain(){
        testMatching(queries);
    }

    @Test
    void testMatchingOTA(){
        //Only use two event points for a fingerprint
        Config.set(Key.OLAF_EPS_PER_FP,"2");
        //Use simple histogram matching for noisy queries
        Config.set(Key.OLAF_MATCH_FALLBACK_TO_HIST,"TRUE");
        //TO avoid false positives: allow fewer seconds without matches.
        Config.set(Key.OLAF_MIN_SEC_WITH_MATCH,"0.30");

        testMatching(queries_ota);
    }
}