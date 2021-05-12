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
	
	//query info
	public final String queryPath;	
	public final double queryStart;
	public final double queryStop;
	
	//ref info
	public final String refIdentifier;
	public final String refPath;
	public final double refStart;
	public final double refStop;
	
	//match info
	public final double score;
	public final double timeFactor;
	public final double frequencyFactor;
	public final double scoreVariance;
	
	
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
	public QueryResult(String queryPath,double queryStart,double queryStop, String refPath, String refIdentifier, double refStart, double refStop, double score,double timeFactor, double frequencyFactor,double scoreVariance){
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
		
		this.scoreVariance = scoreVariance;
	}
	
	
	
	public QueryResult(String query, double queryStart, double queryStop, String refIdentifier, String refPath, int score,
			double refStart, double timeFactor, double frequencyFactor) {
		this(query,queryStart,queryStop,refPath,refIdentifier,refStart,0,score,timeFactor,frequencyFactor,0);
	}



	public QueryResult(String queryPath,double queryStart,double queryStop, String refPath, String refIdentifier, double refStart, double refStop, double score,double timeFactor, double frequencyFactor){
		this( queryPath, queryStart, queryStop,  refPath,  refIdentifier,  refStart,  refStop,  score, timeFactor,  frequencyFactor,0);
	}
		



	public static QueryResult emptyQueryResult(String query,double queryStart,double queryStop){
		return new QueryResult(query,queryStart,queryStop,null, null, -1, -1,-1,-1,-1,0);
	}
}
