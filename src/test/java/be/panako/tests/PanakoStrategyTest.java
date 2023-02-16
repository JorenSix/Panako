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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        float maxStartDelta = 3.5f;
        List<Integer> refIds = new ArrayList<>();
        Strategy s = new PanakoStrategy();
        for(File ref : references){
            s.store(ref.getAbsolutePath(),ref.getName());
            refIds.add(TestData.getIdFromFileName(ref.getName()));
        }

        for(File query : queries){
            String path = query.getAbsolutePath();

            Integer expectedId = TestData.getIdFromFileName(query.getName());
            boolean matchExpected = refIds.contains(expectedId);
            int expectedStart = TestData.getStartAndStop(query.getName())[0];

            s.query(path, 1, new HashSet<>(), new QueryResultHandler() {
                @Override
                public void handleQueryResult(QueryResult result) {
                    assertTrue(result.refIdentifier.equalsIgnoreCase(expectedId + ""));
                    assertEquals(expectedStart,result.refStart,maxStartDelta,"Returned start should be close to actual start.");
                }

                @Override
                public void handleEmptyResult(QueryResult result) {
                    assertTrue(!matchExpected,"Unexpected a match for " + query.getName());;
                }
            });
        }
    }
}