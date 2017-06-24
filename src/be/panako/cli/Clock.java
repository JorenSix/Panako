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

package be.panako.cli;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Clock {

	private static class Waiter implements Runnable {
		
		private final int millisecondsToWait;
		public Waiter(int milliseconds){
			millisecondsToWait = milliseconds;
		}
		
	    @Override
	    public void run() {
	    	long nanoStart = System.nanoTime();
	        try {
				Thread.sleep(millisecondsToWait);
				System.out.println((System.nanoTime()-nanoStart)/1000000.0);
			} catch (InterruptedException e) {
				
			}
	    }
	};
	
	public static void main(String[] args) {
		 
	     ExecutorService es = Executors.newFixedThreadPool(50);
	     for(int i = 100 ; i < 13500 ; i+=200){
	    	 es.execute(new Waiter(i));
	     }
	     es.shutdownNow();
	     for(int i = 100 ; i < 13500 ; i+=200){
	    	 es.execute(new Waiter(i));
	     }
	}

}
