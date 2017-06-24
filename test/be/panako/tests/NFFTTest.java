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

package be.panako.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.strategy.nfft.NFFTStrategy;

public class NFFTTest {

	@Test
	public void testFingerprintHashSign() {
		NFFTStrategy strategy = new NFFTStrategy();
		strategy.toString();
		//fail();
		
		NFFTFingerprint firstPrint = new NFFTFingerprint(2424,28,0.0f,2524,22,0.0f);
		NFFTFingerprint otherPrint = new NFFTFingerprint(887,28,0.0f,987,34,0.0f);
		assertEquals("Sign should be reversed", firstPrint.hash() * -1 , otherPrint.hash());
		
		firstPrint = new NFFTFingerprint(18732,42,0.0f,18799,28,0.0f);
		otherPrint = new NFFTFingerprint(809,42,0.0f,876,28,0.0f);		
		assertEquals("Sign should be the same", firstPrint.hash(), otherPrint.hash());
	}
	
	@Test
	public void testFingerprintExctraction(){
		NFFTStrategy strategy = new NFFTStrategy();
		
		for(int t = 0 ; t <  10; t++){
			File[] pair = TestUtilities.getRandomRefQueryPair();
			List<NFFTFingerprint> ref = strategy.extractFingerprintsFromQuery(pair[0].getAbsolutePath());
			List<NFFTFingerprint> query = strategy.extractFingerprintsFromQuery(pair[1].getAbsolutePath());
			
			//System.out.println("Extracted " + query.size() + " prints from query, " + ref.size() + " prints from ref" );
			
			int counter =0;
			for(int i = 0 ; i < query.size() ; i++){
				for(int j = 0 ; j < ref.size() ; j++){
					if(query.get(i).hash() == ref.get(j).hash()){
						counter++;
					}
				}
			}
			assertTrue("At least 10% of fingerprints from query should be present in ref: " + counter + " fingerprints in common, should be at least: " + query.size()/10, counter > query.size()/10);
		}
	}

}
