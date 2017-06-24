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

import java.util.Random;

import org.junit.Test;

import be.panako.strategy.nfft.storage.Storage;
import be.panako.strategy.nfft.storage.redisson.NFFTRedisStorage;

public class NFFTRedisStorageTests {
	
	@Test
	public void test() {
		Storage storage = NFFTRedisStorage.getInstance();
		Random r = new Random();
		for( int i = 0 ; i < 10 ; i++){
			int nrOfAudioObjectsPrev = storage.getNumberOfAudioObjects();
			int nrOfFingerprintsPrev = storage.getNumberOfFingerprints();
			
			storage.addAudio(r.nextInt(), "description");
			
			storage.addFingerprint(511 + r.nextInt(), 12,  r.nextInt());
			storage.addFingerprint(5111, 123, r.nextInt());
			
			int nrOfAudioObjectsCurr = storage.getNumberOfAudioObjects();
			int nrOfFingerprintsCurr = storage.getNumberOfFingerprints();
			
			assertEquals(nrOfAudioObjectsPrev+1, nrOfAudioObjectsCurr);
			assertEquals(nrOfFingerprintsPrev+2, nrOfFingerprintsCurr);	
		}
		
		
	}

}
