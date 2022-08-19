package be.panako.tests;

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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OlafStorageKVTest {

    @BeforeEach
    void setUp() {
        Config config = Config.getInstance();
        String tempStoragePath = FileUtils.combine(FileUtils.temporaryDirectory(),"olaf_test");
        config.set(Key.OLAF_LMDB_FOLDER,tempStoragePath);
    }

    @AfterEach
    void tearDown() {
        Config config = Config.getInstance();
        System.out.println( config.get(Key.OLAF_LMDB_FOLDER));
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
}