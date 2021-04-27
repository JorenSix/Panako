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




package be.panako.strategy;

public class QueryResult {
	
	public final String identifier;
	public final String description;
	
	public final String query;
	
	public final double score;
	public final double time;
	public final double queryTimeOffsetStart;
	public final double queryTimeOffsetStop;
	public final double timeFactor;
	public final double frequencyFactor;
	

	
	/**
	 * @param queryTimeOffsetStart
	 *            The start time offset in the query. The match is found at
	 *            <code>queryTimeOffsetStart+time</code>.
	 * @param queryTimeOffsetStop
	 *            The stop time offset of the query.
	 * @param identifier
	 *            The internal identifier of the matched audio
	 * @param description
	 *            The meta-data, description of the matched audio
	 * @param score
	 *            The score for the match
	 * @param time
	 *            The starting position in the matched audio, in seconds.
	 * @param timeFactor
	 *            The factor (percentage) of change in time. 110 means 10%
	 *            speedup compared to the reference. 90 means 10% slower than
	 *            reference.
	 * @param frequencyFactor
	 *            The factor (percentage) of change in frequency. 110 means 10%
	 *            higher frequency compared to the reference. 90 means a 10%
	 *            lower frequency.
	 */
	public QueryResult(String query,double queryTimeOffsetStart,double queryTimeOffsetStop, String identifier, String description, double score, double time,double timeFactor, double frequencyFactor){
		this.queryTimeOffsetStart = queryTimeOffsetStart;
		this.queryTimeOffsetStop = queryTimeOffsetStop;
		this.identifier = identifier;
		this.description = description;
		this.score = score;
		this.time = time;
		this.timeFactor=timeFactor;
		this.frequencyFactor = frequencyFactor;
		this.query = query;
	}
	
	public static QueryResult emptyQueryResult(String query,double queryTimeOffsetStart,double queryTimeOffsetStop){
		return new QueryResult(query,queryTimeOffsetStart,queryTimeOffsetStop,null, null, -1, -1,-1,-1);
	}
}
