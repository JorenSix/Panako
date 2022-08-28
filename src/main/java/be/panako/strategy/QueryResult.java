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

/**
 * A data class representing a query result
 */
public class QueryResult {

	/**
	 * The path of the query
	 */
	public final String queryPath;
	/**
	 * The start of the match in the query, in seconds.
	 */
	public final double queryStart;
	/**
	 * The end of the match in the query, in seconds.
	 */
	public final double queryStop;
	
	//ref info
	/**
	 * The internal identifier of the matching audio
	 */
	public final String refIdentifier;
	/**
	 * The path of the matching audio
	 */
	public final String refPath;
	/**
	 * The start of the match in the reference audio, in seconds.
	 */
	public final double refStart;
	/**
	 * The end of the match in the reference audio, in seconds.
	 */
	public final double refStop;
	
	//match info
	/**
	 * The start of the match in the reference audio, in seconds.
	 */
	public final double score;
	/**
	 * The amount of time stretching that needs to be applied to the query to match the reference audio.
	 *
	 */
	public final double timeFactor;
	/**
	 * The amount of pitch shifting that needs to be applied to the query to match the reference audio.
	 * A percentage which is expected to be between 0.8 and 1.2
	 */
	public final double frequencyFactor;
	/**
	 * If the match has a duration of 10 seconds but only the first and last second contain all the matches,
	 * this indicates an unreliable match. 2/10 = 0.2 would be the percentage.
	 * If all seconds also have matches then it is 1.0.
	 */
	public final double percentOfSecondsWithMatches;
	
	/**
	 * Create a new query result data class.
	 *
	 * @param queryPath
	 * 			  The path of a query
	 * @param queryStart
	 *            The start time offset in the query. The match is found at
	 *            <code>queryTimeOffsetStart+time</code>.
	 * @param queryStop
	 *            The stop time offset of the query.
	 * @param refPath
	 *            The path of the reference stored in the database.
	 * @param refIdentifier
	 *            The internal identifier of the matched audio
	 * @param score
	 *            The score for the match
	 * @param refStart
	 *            The starting position in the matched audio, in seconds.
	 * @param refStop
	 *            The stopping position in the matched audio, in seconds.
	 * @param timeFactor
	 *            The factor (percentage) of change in time. 110 means 10%
	 *            speedup compared to the reference. 90 means 10% slower than
	 *            reference.
	 * @param frequencyFactor
	 *            The factor (percentage) of change in frequency. 110 means 10%
	 *            higher frequency compared to the reference. 90 means a 10%
	 *            lower frequency.
	 * @param percentOfSecondsWithMatches
	 *      	  If the match has a duration of 10 seconds but only the first and last second contain all the matches,
	 * 			  this indicates an unreliable match. 2/10 = 0.2 would be the percentage.
	 * 			  If all seconds also have matches then it is 1.0.
	 */
	public QueryResult(String queryPath,
					   double queryStart,
					   double queryStop,
					   String refPath,
					   String refIdentifier,
					   double refStart,
					   double refStop,
					   double score,
					   double timeFactor,
					   double frequencyFactor,
					   double percentOfSecondsWithMatches){
		this.queryPath = queryPath;
		this.queryStart = queryStart;
		this.queryStop = queryStop;
		
		this.refPath = refPath;
		this.refIdentifier = refIdentifier;
		this.refStart = refStart;
		this.refStop = refStop;
		
		this.score = score;
		this.timeFactor=timeFactor;
		this.frequencyFactor = frequencyFactor;
		
		this.percentOfSecondsWithMatches = percentOfSecondsWithMatches;
	}

	/**
	 * Create a new query result indicating an empty match.
	 * @param query The query
	 * @param queryStart  The start time offset in the query. The match is found at <code>queryTimeOffsetStart+time</code>.
	 * @param queryStop The stop time offset of the query.
	 * @return  An empty query result.
	 */
	public static QueryResult emptyQueryResult(String query,double queryStart,double queryStop){
		return new QueryResult(query,queryStart,queryStop,null, null, -1, -1,-1,-1,-1,0);
	}
}
