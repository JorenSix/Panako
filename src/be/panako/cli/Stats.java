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

import be.panako.strategy.Strategy;



/**
 * Shows some statistics about the storage.
 * @author Joren Six
 */
public class Stats extends Application {

	@Override
	public void run(String... args) {
		Strategy strategy = Strategy.getInstance();
		if(args.length == 1){
			int seconds = Integer.valueOf(args[0]);
			while(true){
				strategy.printStorageStatistics();
				try {
					Thread.sleep(seconds * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
				System.out.println();
			}
		}else{
			strategy.printStorageStatistics();
		}
	}
	

	@Override
	public String description() {
		return "Calculates and prints some statistics about the audio objects in the storage.\n"
				+ "\tn\tPrint statistics every n seconds. If no n is \n\t\tpresent, statistics are printed once.";
	}

	@Override
	public String synopsis() {
		return "stats [n]";
	}

	
	@Override
	public boolean needsStorage() {
		return true;
	}
	
	@Override
	public boolean writesToStorage() {
		return false;
	}
}
