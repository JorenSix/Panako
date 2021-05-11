/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import be.panako.strategy.Strategy;
import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.StopWatch;
import be.panako.util.TimeUnit;

/**
 * Store audio fingerptings in the storage. 
 * @author Joren Six
 */
public class Store extends Application {
	private final static Logger LOG = Logger.getLogger(Store.class.getName());
	
	@Override
	public void run(final String... args) {
		int processors = availableProcessors();
		int counter=0;
		final ExecutorService executor = Executors.newFixedThreadPool(processors);
		final List<File> files = this.getFilesFromArguments(args);
		boolean extractMetaData = hasArgument("-m", args) || hasArgument("--meta-data", args);
		if(files.size() > 1){
			String msg = "Processing " + files.size() + " files on " + processors + " seperate threads.";
			System.out.println(msg);
			LOG.info("Store task started. " +  msg);
		}
		System.out.println("Audiofile;Audio duration;Fingerprinting duration;ratio");
		for(File file: files){
			counter++;
			
			StoreTask task = new StoreTask(file, counter, files.size(),extractMetaData);
			if(processors == 1) {
				// Only one thread available:
				// run on the main thread
				task.run();
			}else {
				// run on thread managed by pool
				executor.submit(task);
			}
		}
	
		try {
			//do not accept more tasks.
			executor.shutdown();
			//wait for tasks to finish
			executor.awaitTermination(300, java.util.concurrent.TimeUnit.DAYS);
			//System.exit(0);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public String description() {
		return "Stores audio fingerprints in the storage.";
	}

	@Override
	public String synopsis() {	
		return "store [audiofilelist.txt... audio_files...]";
	}
	
	private static class StoreTask implements Runnable{
		private final File file;
		private final int taskID;
		private final int totalTasks;
		private final boolean extractMetaData;
		
		
		public StoreTask(File file,int taskID,int totalTasks,boolean extractMetaData){
			this.file = file;
			this.taskID = taskID;
			this.totalTasks = totalTasks;
			this.extractMetaData = extractMetaData;
		}

		@Override
		public void run() {
			
			StopWatch w = new StopWatch();
			if(checkFile(file)){
				
				Strategy strategy = Strategy.getInstance();
				
				
				boolean isDouble = false;				
				if(Config.getBoolean(Key.CHECK_DUPLICATE_FILE_NAMES) ){
					isDouble =  strategy.hasResource(file.getAbsolutePath());
				}

				String message=null;
				if(isDouble){
					message = String.format("%d/%d;%s;%s;%s",taskID,totalTasks,file.getName(),StopWatch.toTime("", 0),"Skipped: resource already stored;");
				}else{
					double durationInSeconds = strategy.store(file.getAbsolutePath(), file.getName());
					
					if(extractMetaData){
						extractMetaData(file);
					}
					double cpuSecondsPassed = w.timePassed(TimeUnit.SECONDS);
					String audioDuration = StopWatch.toTime("", (int) Math.round(durationInSeconds));
					String cpuTimeDuration = w.formattedToString();
					double timeRatio = durationInSeconds/cpuSecondsPassed;
					message = String.format("%d/%d;%s;%s;%s;%.2f",taskID,totalTasks,file.getName(),audioDuration,cpuTimeDuration,timeRatio);			
				}
				LOG.info(message);
				System.out.println(message);
			}
		}
		
		private void extractMetaData(File audioFile){
			Strategy strategy = Strategy.getInstance();
			String identifier = strategy.resolve(file.getAbsolutePath());
			String dir = Config.get(Key.META_DATA_DIRECTORY);
			File metaDataFile = new File(dir,identifier+".json");
			String command = Config.get(Key.META_DATA_COMMAND);
			Map<String,File> map = new HashMap<String,File>();
			map.put("audiofile", audioFile);
			map.put("metadatafile", metaDataFile);
			CommandLine cmdLine = new CommandLine(command);
			cmdLine.addArgument("${audiofile}");
			cmdLine.addArgument("${metadatafile}");
			cmdLine.setSubstitutionMap(map);
			DefaultExecutor executor = new DefaultExecutor();
			//executor.setExitValue(1);
			ExecuteWatchdog watchdog = new ExecuteWatchdog(1000000);
			executor.setWatchdog(watchdog);
			try {
				int exitValue = executor.execute(cmdLine);
				if(exitValue==0){
					System.out.println("Extracted metadata successfully");
				}else{
					System.err.println("Failed to extract metadata for:" + audioFile);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		private boolean checkFile(File file){
			boolean fileOk = false;
			//file must be smaller than a configured number of bytes
			if(file.length() != 0 && file.length() < Config.getInt(Key.MAX_FILE_SIZE)){
				fileOk = true;
			}else{
				String message = "Could not process " + file.getName() + " it has an unacceptable file size: zero or larger than " + Config.getInt(Key.MAX_FILE_SIZE) + "bytes ).";
				LOG.warning(message);
				System.out.println(message);
			}
			return fileOk;
		}
	}
	
	
	@Override
	public boolean needsStorage() {
		return true;
	}
	
	@Override
	public boolean writesToStorage() {
		return true;
	}
}
