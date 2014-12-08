/***************************************************************************
*                                                                          *                     
* Panako - acoustic fingerprinting                                         *   
* Copyright (C) 2014 - Joren Six / IPEM                                    *   
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


package be.panako.strategy.ncteq;

import be.panako.util.Config;
import be.panako.util.Key;

public class NCteQHashFunction  {
	private final int shift;
	
	public NCteQHashFunction(){
		shift = Config.getInt(Key.NCTEQ_EVENT_POINT_FREQUENCY_DELTA_MAX) * Config.getInt(Key.NCTEQ_BINS_PER_OCTAVE)/1200;	
	}


	public int calculateHash(NCteQFingerprint fingerprint) {
		int f1 = fingerprint.f1;
		int f2 = fingerprint.f2;
		int f3 = fingerprint.f3;
		
		int t1 = fingerprint.t1;
		int t2 = fingerprint.t2;
		int t3 = fingerprint.t3;
				
		//For 6.375 octaves with 36 bins per octave every frequency bin is in [0-255] < 2^8
		//Every bin is 1200/36 = 33 cents
		//To allow deviation divide the frequency component by 16 so it lays in [0-16] < 2^5
		//Hash is designed to allow a 16% deviation in pitch. So pitch between [84%-116%] in cents: 
		//  1200*ln(0.84)/ln(2) = -302 cents
		//  1200*ln(1.16)/ln(2) = +257 cents
		//[-302,257] in bins = [-9,7] = 16 bins, 256 bins/16 bins = 16 steps
				
		//Bin f1 serves as an indication where f1 is.
		int binF1 = Math.round(f1/16f)  % (1<<4);//[0-15], 4bits
		//Bin F3 indicates where f3 is
		int binF3 = Math.round(f3/16f)  % (1<<4);//[0-15], 4bits
		
		
		
				
		//The delta components are preserved when pitch shifting, thanks to the constant Q.
		//They are also tempo information is changed
		//Each delta is within 1066 cents, with 36 bins per octave this means 31 bins
		//The delta can be positive or negative [-1066,1066] => [-1066*36/1200,1066*36/1200] = [-32,32] 
		//Map it to positive, so add 32 => [0,64], 6bits
				
		int deltaF1 = (f2 - f1 + shift) % (1<<6);//[0-63], 6bits 
		int deltaF2 = (f3 - f2 + shift) % (1<<6);//[0-63], 6bits
		
		//No direct delta t can be used, only a ratio between the two delta t values. 
		//The idea is that linear speed changes are supported then.
		//the ratio of times, a fractional percentage scaled to seven bits
		//E.g. if the sample rate is 44100 and steps of 1536 are taken,
		// and the minimum delta t = 34.8 milliseconds, max delta t = 1400 milliseconds
		// step in ms = 1536/44100/s * 1000 = 34.8ms so 
		//  min delta t is 1 steps, max delta t = 1400/34.8 = 40
		//  ratio is between (t2-t1) / (t3-t1) is
		// [1-40]/[2-80] => 68 different values, map to 64
		//
		//hash = Hash.new
		//(1..40).each do |small| 
		//  (2..80).each do |big|
		//	  hash[(small.to_f/big.to_f * 4).round] = true
		//  end
	    //end
	    //puts hash.keys.sort.join("\n")
	    //puts hash.size
		int ratio = Math.round(ratio(t1,t2,t3)*8) % (1<<4);//[0-15], 4bits
		
		
		//Adds one bit, info about which delta is the biggest
		int firstTimeDeltalargest = (t2-t1) > (t3-t2) ? 1 : 0;//[0-1]1bit
		
		// Adds info about the size of t3-t1
		// Time can deviate about 10%, the delta between t3 and t2
		// must be scaled
		// [68ms,2800ms] = [2-80]
		// [76,3080] (+10%) = [2,88]
		// [57,2520] (-10%] = [2,72]
		// to make [2,88] = [2,72] divide by 20 and round:
		// [0,round(4.4)] = [0,round(3.6)]
		int timeDeltaBin = Math.round((t3-t1)/20.0f) % (1<<2);//2bits
		
		//Total hash entropy is 4+4+6+6+4+1+2 = 26bits (about 64*10^6)
		/* old balanced (works well...)
		int hash = 0;
		hash += firstTimeDeltalargest;
		hash += binF1        * (1<<1);  //1
		hash += deltaF1      * (1<<6);  //1+5
		hash += deltaF2      * (1<<12); //1+5+6
		hash += ratio        * (1<<18); //1+5+6+6
		hash += timeDeltaBin * (1<<24); //1+5+6+6+6
		hash += binF3 * (1<<26); //1+5+6+6+6
		*/
		
		//new balanced
		//Hash entropy: 4+4+6+6+4+1+2 = 27bits +- 130*10^6
		int hash = 0;
		hash += binF1					* 1  	 ;//4bits		
		hash += binF3        			* (1<<4) ;//4bits (4)
		hash += deltaF1      			* (1<<8) ;//6bits (4+4)
		hash += deltaF2      			* (1<<14);//6bits (4+4+6)
		hash += ratio        			* (1<<20);//4bits (4+4+6+6)
		hash += firstTimeDeltalargest 	* (1<<24);//1bit  (4+4+6+6+4)
		hash += timeDeltaBin 			* (1<<25);//2bits (4+4+6+6+4+1)
		
		return hash;
	}
	
	public float ratio(int t1,int t2,int t3){
		return (t2 - t1)/(float)(t3 - t1);
	}

	public String explain(NCteQFingerprint fingerprint) {
		
		int f1 = fingerprint.f1;
		int f2 = fingerprint.f2;
		int f3 = fingerprint.f3;
		
		int t1 = fingerprint.t1;
		int t2 = fingerprint.t2;
		int t3 = fingerprint.t3;
		
		int binF1 = Math.round(f1/16f)  % (1<<4);//[0-15], 4bits
		int binF3 = Math.round(f3/16f)  % (1<<4);//[0-15], 4bits
		int deltaF1 = (f2 - f1 + shift) % (1<<6);//[0-63], 6bits 
		int deltaF2 = (f3 - f2 + shift) % (1<<6);//[0-63], 6bits
		int ratio = Math.round(ratio(t1,t2,t3)*8) % (1<<4);//[0-15], 4bits
		int firstTimeDeltalargest = (t2-t1) > (t3-t2) ? 1 : 0;//[0-1]1bit
		int timeDeltaBin = Math.round((t3-t1)/20.0f) % (1<<2);//2bits
		
		int hash = 0;
		hash += binF1					* 1  	 ;//4bits		
		hash += binF3        			* (1<<4) ;//4bits (4)
		hash += deltaF1      			* (1<<8) ;//6bits (4+4)
		hash += deltaF2      			* (1<<14);//6bits (4+4+6)
		hash += ratio        			* (1<<20);//4bits (4+4+6+6)
		hash += firstTimeDeltalargest 	* (1<<24);//1bit  (4+4+6+6+4)
		hash += timeDeltaBin 			* (1<<25);//2bits (4+4+6+6+4+1)
		
		String explanation = "";
		explanation += String.format("Hash %d\n",hash);
		explanation += String.format("  (t1,f1)           (%d,%d)\n",t1,f1);
		explanation += String.format("  (t2,f2)           (%d,%d)\n",t2,f2);
		explanation += String.format("  (t3,f3)           (%d,%d)\n",t3,f3);
		explanation += String.format("  about F1          %d\t(freq bin)\n", binF1);
		explanation += String.format("  about F3          %d\t(freq bin)\n", binF3);
		explanation += String.format("  f2-f1             %d\t(freq bin)\n", deltaF1);
		explanation += String.format("  f3-f2             %d\t(freq bin)\n", deltaF2);
		explanation += String.format("  (t2-t1)/(t3-t1)*8 %d\t(time ratio)\n", ratio);
		explanation += String.format("  t2-t1>t3-t2?      %d\t(bit)\n", firstTimeDeltalargest);
		explanation += String.format("  (t3-t1)/20.0      %d\t(time bin)\n", timeDeltaBin);
		
		return explanation;
	}

}
