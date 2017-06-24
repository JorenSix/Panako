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



package be.panako.cli;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.UIManager;

import be.panako.strategy.nfft.NFFTStreamSync;
import be.panako.strategy.nfft.NFFTSyncMatch;
import be.panako.ui.syncsink.SyncSinkFrame;

public class Syncsink extends Application {
	
	@Override
	public void run(final String... args) {
		if(args.length == 0){
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} catch (Exception e) {
					}
					SyncSinkFrame frame = new SyncSinkFrame();
					frame.setSize(800, 600);
					frame.setVisible(true);
					Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
					frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
					
					//add the files
					int i = 0;
					for(File file : getFilesFromArguments(args)){
						frame.openFile(file,i);
						i++;
					}
				}
			});
		}else if(args.length > 1){
			String reference = new File(args[0]).getAbsolutePath();
			for(int i = 1 ; i < args.length;i++){
				String other = new File(args[i]).getAbsolutePath();
				NFFTStreamSync syncer = new NFFTStreamSync(reference,other);
				syncer.synchronize();
				//can be null when no match is found
				NFFTSyncMatch match = syncer.getMatch();
				if(match == null){
					System.out.println(String.format("%s;%s;NaN",args[0],args[1]));
				}else{
					System.out.println(String.format("%s;%s;%.3f",args[0],args[1],match.getRefinedOffset()));
				}
			}
		}else{
			System.err.print(synopsis());
		}
	}
	
	@Override
	public String description() {
		return "Shows a user interface to synchronize audio/video/data files with respect to the reference.";
	}

	@Override
	public String synopsis() {
		return "[reference] [others...]";
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
