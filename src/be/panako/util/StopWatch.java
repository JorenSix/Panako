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



package be.panako.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StopWatch counts the ticks (ns) passed between initialization and invocation
 * of ticksPassed.
 * 
 * @author Joren Six
 */
public final class StopWatch {

	/**
	 * Number of ticks between start and stop in ns (10^-9 s).
	 */
	private transient long ticks;

	/**
	 * Create and start the stop watch.
	 */
	public StopWatch() {
		ticks = System.nanoTime();
	}

	/**
	 * @return The number of ticks passed between initialization and the call to
	 *         <code>ticksPassed</code>. In milliseconds or 10^-3 seconds.
	 */
	public long ticksPassed() {
		return (long) timePassed(TimeUnit.MILLISECONDS);
	}

	/**
	 * @return The number of ticks passed between initialization and the call to
	 *         <code>ticksPassed</code>. In nanoseconds or 10^-9 seconds.
	 */
	public long nanoTicksPassed() {
		return Math.abs(System.nanoTime() - ticks);
	}

	/**
	 * Calculates and returns the time passed in the requested unit.
	 * @param unit The requested time unit.
	 * @return The time passed in the requested unit.
	 */
	public double timePassed(TimeUnit unit){
		return unit.convert(nanoTicksPassed(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Starts or restarts the watch.
	 */
	public void start() {
		ticks = System.nanoTime();
	}

	@Override
	public String toString() {
		return ticksPassed() + "ms";
	}
	
	/**
	 * Returns a 24h time string based on an input resource. E.g. 
	 * "20120325-235950" + 25 seconds would return "00:00:15"  
	 * @param inputResource The name of the input resource with a date and time part. E.g. "20120325-235950.wav"
	 * @param currentSeconds The number of seconds to add to the time part.
	 * @return E.g. "20120325-235950" + 25 seconds would return "00:00:15" 
	 */
	public static String toTime(String inputResource,int currentSeconds){
		Pattern p = Pattern.compile(".*(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)-(\\d\\d)(\\d\\d)(\\d\\d).*");
		Matcher m = p.matcher(inputResource);
		int secondsOffset = 0;
		if(m.matches()){
			int hours = Integer.valueOf(m.group(4));
			int minutes = Integer.valueOf(m.group(5));
			int seconds = Integer.valueOf(m.group(6));
			
			secondsOffset = hours * 60 * 60 + minutes * 60 + seconds;
		}
		int totalSeconds = currentSeconds + secondsOffset;
		totalSeconds = totalSeconds % (24 * 3600);
		
		int hours = (int) TimeUnit.HOURS.convert(totalSeconds, TimeUnit.SECONDS);
		int minutes = (int) TimeUnit.MINUTES.convert(totalSeconds - hours * 3600, TimeUnit.SECONDS);
		int seconds = (int) totalSeconds - hours * 3600 - minutes * 60;
		
		String result = String.format("%02d:%02d:%02d", hours,minutes,seconds);			
		return result;		
	}

	public String formattedToString() {
		long ticksPassed = ticksPassed();
		long nanoTicksPassed = nanoTicksPassed();
		final String formatString;
		final double value;
		if (ticksPassed >= 1000) {
			formatString = "%.2f s";
			value = ticksPassed / 1000.0;
		} else if (ticksPassed >= 1) {
			formatString = "%.2f ms";
			value = ticksPassed;
		} else if (nanoTicksPassed >= 1000) {
			formatString = "%.2f µs";
			value = nanoTicksPassed / 1000.0;
		} else {
			formatString = "%.2f ns";
			value = nanoTicksPassed;
		}
		return String.format(Locale.US, formatString, value);
	}
	
	public static String formattedToString(double secondsPassed){
		int hours = (int) TimeUnit.HOURS.convert(secondsPassed, TimeUnit.SECONDS);
		int minutes = (int) TimeUnit.MINUTES.convert(secondsPassed - hours * 3600, TimeUnit.SECONDS);
		int seconds = (int) secondsPassed - hours * 3600 - minutes * 60;
		int milliSeconds = (int) ((secondsPassed - (int) secondsPassed) * 1000);
		String result ="";
		if(hours > 0){
			result += hours + "h:";
		}
		if(minutes > 0 || !result.isEmpty()){
			result += minutes + "m:";
		}
		if(seconds > 0 || !result.isEmpty()){
			result += seconds + "s";
		}
		if(milliSeconds > 0){
			result += ":" + milliSeconds  + "ms";
		}
		return result;
	} 
}
