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



package be.panako.strategy.quad;

import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;

/**
 * 
 * This should implement <a href="http://www.dafx14.uni-erlangen.de/papers/dafx14_reinhard_sonnleitner_quad_based_audio_fingerpr.pdf">QUAD-BASED AUDIO FINGERPRINTING ROBUST TO TIME AND FREQUENCY SCALING</a>
 * 
 * by Reinhard Sonnleitner and Gerhard Widmer.
 * @author joren
 *
 */
public class QuadStrategy extends Strategy {

	@Override
	public double store(String resource, String description) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void query(String query, int maxNumberOfResults,
			QueryResultHandler handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void monitor(String query, int maxNumberOfReqults,
			QueryResultHandler handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasResource(String resource) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStorageAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void printStorageStatistics() {
		// TODO Auto-generated method stub
	}

}
