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

import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import be.panako.ui.CteQFingerprintBrowser;
import be.panako.ui.IFFTFingerprintBrowser;
import be.panako.ui.NCteQFingerprintBrowser;
import be.panako.ui.NFFTFingerprintBrowser;
import be.panako.util.Config;
import be.panako.util.Key;

public class Browser extends Application {

	@Override
	public void run(String... args) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					JFrame frame = null; 
					if(Config.get(Key.STRATEGY).equals("CTEQ")) {
						frame = new CteQFingerprintBrowser();
					} else if(Config.get(Key.STRATEGY).equals("NFFT")) {
						frame = new NFFTFingerprintBrowser();
					}else if(Config.get(Key.STRATEGY).equals("NCTEQ")) {
						frame = new NCteQFingerprintBrowser();
					}else if(Config.get(Key.STRATEGY).equals("IFFT")) {
						frame = new IFFTFingerprintBrowser();
					}
					frame.pack();
					frame.setSize(800,550);
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public String description() {
		return "Starts the fingerprinter browser";
	}

	@Override
	public String synopsis() {
		return "browser";
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
