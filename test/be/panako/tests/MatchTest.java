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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;

import org.junit.Test;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;

public class MatchTest {
	
	@Test
	public void storeAndQueryTest(){
		String smallDatasetReferenceFolder = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/reference";
		String[] ref = new File(smallDatasetReferenceFolder).list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("mp3") && name.startsWith("189211");
			}
		});
		
		Strategy strategy = Strategy.getInstance();
		for(String refFile : ref){
			String resource = new File(smallDatasetReferenceFolder,refFile).getAbsolutePath();
			if(!strategy.hasResource(resource)){
				strategy.store(resource, refFile);
			}
		}
		
		String queriesFolder = "/media/data/datasets/Fingerprinting datasets/Datasets/small_dataset/queries";
		String[] queries = new File(queriesFolder).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("mp3") && name.startsWith("189211");
			}
		});
		
		for(final String queryFile : queries){
			String resource = new File(queriesFolder,queryFile).getAbsolutePath();
			
			strategy.query(resource,3,new HashSet<Integer>(),new QueryResultHandler() {
				
				@Override
				public void handleQueryResult(QueryResult result) {
					int startExpected = Integer.valueOf(queryFile.split("_")[1].split("-")[0].replace("s", ""));
					System.out.println(result.refPath + " " + queryFile);
					System.out.println(Math.round(result.queryStart));
					assertEquals("Found an unexpected start of query for " + queryFile, startExpected, Math.round(result.queryStart));
				}
				
				@Override
				public void handleEmptyResult(QueryResult result) {
					assertTrue("Result should not be empty!", false);
				}
			});
		}
	}

}
