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


package be.panako.strategy.rafs;

import java.util.BitSet;
import java.util.TreeMap;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * @author Joren Six
 *
 * Packs individual 32bits fingerprints into blocks of 128bits. 
 * To counter timing issues blocks are started with some overlap.
 */
public class RafsPacker implements AudioProcessor{
	
	private final RafsExtractor extractor;
	
	public final TreeMap<Float,BitSet> packedFingerprints;
	public final TreeMap<Float,int[]> packedProbabilities;
	
	private final boolean trackProbabilities;
	private BitSet currentFingerprint;
	private int[] currentProbabilities;
	private double currentTimeStamp = 0;
	private int bitIndex = 0;
	
	public RafsPacker(RafsExtractor extractor,boolean trackProbabilities) {
		this.extractor = extractor;
		packedFingerprints = new TreeMap<>();
		packedProbabilities = new TreeMap<>();
		this.trackProbabilities=trackProbabilities;
		currentFingerprint = new BitSet(128);
		currentProbabilities = new int[128];
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		
		BitSet set = extractor.fingerprints.get((float) audioEvent.getTimeStamp());
		int[] probabilities = null;
		if(trackProbabilities)
			probabilities = extractor.fingerprintProbabilities.get((float) audioEvent.getTimeStamp());
		
		if(set != null){
			if(bitIndex == 0){
				currentTimeStamp = audioEvent.getTimeStamp();
			}
			
			for(int i = 0 ; i < 32 ; i++){
				currentFingerprint.set(bitIndex,set.get(i));
				if(trackProbabilities)
					currentProbabilities[bitIndex]=probabilities[i];
				bitIndex++;
			}
			
			//completed print 
			if(bitIndex == 128){
				//store the print
				packedFingerprints.put((float) currentTimeStamp, (BitSet) currentFingerprint.clone());
				
				if(trackProbabilities)
					packedProbabilities.put((float) currentTimeStamp, currentProbabilities.clone());
				
				//overlap of one print of 32 bits!
				bitIndex = 0;
				
				for(int i = 0 ; i < 32 ; i++){
					currentFingerprint.set(bitIndex,set.get(i));
					if(trackProbabilities)
						currentProbabilities[bitIndex]=probabilities[i];
					bitIndex++;
				}
				currentTimeStamp = audioEvent.getTimeStamp();				
			}
		}		
		
		return true;
	}

	@Override
	public void processingFinished() {
	}
}
