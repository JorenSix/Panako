package be.panako.strategy.nfft.storage.redisson;

import java.util.ArrayList;
import java.util.List;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.redisson.core.RMap;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.strategy.nfft.storage.NFFTFingerprintQueryMatch;
import be.panako.strategy.nfft.storage.Storage;

public class NFFTRedisStorage implements Storage {
	
	/**
	 * The single instance of the storage.
	 */
	private static Storage instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static Storage getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new NFFTRedisStorage();
				}
			}
		}
		return instance;
	}
	
	private final RMap<Integer, List<Integer>> fingerprintMap;
	private final RMap<Integer, String> metaDataMap;
	
	public NFFTRedisStorage() {
		Config config = new Config();
		config.useSingleServer().setAddress("127.0.0.1:6379");
		//config.useSingleServer().setAddress("157.193.92.74:6379");

		RedissonClient redisson = Redisson.create(config);
		fingerprintMap = redisson.getMap("integerMap");
		metaDataMap = redisson.getMap("descriptionMap");
	}

	@Override
	public void addAudio(int identifier, String description) {
		metaDataMap.put(identifier, description);
	}

	@Override
	public void audioObjectAdded(int numberOfSeconds) {
		
	}

	@Override
	public int getNumberOfFingerprints() {
		return fingerprintMap.size();
	}

	@Override
	public String getAudioDescription(int identifier) {
		return metaDataMap.get(identifier);
	}

	@Override
	public int getNumberOfAudioObjects() {
		return metaDataMap.size();
	}

	@Override
	public double getNumberOfSeconds() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasDescription(String description) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public float addFingerprint(int identifier, int time, int landmarkHash) {
		List<Integer> list;
		if(fingerprintMap.containsKey(landmarkHash)){
			list = fingerprintMap.get(landmarkHash);
		}else{
			list = new ArrayList<Integer>();
			
		}
		list.add(identifier);
		list.add(time);
		fingerprintMap.put(landmarkHash,list);
		return 0;
	}

	@Override
	public List<NFFTFingerprintQueryMatch> getMatches(
			List<NFFTFingerprint> fingerprints, int size) {
		// TODO Auto-generated method stub
		return null;
	}

}
