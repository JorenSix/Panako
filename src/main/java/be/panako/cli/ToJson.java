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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;

import be.panako.strategy.Strategy;


/**
 * Output the fingerprints extracted from an audio file as JSON: mainly for debugging and integration.
 * @author Joren Six
 */
class ToJson extends Application{

	@Override
	public void run(String... args) {
	
		List<File> files = this.getFilesFromArguments(args);
		
		boolean base64Encode = hasArgument("-b64", args) || hasArgument("--base64", args);
		
		Strategy strategy = Strategy.getInstance();
		
		for(File file: files){
			String json = strategy.toJson(file.getAbsolutePath());
			
			if(base64Encode) {
				try {
					// Compress with zlib
					byte[] input = json.getBytes("UTF-8");
					Deflater deflater = new Deflater();
					deflater.setInput(input);
					deflater.finish();
					
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
					byte[] buffer = new byte[1024];
					while (!deflater.finished()) {
						int count = deflater.deflate(buffer);
						outputStream.write(buffer, 0, count);
					}
					outputStream.close();
					deflater.end();
					
					byte[] compressed = outputStream.toByteArray();
					
					// Encode to base64
					String encoded = Base64.getEncoder().encodeToString(compressed);
					System.out.println(encoded);
				} catch (Exception e) {
					System.err.println("Error compressing and encoding: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				System.out.println(json);
			}
		}
		
	}

	@Override
	public String description() {
		return "Outputs the fingerprints as JSON to stdout. Use -b64 or --base64 to compress with zlib and encode as base64.";
	}

	@Override
	public String synopsis() {
		return "[-b64|--base64] [audio_file...]";
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

	

