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





package be.panako.cli;


import java.util.HashSet;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.tarsos.dsp.util.AudioResourceUtils;

public class Monitor extends Application implements QueryResultHandler {

	
	@Override
	public void run(String... args) {
		String inputResource = null;
		if (args.length > 0){
			inputResource = AudioResourceUtils.sanitizeResource(args[0]);
		}else if (args.length == 0){
			inputResource = Panako.DEFAULT_MICROPHONE;
		}
		Strategy strategy = Strategy.getInstance();
		
		Panako.printQueryResultHeader();
		strategy.monitor(inputResource,1,new HashSet<Integer>(), this);
	}

	@Override
	public String description() {
		return "Monitors a stream or a long audio file, the main difference with query is that more than one detection result is expected.";
	}

	@Override
	public String synopsis() {
		return "[pls|m3u|file]";
	}

	@Override
	public boolean needsStorage() {
		return true;
	}

	@Override
	public boolean writesToStorage() {
		return false;
	}

	@Override
	public void handleQueryResult(QueryResult r) {
		Panako.printQueryResult(r);
	}

	@Override
	public void handleEmptyResult(QueryResult r) {
		Panako.printQueryResult(r);
	}
}
