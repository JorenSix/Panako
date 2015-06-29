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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;
import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;

/**
 * <p>
 * The meta data Servlet serves meta data for a song.
 * The url is <code>POST /v1.0/metadata</code>. The body of the <code>POST</code>-request should contain
 * the following JSON: <code>{id: 1506}</code> for the audio with identifier 1506.
 * </p>
 * <p>
 * The meta-data is available in a JSON file in a metadata directory. The contents of this file is send. e.g.
 * <code>/opt/panako/metadata/1506.json</code>.
 * </p>
 * <p>
 * A GET-request results in the help-file.
 * </p>
 * @author joren
 *
 */
public class MetaDataServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6714047575444274959L;
	private final MatchServlet helpServlet;
	private final NFFTStrategy strategy;
	private final String metaDataDirectory;
	
	public MetaDataServlet(){
		helpServlet = new MatchServlet();
		strategy = (NFFTStrategy) Strategy.getInstance();
		metaDataDirectory= Config.get(Key.META_DATA_DIRECTORY);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		helpServlet.doGet(request,response);
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
		
		System.out.println("recieved a MetaData JSON request");
		System.out.println("on thread:" + Thread.currentThread().getId());
		
		JSONObject obj = new JSONObject(incomJSONRequest);
		
		Integer identifier = Integer.parseInt(obj.get("id").toString());
		
		System.out.println("Fetch Metadata for:");
		System.out.println("Audio identifier: " + identifier);
		
		
		String fileName = strategy.getAudioDescription(identifier);
		

		final PrintWriter output = response.getWriter();
		if(fileName==null){
			output.println("{\"error\":\"No audio file with descriptor id " + identifier + " found.\"}");
		}else{
			fileName = identifier + ".mp3";
			String extension = "";
			int i = fileName.lastIndexOf('.');
			if (i > 0) {
			    extension = fileName.substring(i+1);
			}
	
			
			String jsonMetadataFile = new File(metaDataDirectory,fileName.replace(extension, "json")).getAbsolutePath();;
			if(FileUtils.exists(jsonMetadataFile)){
				output.println(FileUtils.readFile(jsonMetadataFile));
				// Set the response message's MIME type
				response.setContentType("application/json");
			}else{
				output.println("{\"error\":\"No JSON meta data file found at "+ jsonMetadataFile + ".\"}");
			}
		}
		
		
		
		// Allocate a output writer to write the response message into the
		// network socket
		output.close();
	}

}
