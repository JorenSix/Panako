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


package be.panako.strategy;

import java.util.Set;

import org.reflections.Reflections;

import be.panako.util.Config;
import be.panako.util.Key;

public abstract class Strategy {
	
	
	/**
	 * Store an audio file in the data store. The name of the resource is used to extract a
	 * numerical identifier. The description is arbitrary.
	 * @param resource The audio resource.
	 * @param description An arbitrary description.
	 * @return The number of seconds of processed audio.
	 */
	public abstract double store(String resource, String description);
	
	public abstract void query(String query, int maxNumberOfResults,Set<Integer> avoid, QueryResultHandler handler);
	
	public abstract void monitor(String query,int maxNumberOfReqults,Set<Integer> avoid,QueryResultHandler handler);
	
	/**
	 * Are there fingerprints for this resource already stored in the database?
	 * @param resource The name of the resource.
	 * @return True if the resource is already treated. False otherwise.
	 */
	public abstract boolean hasResource(String resource);
	
	/**
	 * 
	 * @return True if the storage is available, false otherwise.
	 */
	public abstract boolean isStorageAvailable();
	
	/**
	 * Print some storage statistics.
	 */
	public abstract void printStorageStatistics();
	
	/**
	 * Checks the configuration and returns a strategy.
	 * @return An instance of the strategy.
	 */	
	private static Strategy strategy;
	public static Strategy getInstance(){
		if(strategy == null){
			Reflections reflections = new Reflections("be.panako.strategy");
			Set<Class<? extends Strategy>> modules =   reflections.getSubTypesOf(be.panako.strategy.Strategy.class);
			String configuredStrategyName = Config.get(Key.STRATEGY);
			
			for(Class<? extends Strategy> module : modules) {
				try {
					if(configuredStrategyName.equalsIgnoreCase(Strategy.classToName(module))) {
						strategy = ((Strategy) module.getDeclaredConstructor().newInstance());
						break;
					}
				} catch (Exception e) {
					//should not happen, instantiation should not be a problem
					e.printStackTrace();
				}
			}
		}
		return strategy;
	}
	
	public static String classToName(Class<? extends Strategy> c){
		//fully qualified name
		String name = c.getCanonicalName();
		//unqualified name
		name = name.substring(name.lastIndexOf('.')+1);
		//lower case first letter
		name = name.substring(0,1).toLowerCase() + name.substring(1);
		
		return name.replace("Strategy", "");
	}

	/**
	 * Returns an internal identifier, probably an integer, for a given filename. 
	 * @param filename the name of the file to resolve.
	 * @return An internal identifier, probably an integer, for a given filename.
	 */
	public abstract String resolve(String filename);

	
	public void print(String path, boolean sonicVisualizerOutput) {		
	}

	/**
	 * Clear al information from the key value store
	 */
	public abstract void clear();
}
