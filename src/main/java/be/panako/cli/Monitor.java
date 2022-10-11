/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2022 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/

package be.panako.cli;


import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.util.AudioResourceUtils;

/**
 * A command line application to monitor long audio files.
 */
public class Monitor extends Application implements QueryResultHandler {
	private final static Logger LOG = Logger.getLogger(Monitor.class.getName());
	/**
	 * Default constructor
	 */
	public Monitor(){}

	@Override
	public void run(String... args) {
		Strategy strategy = Strategy.getInstance();
		int processors = availableProcessors();

		if (args.length == 0){
			String inputResource = Panako.DEFAULT_MICROPHONE;
			Panako.printQueryResultHeader();
			strategy.monitor(inputResource,1,new HashSet<Integer>(), this);
			return;
		}
		// else get and process list of files

		List<File> files = this.getFilesFromArguments(args);

		if(files.size() > 1){
			System.out.println("Processing " + files.size() + " queries on " + processors + " separate threads.");
		}

		Panako.printQueryResultHeader();

		if(hasArgument("debug", args) || processors==1){
			int taskNumber = 1;
			for(File file: files){
				new Monitor.MonitorTask(file.getAbsolutePath(),taskNumber,files.size()).run();
				taskNumber++;
			}
		}else{
			ExecutorService executor = Executors.newFixedThreadPool(processors);
			int taskNumber = 1;
			for(File file: files){
				executor.submit(new Monitor.MonitorTask(file.getAbsolutePath(),taskNumber,files.size()));
				taskNumber++;
			}
			executor.shutdown();
			try {
				//wait for tasks to finish
				executor.awaitTermination(300, java.util.concurrent.TimeUnit.DAYS);
				System.exit(0);
			} catch (InterruptedException e1) {
				//Thread was interrupted
				LOG.severe("Did not finish all tasks, thread was interrupted!");
			}
		}
	}

	private static class MonitorTask implements Runnable, QueryResultHandler{
		private final String path;
		private final HashSet<Integer> emptyHashSet = new HashSet<Integer>();
		private final Strategy strategy;
		private final int numberOfQueryResults;
		private final int taskNumber;
		private final int totalTasks;

		public MonitorTask(String path,int taskNumber, int totalTasks){
			this.path = path;
			this.numberOfQueryResults = Config.getInt(Key.NUMBER_OF_QUERY_RESULTS);
			strategy = Strategy.getInstance();
			this.taskNumber = taskNumber;
			this.totalTasks = totalTasks;
		}

		@Override
		public void run() {
			strategy.monitor(path, this.numberOfQueryResults,emptyHashSet, this);
		}

		@Override
		public void handleQueryResult(QueryResult r) {
			Panako.printQueryResult(r,taskNumber,totalTasks);
		}

		@Override
		public void handleEmptyResult(QueryResult r) {
			Panako.printQueryResult(r,taskNumber,totalTasks);
		}
	}

	@Override
	public String description() {
		return "Monitors a stream or a long audio file, the main difference with query is that more than one detection result is expected.";
	}

	@Override
	public String synopsis() {
		return "[pls|m3u|file|txt...]";
	}

	@Override
	public boolean needsStorage() {
		return true;
	}

	@Override
	public boolean writesToStorage() {
		return false;
	}

	@Override
	public void handleQueryResult(QueryResult r) {
		Panako.printQueryResult(r);
	}

	@Override
	public void handleEmptyResult(QueryResult r) {
		Panako.printQueryResult(r);
	}
}
