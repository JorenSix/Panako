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
	       
