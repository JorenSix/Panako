package be.panako.util;

import java.util.BitSet;

public class Hamming {

	 public static int d(int x, int y) {
	        int dist = 0;
	        int val = x ^ y;

	        // Count the number of set bits (Knuth's algorithm)
	        while (val != 0) {
	            ++dist;
	            val &= val - 1;
	        }

	        return dist;
	    }
	    
	    /**
	     * Returns Hamming distance between the two long integers.
	     */
	    public static int d(long x, long y) {
	        int dist = 0;
	        long val = x ^ y;

	        // Count the number of set bits (Knuth's algorithm)
	        while (val != 0) {
	            ++dist;
	            val &= val - 1;
	        }

	        return dist;
	    }
	    
	    public static int d(BitSet x, BitSet y){
	    	
	    	long[] xLong = x.toLongArray();
	    	long[] yLong = y.toLongArray();
	    	
	    	int d = 0;
	    	for(int i = 0 ; i< xLong.length && i<yLong.length;i++){
	    		d += d(xLong[i],yLong[i]);
	    	}
	    	return d;
	    	
    		//clone the bit set x
    		//BitSet xored = (BitSet) x.clone();
    		//xor (modifies) the bit set
    		//xored.xor(y);
    		//return the number of 1's in the 
    		//xored set
    		//return xored.cardinality();	    	
	    }
	}
	       
