package be.panako.strategy.rafs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.redisson.core.RMap;

import be.tarsos.mih.storage.MIHStorage;

public class RedisStorage implements MIHStorage {
	
	private final List<RMap<Integer, long[]>> hashtables;
	private final RedissonClient redisson;
	
	public RedisStorage(int numberOfTables,String name){
		Config config = new Config();
		config.useSingleServer().setAddress("127.0.0.1:6379");

		redisson = Redisson.create(config);
		hashtables = new ArrayList<>();
		for(int i = 0 ; i < numberOfTables ; i++){
			RMap<Integer, long[]> map = redisson.getMap("fprint" + i + "" + name );
			long[] values = {0l};
			map.put(0,values);
			
			hashtables.add(map);
		}
	}
	@Override
	public boolean containsKey(int hashtableIndex, int key) {
		return hashtables.get(hashtableIndex).containsKey(key);
	}

	@Override
	public long[] put(int hashtableIndex, int key, long[] newList) {
		return hashtables.get(hashtableIndex).put(key,newList);
	}

	@Override
	public long[] get(int hashtableIndex, int key) {
		return hashtables.get(hashtableIndex).get(key);
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
