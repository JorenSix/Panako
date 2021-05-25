package be.panako.cli;

import java.util.HashSet;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.olaf.OlafStrategy;
import be.panako.util.Config;
import be.panako.util.Key;

public class Same extends Application{

	@Override
	public void run(String... args) {
		
		if(args.length == 2) {
			String first = args[0];
			String other = args[1];
			if( checkFile(first) && checkFile(other))
				same(first,other);
			else
				printHelp();
		}else {
			printHelp();
		}
	}
	
	private void same(String first,String other) {
		OlafStrategy olaf = (OlafStrategy) OlafStrategy.getInstance();
		Config.set(Key.OLAF_STORAGE,"MEM");
		olaf.store(first, "first");
		
		QueryResultHandler handler = new QueryResultHandler() {
			@Override
			public void handleQueryResult(QueryResult result) {
				System.out.println("Percentage of seconds with matches: " + Math.round(result.percentOfSecondsWithMatches * 100) + "%");
			}

			@Override
			public void handleEmptyResult(QueryResult result) {
				System.out.println("Percentage of seconds with matches: 0%");
			}};
		olaf.query(other,1 , new HashSet<Integer>(),handler);
	}

	@Override
	public String description() {
		return "Returns a probability estimating whether two files contain the same audio\n\t";
	}

	@Override
	public String synopsis() {
		return "first.mp3 second.mp3";
	}

	@Override
	public boolean needsStorage() {
		return false;
	}

	@Override
	public boolean writesToStorage() {
		return false;
	}

}
