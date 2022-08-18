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

import java.util.HashSet;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.olaf.OlafStrategy;
import be.panako.util.Config;
import be.panako.util.Key;

/**
 * A command line application to check whether two audio files contain duplicate material.
 */
class Same extends Application{

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
		
		//use in memory data storage
		Config.set(Key.OLAF_STORAGE,"MEM");
		Config.set(Key.PANAKO_STORAGE,"MEM");
		
		olaf.store(first, "first");
		
		QueryResultHandler handler = new QueryResultHandler() {
			@Override
			public void handleQueryResult(QueryResult result) {
				System.out.println("Percentage of seconds with fingerprint matches: " + Math.round(result.percentOfSecondsWithMatches * 100) + "%");
			}

			@Override
			public void handleEmptyResult(QueryResult result) {
				System.out.println("Percentage of seconds with fingerprint matches: 0%");
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
