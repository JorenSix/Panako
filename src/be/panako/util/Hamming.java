package be.panako.strategy.chromaprint;

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

	 public static void main(String args[]) throws Exception {

	  int a = 3, b = 255;
	  System.out.println("hamming distance between " + a + " and " + b + " is " + d(a, b));
	 }

	}
	       
