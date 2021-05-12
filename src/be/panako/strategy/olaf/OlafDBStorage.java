package be.panako.strategy.olaf;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lmdbjava.Cursor;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.GetOp;
import org.lmdbjava.SeekOp;
import org.lmdbjava.Stat;
import org.lmdbjava.Txn;

import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;

public class OlafDBStorage implements OlafStorage {
	
	/**
	 * The single instance of the storage.
	 */
	private static OlafDBStorage instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static OlafDBStorage getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new OlafDBStorage();
				}
			}
		}
		return instance;
	}
	
	
	final Dbi<ByteBuffer> fingerprints;
	final Dbi<ByteBuffer> resourceMap;
	final Env<ByteBuffer> env;
	
	final List<long[]> storeQueue;
	final List<long[]> deleteQueue;
	final List<Long> queryQueue;
	
	public OlafDBStorage() {
		String folder = Config.get(Key.OLAF_LMDB_FOLDER);
		if(!new File(folder).exists()) {
			FileUtils.mkdirs(folder);
		}
		if(!new File(folder).exists()) {
			throw new RuntimeException("Could not create LMDB folder: " + folder);
		}
		File path = new File(folder);
		
		env =  org.lmdbjava.Env.create()
        .setMapSize(1024l * 1024l * 1024l * 100l)
        .setMaxDbs(2)
        .setMaxReaders(2)
        .open(path);
		
		final String fingerprintName = "olaf_fingerprints";
		fingerprints = env.openDbi(fingerprintName, DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY, DbiFlags.MDB_DUPSORT, DbiFlags.MDB_DUPFIXED);
		
		final String resourceName = "olaf_resource_map";		
		resourceMap = env.openDbi(resourceName,DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY);
		
		storeQueue = new ArrayList<>();
		deleteQueue = new ArrayList<>();
		queryQueue = new ArrayList<>();
	}
	
	public void close() {
		env.close();
	}
	
	
	/* (non-Javadoc)
	 * @see be.panako.strategy.olaf.OlafStorage#storeMetadata(long, java.lang.String, float, int)
	 */
	@Override
	public void storeMetadata(long resourceID,String resourcePath,float duration, int fingerprints) {
		final ByteBuffer key = ByteBuffer.allocateDirect(8);
		byte[] resourcePathBytes = resourcePath.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		final ByteBuffer val = ByteBuffer.allocateDirect(resourcePathBytes.length + 16); 
		key.putLong(resourceID).flip();
		
		val.putFloat(duration);
		val.putInt(fingerprints);
	    val.put(resourcePathBytes).flip();
	    
	    resourceMap.put(key, val);
	}
	
	@Override
	public OlafResourceMetadata getMetadata(long resourceID) {
		
		OlafResourceMetadata metadata = null;
	    
		try (Txn<ByteBuffer> txn = env.txnRead()) {
			final ByteBuffer key = ByteBuffer.allocateDirect(8);
			key.putLong(resourceID).flip();
			
		    final ByteBuffer found = resourceMap.get(txn, key);
		    
		    if(found != null) {
		    	metadata = new OlafResourceMetadata();
		    	final ByteBuffer fetchedVal = txn.val();
		    	metadata.duration = fetchedVal.getFloat();
		    	metadata.numFingerprints = fetchedVal.getInt();
		    	metadata.path = StandardCharsets.UTF_8.decode(fetchedVal).toString();
		    	metadata.identifier =(int) resourceID;
		    }
		    txn.close();
		}
		
		return metadata;    
	}
	
	/* (non-Javadoc)
	 * @see be.panako.strategy.olaf.OlafStorage#addToStoreQueue(long, int, int)
	 */
	@Override
	public void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1) {
		long[] data = {fingerprintHash,resourceIdentifier,t1};
		storeQueue.add(data);
	}
	
	/* (non-Javadoc)
	 * @see be.panako.strategy.olaf.OlafStorage#processStoreQueue()
	 */
	@Override	
	public void processStoreQueue() {
		if(storeQueue.isEmpty())
			return;
		
		try (Txn<ByteBuffer> txn = env.txnWrite()) {
			
			final ByteBuffer key = ByteBuffer.allocateDirect(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		    final ByteBuffer val = ByteBuffer.allocateDirect(8);
		    
		      // A cursor always belongs to a particular Dbi.
		      final Cursor<ByteBuffer> c = fingerprints.openCursor(txn);
		      
		      for(long[] data : storeQueue) {
		    	  key.putLong(data[0]).flip();
		    	  val.putInt((int) data[1]).putInt((int) data[2]).flip();
		    	  
		    	  c.put(key, val);

		    	  key.clear();
		    	  val.clear();
		      }  
		            
		      c.close();
		      txn.commit();
		      storeQueue.clear();
		    }catch (Exception e) {
		    	e.printStackTrace();
		    }
	}
	
	public void clearStoreQueue() {
		storeQueue.clear();
	}
	
	public void addToDeleteQueue(long key, int val1, int val2) {
		long[] data = {key,val1,val2};
		deleteQueue.add(data);
	}
	
	public void processDeleteQueue() {
		if(deleteQueue.isEmpty())
			return;
		
		try (Txn<ByteBuffer> txn = env.txnWrite()) {
			
			final ByteBuffer key = ByteBuffer.allocateDirect(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		    final ByteBuffer val = ByteBuffer.allocateDirect(8);
		    
		      // A cursor always belongs to a particular Dbi.
		      final Cursor<ByteBuffer> c = fingerprints.openCursor(txn);
		      
		      for(long[] data : deleteQueue) {
		    	  key.putLong(data[0]).flip();
		    	  val.putInt((int) data[1]).putInt((int) data[2]).flip();
		    	  if(c.get(key,val,SeekOp.MDB_GET_BOTH)) {
		    		  c.delete();
		    	  }
		    	  key.clear();
		    	  val.clear();
		      }  
		      
		      c.close();
		      txn.commit();
		      deleteQueue.clear();
		    }catch (Exception e) {
		    	e.printStackTrace();
		    }
	}
	
	public void addToQueryQueue(long queryHash) {
		queryQueue.add(queryHash);
	}
	
	public static class OlafDBHit{
		public final long originalHash;
		public final long matchedNearHash;
		
		public final int t;
		public final int resourceID;
		
		public OlafDBHit(long originalHash, long matchedNearHash,long t, long resourceID) {
			this.originalHash = originalHash;
			this.matchedNearHash = matchedNearHash;
			this.t=(int)t;
			this.resourceID=(int)resourceID;
		}
		
	}
	
	public void processQueryQueue(Map<Long,List<OlafDBHit>> matchAccumulator,int range) {
		processQueryQueue(matchAccumulator, range, new HashSet<Integer>());
	}
	
	public void processQueryQueue(Map<Long,List<OlafDBHit>> matchAccumulator,int range,Set<Integer> resourcesToAvoid) {
		
		if(queryQueue.isEmpty())
			return;
		
		try (Txn<ByteBuffer> txn = env.txnRead()) {
			  // A cursor always belongs to a particular Dbi.
		      final Cursor<ByteBuffer> c = fingerprints.openCursor(txn);
		      
		      final ByteBuffer keyBuffer = ByteBuffer.allocateDirect(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		      
		      for(long originalKey : queryQueue) {
		    	  
		    	  long startKey = originalKey - range;
		    	  long stopKey = originalKey + range;
		    	  
		    	  keyBuffer.putLong(startKey).flip();
			      
			      if(c.get(keyBuffer, GetOp.MDB_SET_RANGE)) {
			    	  long fingerprintHash =  c.key().order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
		    		  long resourceID = c.val().getInt();
				      long t = c.val().getInt();
				      
				      //System.out.printf("Direct match: %d id %d t1 %d\n",fingerprintHash , resourceID,t);
				      
				      if(fingerprintHash <= stopKey) {
				    	  if(!resourcesToAvoid.contains((int) resourceID)) {
				    		  if(!matchAccumulator.containsKey(originalKey))
				    			  matchAccumulator.put(originalKey,new ArrayList<OlafDBHit>());
				    		  matchAccumulator.get(originalKey).add(new OlafDBHit(originalKey, fingerprintHash, t, resourceID));
				    	  }
				   
					      while(true) {
					    	  while(c.seek(SeekOp.MDB_NEXT_DUP)) {
					    		  fingerprintHash =  c.key().order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
					    		  resourceID = c.val().getInt();
							      t = c.val().getInt();
							      //System.out.printf("Dup Hash: %d id %d t1 %d\n",fingerprintHash , resourceID,t);
							      
							      if(!resourcesToAvoid.contains((int) resourceID)) {
						    		  if(!matchAccumulator.containsKey(originalKey))
						    			  matchAccumulator.put(originalKey,new ArrayList<OlafDBHit>());
						    		  matchAccumulator.get(originalKey).add(new OlafDBHit(originalKey, fingerprintHash, t, resourceID));
						    	  }
						      }
						      
						      if(c.seek(SeekOp.MDB_NEXT)) {
						    	  fingerprintHash =  c.key().order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
						    	  resourceID = c.val().getInt();
							      t = c.val().getInt();
							      if(fingerprintHash > stopKey)
							    	  break;
							      
							      //System.out.printf("Next Hash: %d id %d t1 %d\n",fingerprintHash , resourceID,t);
							      
							      
							      if(!resourcesToAvoid.contains((int) resourceID)) {
						    		  if(!matchAccumulator.containsKey(originalKey))
						    			  matchAccumulator.put(originalKey,new ArrayList<OlafDBHit>());
						    		  matchAccumulator.get(originalKey).add(new OlafDBHit(originalKey, fingerprintHash, t, resourceID));
						    	  }
						      } else {
						    	  //no next found, end of db
						    	  break;
						      }
					      }
				      }
			      }
		      }
		      c.close();
		      txn.commit();
		      queryQueue.clear();
		}
		
	}
	
	public long entries(boolean printStats){
		long entries = 0;
		final Stat stats;
	    try (Txn<ByteBuffer> txn = env.txnRead()) {
	      stats = fingerprints.stat(txn);
	      entries = stats.entries;
	      
	      if(printStats) {
	    	  
	    	  String folder = Config.get(Key.OLAF_LMDB_FOLDER);
	    	  String dbpath = FileUtils.combine(folder,"data.mdb");
	    	  long dbSizeInMB = new File(dbpath).length() / (1024 * 1024);
	    	  
		      System.out.printf("[MDB INDEX statistics]\n");
		      System.out.printf("=========================\n");
		      System.out.printf("> Size of database page:        %d\n", stats.pageSize);
		      System.out.printf("> Depth of the B-tree:          %d\n", stats.depth);
		      System.out.printf("> Number of items in databases: %d\n", stats.entries);
		      System.out.printf("> File size of the databases:   %dMB\n", dbSizeInMB);
		      System.out.printf("=========================\n\n");
	      }
	      txn.close();     
	    }
	    
	    try (Txn<ByteBuffer> txn = env.txnRead()) {
			  // A cursor always belongs to a particular Dbi.
		      final Cursor<ByteBuffer> c = resourceMap.openCursor(txn);
		      
		      final ByteBuffer keyBuffer = ByteBuffer.allocateDirect(8);
		      keyBuffer.putLong(0l).flip();
		      
		      double totalDuration = 0;
		      long totalPrints = 0;
		      long totalResources = 0;
		      
		      double maxPrintsPerSecond = 0;
		      String maxPrintsPerSecondPath = "";
		      double minPrintsPerSecond = 100000;
		      String minPrintsPerSecondPath = "";
		      
		      
		      
		      while(c.seek(SeekOp.MDB_NEXT)) {
		    	  
		    	 //long resourceIdentifier =  c.key().getLong();
			     float duration = c.val().getFloat();
			     int numFingerprints =  c.val().getInt();
			     float printsPerSecond =  (float) numFingerprints / duration;
			     String path = StandardCharsets.UTF_8.decode(c.val()).toString();
			     
			     if(printsPerSecond > maxPrintsPerSecond) {
			    	 maxPrintsPerSecond = printsPerSecond;
			    	 maxPrintsPerSecondPath = path;
			     }
			     
			     if(printsPerSecond < minPrintsPerSecond) {
			    	 minPrintsPerSecond = printsPerSecond;
			    	 minPrintsPerSecondPath = path;
			     }
		    	 
			     //System.out.printf("> %13d   %.3fs   %7dfp   %5.1ffp/s   '%s'\n",resourceIdentifier,duration,numFingerprints,printsPerSecond,path);
			     
			     totalDuration += duration;
			     totalPrints += numFingerprints;
			     totalResources++;
		      }
		      
		      double avgPrintsPerSecond =   totalPrints / totalDuration;
		      //System.out.printf("=========================\n\n");
		      
		      System.out.printf("[MDB INDEX TOTALS]\n");
		      System.out.printf("=========================\n");
		      System.out.printf("> %d audio files \n",totalResources);
		      System.out.printf("> %.3f seconds of audio\n",totalDuration);
		      System.out.printf("> %d fingerprint hashes \n",totalPrints);
		      System.out.printf("=========================\n\n");
		      
		      System.out.printf("[MDB INDEX INFO]\n");
		      System.out.printf("=========================\n");
		      System.out.printf("> Avg prints per second: %5.1ffp/s \n",avgPrintsPerSecond);
		      System.out.printf("> Min prints per second: %5.1ffp/s '%s'\n",minPrintsPerSecond,minPrintsPerSecondPath);
		      System.out.printf("> Max prints per second: %5.1ffp/s '%s'\n",maxPrintsPerSecond,maxPrintsPerSecondPath);
		      System.out.printf("=========================\n\n");
		      
		      c.close();
		      txn.close();
	    }
	    
	    
	    return entries;
	}

	public void deleteMetadata(long resourceID) {  
		
		try (Txn<ByteBuffer> txn = env.txnWrite()) {
			
			final ByteBuffer key = ByteBuffer.allocateDirect(8);
			key.putLong(resourceID).flip();
			
			final ByteBuffer found = resourceMap.get(txn, key);
			if(found !=null) {
				resourceMap.delete(txn, key);
			}else {
				//not found, not deleted
			}
			
		    txn.commit();
		 
	    }catch (Exception e) {
	    	e.printStackTrace();
	    }
		
	}

}
