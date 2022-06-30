/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2022 - Joren Six / IPEM                             *
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

import javax.sound.sampled.LineUnavailableException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.util.AudioResourceUtils;


public class Play extends Application {
	
	
	private static final int TARGET_SAMPLE_RATE = 15649;

	@Override
	public void run(String... args) {
		String inputResource = AudioResourceUtils.sanitizeResource(args[0]);
		AudioDispatcher d;
		try {
			d = AudioDispatcherFactory.fromPipe(inputResource, TARGET_SAMPLE_RATE, 2028, 0);
			d.addAudioProcessor(new AudioPlayer(JVMAudioInputStream.toAudioFormat(d.getFormat())));
			d.run();
		}  catch (LineUnavailableException e) {
			e.printStackTrace();
			System.err.print(e.getLocalizedMessage());
		}
	}

	@Override
	public String description() {
		return "Plays an audio file or stream. It is used to test the audio conversion system. "
				+ " \n If the JVM and decoder are configured well, it should play (http) streams and audio from container formats."
				+ "";
	}

	@Override
	public String synopsis() {
		return "play [xspf|pls|m3u|asx|avi|file|stream]";
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
