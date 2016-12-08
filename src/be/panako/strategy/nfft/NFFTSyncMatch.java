/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2015 - Joren Six / IPEM                             *
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


package be.panako.strategy.nfft;

public class NFFTSyncMatch {
	
	private final String reference;
	private final String matchingStream;
	
	private final double startInReference; 
	private final double stopInReference; 
	private final double startInMatchingStream;
	private final double stopInMatchinStream;
	private final double score;
	private final double roughOffset;//in fft blocks
	private final double refinedOffset;//fft blocks + covariance
	
	
	
	public NFFTSyncMatch(String reference, String matchingStream,double startInReference,double stopInReference,double startInMatchingStream,double stopInMatchinStream,double score,double roughOffset,double refinedOffset){
		this.reference = reference;
		this.matchingStream = matchingStream;
		
		this.startInReference = startInReference;
		this.stopInReference = stopInReference;
		this.startInMatchingStream = startInMatchingStream;
		this.stopInMatchinStream = stopInMatchinStream;
		this.score = score;
		this.roughOffset = roughOffset;
		this.refinedOffset = refinedOffset;
	}
	
	
	public double getStartInReference(){return startInReference;}
	public double getStopInReference(){return stopInReference ;}
	public double getStartInMatchingStream(){return startInMatchingStream;}
	public double getStopInMatchinStream(){return stopInMatchinStream;}
	public double getScore(){return score;}
	public double getRoughOffset(){return roughOffset;}
	public double getRefinedOffset(){return refinedOffset;}
	
	
	public String getMatchingStream(){
		return matchingStream;
	}
	
	
	public String getReferenceFileName(){
		return reference;
	}
	
	
}
