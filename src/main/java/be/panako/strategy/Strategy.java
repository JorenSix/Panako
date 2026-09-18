/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2022 - Joren Six / IPEM                             *
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

/**
 * The main interface to an acoustic fingerprinting strategy or algorithm. The main tasks are to store, query, delete audio files.
 */
public abstract class Strategy {

	/**
	 * Default constructor
	 */
	public  Strategy(){}

	/**
	 * Store an audio file in the data store. The name of the resource is used to extract a
	 * numerical identifier. The description is arbitrary.
	 * @param resource The audio resource.
	 * @param description An arbitrary description.
	 * @return The number of seconds of processed audio.
	 */
	public abstract double store(String resource, String description);

	/**
	 * Remove an audio file from the data store. The name of the resource is used to extract a
	 * numerical identifier.
	 * @param resource The path name of the audio resource.
	 * @return The duration of the audio in seconds.
	 */
	public abstract double delete(String resource);

	/**
	 * Query the index for matches.
	 * @param query The path name of the query audio file
	 * @param maxNumberOfResults The maximum results to return.
	 *                              E.g. 1, to only return the best result, 10 to return the best 10.
	 * @param avoid A set of identifiers to ignore in the result set.
	 *                 E.g. for deduplication it might be of interest to ignore the query itself
	 * @param handler A handler to process the results.
	 */
	public abstract void query(String query, int maxNumberOfResults,Set<Integer> avoid, QueryResultHandler handler);

	/**
	 * The query is chopped up in parts of a configurable length (e.g. 20 seconds). Each part is matched with the
	 * index.
	 * @param query The path name of the query audio file
	 * @param maxNumberOfResults The maximum results to return.
	 *                              E.g. 1, to only return the best result, 10 to return the best 10.
	 * @param avoid A set of identifiers to ignore in the result set.
	 *                 E.g. for deduplication it might be of interest to ignore the query itself
	 * @param handler A handler to process the results.
	 */
	public abstract void monitor(String query,int maxNumberOfResults,Set<Integer> avoid,QueryResultHandler handler);
	
	/**
	 * Are there fingerprints for this resource already stored in the database?
	 * @param resource The name of the resource.
	 * @return True if the resource is already treated. False otherwise.
	 */
	public abstract boolean hasResource(String resource);
	
	/**
	 * Check if the storage system is available.
	 *
	 * @return True if the storage is available, false otherwise.
	 */
	public abstract boolean isStorageAvailable();
	
	/**
	 * Print some storage statistics.
	 */
	public abstract void printStorageStatistics();
	

	private static Strategy strategy;

	/**
	 * Returns an instance of the configured strategy. Using reflection it creates a list of 'Strategy' implementers
	 * and checks if the configured strategy can be found. <br>
	 * E.g. for the configured value "Panako" there needs to be a "PanakoStrategy" class implementing Strategy.
	 *
	 * @return An instance of the strategy.
	 */
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
	
	private static String classToName(Class<? extends Strategy> c){
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


	/**
	 * Print the fingerprints to a human readable format or to an output compatible with
	 * Sonic Visualizer. Mainly used for caching or debugging.
	 *
	 * @param path                  the name of the file to resolve.
	 * @param sonicVisualizerOutput True if the output needs to be compatible with sonic visualizer
	 * @param printOnlyEPs
	 */
	public abstract void print(String path, boolean sonicVisualizerOutput, boolean printOnlyEPs);

	/**
	 * Generate a JSON representation of the fingerprints for a given audio file.
	 * Similar to print but returns structured JSON data instead of printing to stdout.
	 *
	 * @param path the name of the file to process.
	 * @return A JSON string representation of the fingerprints
	 */
	public abstract String toJson(String path);

	/**
	 * Clear <b>all</b> information from the key value store
	 */
	public abstract void clear();

	/**
	 * Return the meta-data for a stored resource
	 * @param path The path of the resource
	 * @return The meta-data string
	 */
	public abstract String metadata(String path);

}
