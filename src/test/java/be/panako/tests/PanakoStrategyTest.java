package be.panako.tests;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.panako.PanakoStrategy;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanakoStrategyTest {

    List<File> references;
    List<File> queries;
    @BeforeEach
    void setUp() {
        references = TestData.referenceFiles();
        queries = TestData.queryFiles();
        Config.set(Key.PANAKO_LMDB_FOLDER,FileUtils.combine(FileUtils.temporaryDirectory(),"panako_test_data"));
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testPanakoStrategy(){
        Strategy s = new PanakoStrategy();
        for(File ref : references){
            s.store(ref.getAbsolutePath(),ref.getName());
        }

        s.query(queries.get(1).getAbsolutePath(), 1, new HashSet<>(), new QueryResultHandler() {
            @Override
            public void handleQueryResult(QueryResult result) {
                assertTrue(result.refIdentifier.equalsIgnoreCase(1051039 + ""));
                assertEquals(34,result.refStart,3.5,"Expect start to be close to 34s");
            }

            @Override
            public void handleEmptyResult(QueryResult result) {
                assertTrue(false);
            }
        });

    }
}