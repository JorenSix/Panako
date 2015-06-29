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


package be.panako.cli;

import java.io.IOException;

import org.json.JSONObject;

import be.panako.http.HTTPClient;
import be.panako.strategy.SerializedFingerprintsHandler;
import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;

public class Client extends Application {

	@Override
	public void run(String... args) {
		final HTTPClient client = new HTTPClient();
		String url = args[0];
		if(!url.startsWith("http://")){
			url = "http://"+url;
		}
		if(!url.endsWith("/v1.0/match")){
			url = url+"/v1.0/match";
		}
		final String webservceURL = url;
		NFFTStrategy strategy = (NFFTStrategy) Strategy.getInstance();
		
		String query = Panako.DEFAULT_MICROPHONE;
		if(args.length>=1){
			query = args[1];
		}
		strategy.monitor(query, new SerializedFingerprintsHandler() {
			@Override
			public void handleSerializedFingerprints(String fingerprints,
					double queryDuration, double queryOffset) {
				JSONObject object = new JSONObject();
				object.put("query_duration", queryDuration);
				object.put("query_offset", queryOffset);
				String body = object.toString();
				body = body.replace("{", "{\"fingerprints\":" + fingerprints + ",");
				try {
					client.post(webservceURL,body , HTTPClient.printResponseHandler);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		
	}

	@Override
	public String description() {
		return "A client for the rest API";
	}

	@Override
	public String synopsis() {
		return "client url [audio_file]";
	}

	@Override
	public boolean needsStorage() {
		return false;
	}

	@Override
	public boolean writesToStorage() {
		return false;
	}

}
