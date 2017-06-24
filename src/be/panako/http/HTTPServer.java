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

import be.panako.util.Config;
import be.panako.util.Key;
import Acme.Serve.Serve;

public class HTTPServer    {
	
	private final Serve webserver; 
	
	public HTTPServer(){
		// setting properties for the server, and exchangeable Acceptors
		java.util.Properties properties = new java.util.Properties();
		
		int httpPort = Config.getInt(Key.HTTP_SERVER_PORT);
		properties.put("port", httpPort);
		properties.setProperty(Acme.Serve.Serve.ARG_NOHUP, "nohup");
		properties.put(Acme.Serve.Serve.ARG_THREAD_POOL_SIZE, 50);
		
		webserver = new Serve(properties, System.err);
		// Add the Servlet
		webserver.addServlet("/v1.0/match", new MatchServlet());
		webserver.addServlet("/v1.0/metadata", new MetaDataServlet());
		// Add the same servlet to / for documentation
		webserver.addServlet("/", new MatchServlet());
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				stop();
			}
		}));
	}
	
	public void start(){
		webserver.serve();
	}
	
	public void stop(){
		webserver.notifyStop();
		webserver.destroyAllServlets();
	}
}
