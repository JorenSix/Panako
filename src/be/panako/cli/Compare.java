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
import java.util.List;

import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;

/**
 * Compares audio files in answers the following question:
 *  when does the same exact audio reappear in another file?
 *
 */
public class Compare extends Application {

	@Override
	public void run(String... args) {
		
		Strategy strategy = Strategy.getInstance();
		if(strategy instanceof NFFTStrategy){
			NFFTStrategy strat = (NFFTStrategy) strategy;
			List<File> files = this.getFilesFromArguments(args);
			if(files.size()==1){
				strat.compareFingerprints(files.get(0),files.get(0));
			}else{
				for(int i = 1 ; i < files.size();i++){
					strat.compareFingerprints(files.get(0),files.get(1));
					System.out.println("");
				}
			}	
		}else{
			System.out.println("Currently only NFFT supports the print operation!");
			System.err.println("Currently only NFFT supports the print operation!");
		}		
	}

	@Override
	public String description() {
		
		return "Extracts fingerprints prints the times at which fingerprints occur twice.";
	}

	@Override
	public String synopsis() {
		return "[audio_files...]";
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
