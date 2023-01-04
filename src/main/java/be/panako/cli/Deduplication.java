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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.FileUtils;


/**
 * A command line application to deduplicate an archive of music files.
 */
class Deduplication extends Application implements QueryResultHandler  {
	@Override
	public void run(String... args) {

		if(this.hasArgument("-file_hash",args) || this.hasArgument("--file_hash",args)){
			deduplicateWithFileHashing(args);
		}else{
			deduplicateWithFingerprinting(args);
		}
	}

	private void deduplicateWithFileHashing(String... args){
		final HashMap<Integer,List<String>> list = new HashMap<>();
		final Strategy strategy = Strategy.getInstance();
		List<File> files = getFilesFromArguments(args);
		for(int i = 0 ; i < files.size() ; i++){
			File f = files.get(i);
			Integer fileHash = FileUtils.getFileHash(f);
			if(!list.containsKey(fileHash)){
				list.put(fileHash,new ArrayList<>());
			}
			list.get(fileHash).add(f.getAbsolutePath());
			System.out.println(String.format("%d,%d,%d,%s",i,files.size(),fileHash,f.getAbsolutePath()));
		}
		list.forEach((key,l) -> {
			if(l.size() >= 2){
				System.out.print(key);
				l.forEach( e -> {
					System.out.print(";" + e);
				});
				System.out.println();
			}
		});
	}

	private void deduplicateWithFingerprinting(String... args){
		String[] storeArgs = new String[args.length+1];
		storeArgs[0] = "store";
		for(int i = 1 ; i < storeArgs.length ; i++){
			storeArgs[i] = args[i-1];
		}
		Panako.main(storeArgs);

		List<File> files = super.getFilesFromArguments(args);
		//monitor
		Strategy strategy = Strategy.getInstance();

		for(File f: files){
			HashSet<Integer> identifiersToAvoid = new HashSet<Integer>();
			Integer identifierToAvoid = Integer.valueOf(strategy.resolve(f.getName()));
			identifiersToAvoid.add(identifierToAvoid);
			strategy.monitor(f.getAbsolutePath(), 10, identifiersToAvoid, this);
		}
	}

	@Override
	public String description() {
		return "Deduplication tries to find duplicates in a set of files. Basically a store operation and monitor step for all files." +
				"if --file_hash is provided no acoustic fingerprinting is done but the files are compared using a file hash";
	}

	@Override
	public String synopsis() {
		return "deduplication [--file_hash] [audiofilelist.txt... audio_files...]";
	}

	@Override
	public boolean needsStorage() {
		return true;
	}

	@Override
	public boolean writesToStorage() {
		return true;
	}

	@Override
	public void handleQueryResult(QueryResult result) {
		Panako.printQueryResult(result);	
	}

	@Override
	public void handleEmptyResult(QueryResult result) {
		Panako.printQueryResult(result);
	}

}
