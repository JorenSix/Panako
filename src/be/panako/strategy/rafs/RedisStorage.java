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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RListMultimap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import be.tarsos.mih.storage.MIHStorage;

public class RedisStorage implements MIHStorage {
	
	private final List<RListMultimap<Integer, Long>> hashtables;
	private final RedissonClient redisson;
	
	public RedisStorage(int numberOfTables,String name){
		Config config = new Config();
		config.useSingleServer().setAddress("127.0.0.1:6379");

		redisson = Redisson.create(config);
		hashtables = new ArrayList<>();
		for(int i = 0 ; i < numberOfTables ; i++){
			RListMultimap<Integer, Long> map = redisson.getListMultimap("fingerprint_" + i + "" + name);
			hashtables.add(map);
		}
	}
	@Override
	public boolean containsKey(int hashtableIndex, int key) {
		return hashtables.get(hashtableIndex).containsKey(key);
	}

	@Override
	public long[] put(int hashtableIndex, int key, long[] newArray) {
		RList<Long> currentList = hashtables.get(hashtableIndex).get(key);
		//append the current list with the new values
		for(int i = currentList.size(); i <newArray.length ; i++){
			hashtables.get(hashtableIndex).put(key,newArray[i]);
		}		
		return newArray;
	}

	@Override
	public long[] get(int hashtableIndex, int key) {
		RList<Long> values = hashtables.get(hashtableIndex).get(key);
		long[] asLongArray = new long[values.size()];
		for(int i = 0 ; i<asLongArray.length ; i++){
			asLongArray[i] = values.get(i);
		}
		return asLongArray;
	}

	@Override
	public int size(int hashtableIndex) {
		return hashtables.get(hashtableIndex).size();
	}

	@Override
	public Set<Integer> getKeys(int hashtableIndex) {
		return hashtables.get(hashtableIndex).keySet();
	}

	@Override
	public void close() {
		redisson.shutdown();
	}

}
