/***************************************************************************
*                                                                          *                     
* Panako - acoustic fingerprinting                                         *   
* Copyright (C) 2014 - Joren Six / IPEM                                    *   
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

import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;

public class Sync extends Application{

	@Override
	public void run(String... args) {
		Strategy strategy = Strategy.getInstance();
		if(args.length<2){
			System.err.println("Needs at least two files to synchronize.");
			System.err.println();
			System.out.println("Name");
			System.out.println("\t" + this.name());
			System.out.println("Synopsis");
			System.out.println("\tpanako " + this.name() + " " + this.synopsis());
			System.out.println("Description");
			System.out.println("\t" + this.description());
		}
		if(strategy instanceof NFFTStrategy){
			NFFTStrategy strat = (NFFTStrategy) strategy;
			List<File> files = getFilesFromArguments(args);
			String reference = files.get(0).getAbsolutePath();
			String[] others = new String[files.size()-1];
			for(int i = 1 ; i< files.size();i++){
				others[i-1]=files.get(i).getAbsolutePath();
			}
			strat.sync(reference, others);			
		}else{
			System.err.println("Currently only NFFT supports the sync operation!");
		}		
	}
	
	

	@Override
	public String description() {
		return "Returns offsets so that audio/video files can be syncronized to the reference audio or video file.";
	}

	@Override
	public String synopsis() {
		return "reference [others...]";
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
