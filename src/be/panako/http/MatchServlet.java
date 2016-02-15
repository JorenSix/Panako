/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2015 - Joren Six / IPEM                             *
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


package be.panako.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;
import be.panako.util.FileUtils;


public class MatchServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2720842983609869891L;
	
	private final NFFTStrategy strategy;
	public MatchServlet(){
		strategy = (NFFTStrategy) Strategy.getInstance();
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// Set the response message's MIME type
		response.setContentType("text/html");
		response.setContentType("text/html; charset=UTF-8");
		
		// Allocate a output writer to write the response message into the
		// network socket
		PrintWriter out = response.getWriter();
		
		// Write the documentation response message, in an HTML page
		try {
			out.println(FileUtils.readFileFromJar("/be/panako/http/api.html"));
		} finally {
			out.close(); // Always close the output writer
		}
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		StringBuffer jb = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null) {
				jb.append(line);
			}
		} catch (Exception e) {
			/* report an error */
		}
		
		String incomJSONRequest = jb.toString();
		
		System.out.println("recieved a Match JSON request");
		System.out.println("on thread:" + Thread.currentThread().getId());
		
		JSONObject obj = new JSONObject(incomJSONRequest);
		
		String serializedFingerprints = obj.get("fingerprints").toString();
		double queryDuration = obj.getDouble("query_duration");
		double queryOffset = obj.getDouble("query_offset");
		
		System.out.println("Parsed request with properties :");
		System.out.println("fingerprint length: " + serializedFingerprints.toString().length());
		System.out.println("query_duration: " + queryDuration);
		System.out.println("query_offset: " + queryOffset);
		
		final PrintWriter output = response.getWriter();
		
		strategy.matchSerializedFingerprints(serializedFingerprints, 3, new QueryResultHandler(	) {
			
			@Override
			public void handleQueryResult(QueryResult result) {
				
				
				/*
				String queryInfo = String.format("%s;%.0f;%.0f;",query,r.queryTimeOffsetStart,r.queryTimeOffsetStop);
				String matchInfo = String.format("%s;%s;%.0f;%.0f;", r.identifier,r.description,r.time,r.score);
				String factorInfo = String.format("%.0f%%;%.0f%%", r.timeFactor,r.frequencyFactor);
				System.out.println(queryInfo+matchInfo+factorInfo);
				*/
				
				JSONObject object = new JSONObject();
				//query info
				object.put("query", "");
				object.put("query_start", result.queryTimeOffsetStart);
				object.put("query_stop", result.queryTimeOffsetStop);
				//match info
				object.put("match_identifier", result.identifier);
				object.put("match_description", result.description);
				object.put("match_start", result.time);
				object.put("match_score", result.score);

				output.println(object.toString());
			}
			
			@Override
			public void handleEmptyResult(QueryResult result) {
				JSONObject object = new JSONObject();
				//query info
				object.put("query", "");
				object.put("query_start",  result.queryTimeOffsetStart);
				object.put("query_stop", result.queryTimeOffsetStop);
				//match info
				object.put("match_identifier", 0);
				object.put("match_description", "NO MATCH");
				object.put("match_start", 0);
				object.put("match_score", 0);

				output.println(object.toString());
				
			}
		}, queryDuration, queryOffset);
		
		// Set the response message's MIME type
		response.setContentType("application/json");
		
		// Allocate a output writer to write the response message into the
		// network socket		
		output.close();
	}
}
