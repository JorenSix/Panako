/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2021 - Joren Six / IPEM                             *
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import be.panako.strategy.olaf.OlafFingerprint;
import be.panako.strategy.olaf.OlafResourceMetadata;
import be.panako.strategy.olaf.OlafDBStorage;
import be.panako.strategy.olaf.OlafDBStorage.OlafDBHit;
import be.panako.strategy.olaf.OlafStrategy;

public class OlafTests {
	
	@Test
	public void testZOrderCurveFingerprintHash() {
		
		OlafFingerprint fp1 = new OlafFingerprint( 1, 10, 1,   20, 20, 2,    45, 50, 3);
		OlafFingerprint fp2 = new OlafFingerprint( 2, 10, 1,   20, 20, 2,    45, 50, 3); 
		OlafFingerprint fp3 = new OlafFingerprint( 3, 10, 1,   10, 20, 2,    20, 50, 3); 
		OlafFingerprint fp4 = new OlafFingerprint( 1, 10, 1,   10, 23, 2,    45, 50, 3); 
		OlafFingerprint fp5 = new OlafFingerprint( 1, 10, 1,   10, 20, 2,    45, 50, 1); 
		OlafFingerprint fp6 = new OlafFingerprint(11, 10, 1,   20, 20, 2,    55, 50, 3);
		OlafFingerprint fp7 = new OlafFingerprint( 1, 11, 1,   20, 20, 2,    45, 50, 3);
		
		// 1 time bin diff
		assertTrue(fp1.hash()-fp2.hash() == 1);
		
		// 27 time bins diff
		assertTrue(fp1.hash()-fp3.hash() == 27);
		
		// 2 freq bins diff
		assertTrue(Math.abs(fp1.hash()-fp4.hash()) > 1000);
		
		// mag relation diff
		assertTrue(Math.abs(fp1.hash()-fp5.hash()) > 1000);
		
		// time shift, hash is the same
		assertTrue(Math.abs(fp1.hash()-fp6.hash()) == 0);
		
		// 1 freq bin diff , hash is the same
		assertTrue(Math.abs(fp1.hash()-fp7.hash()) == 0);
	}
	
	@Test
	public void testOlafStorage() {
		OlafDBStorage db = OlafDBStorage.getInstance();
		
		
		for(int i = 0 ; i < 7 ; i++) {
			db.addToStoreQueue(i,i,i);
		}
		db.processStoreQueue();
		
		db.addToStoreQueue(1,1,1);
		db.addToStoreQueue(2,9,9);
		db.addToStoreQueue(2,7,7);
		db.processStoreQueue();	
		
		db.addToQueryQueue(3);
		Map<Long,List<OlafDBHit>> matchAccumulator = new HashMap<>();
		db.processQueryQueue(matchAccumulator, 1);
		
		db.addToDeleteQueue(1,1,1);
		db.addToDeleteQueue(2,9,9);
		db.addToDeleteQueue(2,7,7);
		db.processDeleteQueue();
		
		System.out.println();
		
		db.addToQueryQueue(3);
		matchAccumulator.clear();
		db.processQueryQueue(matchAccumulator, 1);
		
		Random r = new Random();
		for(int i = 0 ; i < 10_000 ; i++) {
			db.addToStoreQueue(r.nextLong(), r.nextInt(),r.nextInt());
		}
		
		db.processStoreQueue();
		
		db.storeMetadata(12, "tsqdfsqdf/sdfqsdf/qsdf/qdsfest",10.0f,1000);
		OlafResourceMetadata metadata = db.getMetadata(12);
		assertTrue(metadata.path.equals("tsqdfsqdf/sdfqsdf/qsdf/qdsfest"));
		assertTrue(null == db.getMetadata(13));
		
		for(int i = 0 ; i < 10_000 ; i++) {
			db.addToDeleteQueue((long) Integer.MAX_VALUE + (long) i, i,i);
		}
		db.processDeleteQueue();
		
		for(int i = 0 ; i < 10_000 ; i++) {
			db.addToStoreQueue((long) Integer.MAX_VALUE + (long) i, i, i);
		}
		
		long checkKey = (long) Integer.MAX_VALUE + (long) 10;
		db.addToStoreQueue(checkKey, 5,6);
		db.addToStoreQueue(checkKey, 6,6);
		db.addToStoreQueue(checkKey, 7,6);
		db.processStoreQueue();
		
		//query
		db.addToQueryQueue(checkKey);
		matchAccumulator.clear();
		db.processQueryQueue(matchAccumulator, 0);
		assertTrue(matchAccumulator.get(checkKey).size()>=4);
		
		matchAccumulator.clear();
		db.addToQueryQueue(checkKey);
		db.processQueryQueue(matchAccumulator, 1);
		assertTrue(4 + 1*2 <= matchAccumulator.get(checkKey).size());
		
		matchAccumulator.clear();
		db.addToQueryQueue(checkKey);
		db.processQueryQueue(matchAccumulator, 2);
		assertTrue(4 + 2*2 <= matchAccumulator.get(checkKey).size());
		
				
		db.entries(true);
	}

	
	public void strategyTests() {
String rec = "/Volumes/papiom/Datasets/Free Music Archive - FMA/fma_small/013/013928.mp3";
		
		rec = "/Volumes/papiom/Datasets/Free Music Archive - FMA/fma_small/084/084605.mp3";
		OlafStrategy strat = new OlafStrategy();
		strat.store(rec, "desc");
		
		strat.query(rec, 7, new HashSet<>(), null);
		
		strat.query("/Users/joren/lmdbtest/013928_20_004.wav", 7, new HashSet<>(), null);
		
		strat.query("/Volumes/papiom/Datasets/IPEM archive/tapes/audio/057cr5rm57.m4a", 7, new HashSet<>(), null);
		
		strat.query("/Volumes/papiom/Datasets/Free Music Archive - FMA/fma_small/014/014063.mp3", 7, new HashSet<>(), null);
		
		File f;
		File[] mp3s;
		
		for(int i = 155 ; i <  155; i++) {
			f = new File("/Volumes/papiom/Datasets/Free Music Archive - FMA/fma_small/"+i);
			mp3s = f.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File f, String name) {
					return name.endsWith(".mp3");
				}
			});
			
			for(File mp3 : mp3s) {
				strat.store(mp3.getAbsolutePath(),"desc");
			}
		}
		
		String resource;
		List<OlafFingerprint> prints;
		OlafDBStorage db = OlafDBStorage.getInstance();
		
		db.entries(true);
		
		resource = rec;
		prints = strat.toFingerprints(resource);
		Random r = new Random();
		
		for(OlafFingerprint p : prints) {
			int tOffset = r.ints(-3, (4)).findFirst().getAsInt();
			OlafFingerprint copy = new OlafFingerprint(p.t1 + tOffset, p.f1,p.m1,p.t2,p.f2,p.m2,p.t3,p.f3,p.m3);
			long hash = copy.hash();
			db.addToQueryQueue(hash);
		}
		Map<Long,List<OlafDBHit>> matchAccumulator = new HashMap<>();
		db.processQueryQueue(matchAccumulator, 5);
		System.out.println("matches after modification of t1 " + matchAccumulator.size());
		System.out.println("Prints: " + prints.size());
	
		prints = strat.toFingerprints("/Users/joren/lmdbtest/013928_10s.ogg");
		for(OlafFingerprint p : prints) {
			long hash = p.hash();
			db.addToQueryQueue(hash);
		}
		matchAccumulator.clear();
		db.processQueryQueue(matchAccumulator, 1);
		System.out.println("matches 10s offset " + matchAccumulator.size());
		System.out.println("Prints: " + prints.size());
		
		f = new File("/Volumes/papiom/Datasets/Free Music Archive - FMA/fma_small/014");
		mp3s = f.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".mp3");
			}
		});
		
		int printcount = 0;
		for(File mp3 : mp3s) {
			resource = mp3.getAbsolutePath();
			prints = strat.toFingerprints(resource);
			for(OlafFingerprint p : prints) {
				long hash = p.hash();
				db.addToQueryQueue(hash);
			}
			System.out.println("Prints: " + prints.size() + " " + prints.size()/30.0);
			printcount += prints.size();
		}
		
		matchAccumulator.clear();
		db.processQueryQueue(matchAccumulator, 1);
		System.out.println("matches other mp3s " +  matchAccumulator.size());
		System.out.println("Prints: " + printcount);
	}
}
