package be.panako.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Test;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;

public class MatchTest {
	
	@Test
	public void storeAndQueryTest(){
		String smallDatasetReferenceFolder = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/reference";
		String[] ref = new File(smallDatasetReferenceFolder).list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("mp3") && name.startsWith("189211");
			}
		});
		
		Strategy strategy = Strategy.getInstance();
		for(String refFile : ref){
			String resource = new File(smallDatasetReferenceFolder,refFile).getAbsolutePath();
			if(!strategy.hasResource(resource)){
				strategy.store(resource, refFile);
			}
		}
		
		String queriesFolder = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/queries";
		String[] queries = new File(queriesFolder).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("mp3") && name.startsWith("189211");
			}
		});
		
		for(final String queryFile : queries){
			String resource = new File(queriesFolder,queryFile).getAbsolutePath();
			
			strategy.query(resource,3,new QueryResultHandler() {
				
				@Override
				public void handleQueryResult(QueryResult result) {
					int startExpected = Integer.valueOf(queryFile.split("_")[1].split("-")[0].replace("s", ""));
					System.out.println(result.description + " " + queryFile);
					System.out.println(Math.round(result.time));
					assertEquals("Found an unexpected start of query for " + queryFile, startExpected, Math.round(result.time));
				}
				
				@Override
				public void handleEmptyResult(QueryResult result) {
					assertTrue("Result should not be empty!", false);
				}
			});
		}
	}

}
