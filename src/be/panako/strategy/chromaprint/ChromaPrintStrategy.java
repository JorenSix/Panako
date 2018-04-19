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

package be.panako.strategy.chromaprint;

import java.io.File;
import java.util.NavigableMap;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.FileUtils;


public class ChromaPrintStrategy extends Strategy{
	
	private final NavigableMap<Long, String> chromaPrintStore;
	private final NavigableMap<Integer, String> audioNameStore;
	private final DB db;
	
	public ChromaPrintStrategy() {
		String chomaprintDBFileName = "chormaprint.db";
		 File dbFile = new File(chomaprintDBFileName);
		 db = DBMaker.fileDB(dbFile)
					.closeOnJvmShutdown() // close the database automatically
					.make();
			
		 final String chromaPrintStoreName = "chroma_print_store";
		// The meta-data store.
		 chromaPrintStore = db.treeMap(chromaPrintStoreName)
				.keySerializer(Serializer.LONG)
				.valueSerializer(Serializer.STRING)
				.counterEnable() // enable size counter
				.createOrOpen();
		// The meta-data store.
		 final String audioStoreName = "audio_store";
		audioNameStore = db.treeMap(audioStoreName)
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.STRING)
				.counterEnable() // enable size counter
				.createOrOpen();
		
	}

	@Override
	public double store(String resource, String description) {
		ChromaPrintExtractor ex = new ChromaPrintExtractor(resource, null);
		ex.starExtraction();
		long print = ex.getHash();
		if(chromaPrintStore.containsKey(print)){
			System.out.println(new File(resource).getName()  + " is duplicate of " + chromaPrintStore.get(print));
		}
		int identifier = Integer.valueOf(resolve(resource));
		audioNameStore.put(identifier, description);
		
		return ex.getDuration();
	}

	@Override
	public void query(String query, Set<Integer> avoid, QueryResultHandler handler) {
		ChromaPrintExtractor ex = new ChromaPrintExtractor(query, null);
		ex.starExtraction();
		long print = ex.getHash();
		if(chromaPrintStore.containsKey(print)){
			System.out.println(new File(query).getName()  + " is duplicate of " + chromaPrintStore.get(print));
		}
	}

	@Override
	public void monitor(String query, Set<Integer> avoid, QueryResultHandler handler) {
		ChromaPrintExtractor ex = new ChromaPrintExtractor(query, null);
		ex.starExtraction();
		long print = ex.getHash();
		if(chromaPrintStore.containsKey(print)){
			System.out.println(new File(query).getName()  + " is duplicate of " + chromaPrintStore.get(print));
		}
		
	}

	@Override
	public boolean hasResource(String resource) {
		int resourceIdentifier = Integer.valueOf(resolve(resource));
		String desc = this.audioNameStore.get(resourceIdentifier);
		return new File(resource).getName().equals(desc);
	}

	@Override
	public boolean isStorageAvailable() {
		return true;
	}

	@Override
	public void printStorageStatistics() {
	}

	@Override
	public String resolve(String filename) {
		int identifier = FileUtils.getIdentifier(filename);
		return String.valueOf(identifier);
	}
}
