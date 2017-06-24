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



package be.panako.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class HTTPClient {
	
	/**
	 * Send a HTTP get request to an url.
	 * @param url The url.
	 * @param handler To handle the response you need a handler.
	 * @throws IOException When the server can not be found.
	 */
	public void get(String url, ResponseHandler handler) throws IOException {
		long millis = System.currentTimeMillis();
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		// This is a GET request 
		String type = "GET";
		con.setRequestMethod(type);
		
		int responseCode = con.getResponseCode();
		
		StringBuffer response = new StringBuffer();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		millis = System.currentTimeMillis() - millis;
		handler.handle(responseCode, response.toString(),url,type,millis); 
	}
	
 
	// HTTP POST request
	/**
	 * Sends a POST request to a server.
	 * @param url The URL to POST to.
	 * @param body the body of the post request. This can contain parameters.
	 * @param handler The handler to handle the response from the server.
	 * @throws IOException when communication goes wrong.
	 */
	public void post(String url,String body, ResponseHandler handler) throws IOException {
		long millis = System.currentTimeMillis();
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		// This is a POST request
		String type = "POST";
		con.setRequestMethod(type);
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(body);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		millis = System.currentTimeMillis() - millis;
		handler.handle(responseCode,response.toString(),url,type,millis);
	}
	
	public static ResponseHandler printResponseHandler = new ResponseHandler() {
		@Override
		public void handle(int responseCode, String response,String source,String type,long millis) {
			//print result
			System.out.println("Finished '" + type + "'-request on " + source);
			System.out.println("\tResponse Code : " + responseCode);
			System.out.println("\tResponse time: " + millis);
			System.out.println("\tResponse body: " + response);
		}
	};
}
