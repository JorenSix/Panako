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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import be.panako.strategy.Strategy;
import be.panako.strategy.olaf.OlafStrategy;
import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.StopWatch;
import be.panako.util.TimeUnit;

/**
 * Delete fingerptings from the index.
 * @author Joren Six
 */
public class Delete extends Application {
	private final static Logger LOG = Logger.getLogger(Delete.class.getName());

	/**
	 * Default constructor
	 */
	public Delete(){}

	@Override
	public void run(final String... args) {
		int processors = availableProcessors();
		int counter=0;
		final ExecutorService executor = Executors.newFixedThreadPool(processors);
		final List<File> files = this.getFilesFromArguments(args);
		
		System.out.println("Index;Audiofile;Audio duration;Processing time;Audio duration/processing time");
		for(File file: files){
			counter++;
			
			DeleteTask task = new DeleteTask(file, counter, files.size());
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
		return "Deletes audio fingerprints from the storage.";
	}

	@Override
	public String synopsis() {	
		return "delete [audiofilelist.txt... audio_files...]";
	}
	
	private static class DeleteTask implements Runnable{
		private final File file;
		private final int taskID;
		private final int totalTasks;
		
		
		public DeleteTask(File file,int taskID,int totalTasks){
			this.file = file;
			this.taskID = taskID;
			this.totalTasks = totalTasks;
		}

		@Override
		public void run() {
			
			StopWatch w = new StopWatch();
			if(checkFile(file)){

				Strategy strategy =  Strategy.getInstance();
				
				boolean hasResource = false;
				hasResource =  strategy.hasResource(file.getAbsolutePath());
				
				String message=null;
				if(hasResource){
					message = String.format("%d/%d;%s;%s;%s",taskID,totalTasks,file.getName(),StopWatch.toTime("", 0),"Deletion skipped: resource not in the key value store;");
				}else{
					double durationInSeconds = strategy.delete(file.getAbsolutePath());
					
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
