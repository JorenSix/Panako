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

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import org.json.JSONException;
import org.json.JSONObject;

import be.panako.http.PanakoWebserviceClient;
import be.panako.http.ResponseHandler;
import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioGenerator;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.util.AudioResourceUtils;

public class Tap extends Application  {
	
	private static long startQuery;
	private static long audioStart;
	private static double matchStart;
	private static double queryDuration;

	private final int sampleRate = Config.getInt(Key.NFFT_SAMPLE_RATE);
    private final int bufferSize = Config.getInt(Key.NFFT_SIZE);
    private final int stepSize = Config.getInt(Key.NFFT_STEP_SIZE);
    private PanakoWebserviceClient client;
    
    private final double queryLengthInSeconds = 12.8;
    private final NFFTEventPointProcessor processorEventPoints = new NFFTEventPointProcessor(bufferSize,stepSize,sampleRate);
    private final int queryLengthInAnalysisFrames = (int) Math.round(queryLengthInSeconds*sampleRate/(float) stepSize);

	@Override
	public void run(String... args) {
		client = new PanakoWebserviceClient();
		AudioDispatcher d = null;
		if (args.length > 0){
			String inputResource = AudioResourceUtils.sanitizeResource(args[0]);
			d = AudioDispatcherFactory.fromPipe(inputResource, sampleRate, bufferSize, bufferSize-stepSize);
		}else if (args.length == 0){
			try {
				d = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, bufferSize-stepSize);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
		
		d.addAudioProcessor(processorEventPoints);
		d.addAudioProcessor(new AudioProcessor() {
			
			private int frameCounter;
			
			
			@Override
			public void processingFinished() {
				
			}

			@Override
			public boolean process(AudioEvent audioEvent) {
				if(audioStart == 0){
					audioStart = System.currentTimeMillis();
				}
				if (frameCounter != 0
						&& frameCounter % queryLengthInAnalysisFrames == 0) {
					processorEventPoints.processingFinished();
					List<NFFTFingerprint> fingerprints = new ArrayList<NFFTFingerprint>(processorEventPoints.getFingerprints());
					handleFingerprints(fingerprints);
					processorEventPoints.reset();
				}
				frameCounter++;
				return true;
			}
		});
		
		try {
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		d.run();
	}

	@Override
	public String description() {
		return "Tap to the beat of the incoming audio stream. Taps are printed to the command line." ;
	}
	
	private static class MatchResponseHandler implements ResponseHandler {

		@Override
		public void handle(int responseCode, String response, String source,
				String type, long millis) {
			

			JSONObject responseObject = null;
			try {
				responseObject = new JSONObject(response);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			int matchScore = responseObject.getInt("match_score");
			if(matchStart==0){
			 matchStart = responseObject.getDouble("match_start") - 0.064;
			
			}else{
				matchStart = responseObject.getDouble("match_start") - 0.096;	
			}
			 queryDuration = responseObject.getDouble("query_stop") - responseObject.getDouble("query_start");
			
			 
			 /*String message = ("Finished '" + type + "'-request on " + source);
				message = message + ("\tResponse Code : " + responseCode);
				message = message + ("\tResponse time: " + millis);
				message = message + ("\tResponse body: " + response);*/
				System.out.println(matchStart);
				
			if (matchScore > 1) {
				String matchIdentifier = responseObject.getString("match_identifier");
				new PanakoWebserviceClient().metadata(new MetaDataResponseHandler(), matchIdentifier);
			}
		}
	}
	private static AudioGenerator old = null;
	private static class MetaDataResponseHandler implements ResponseHandler{
		@Override
		public void handle(int responseCode, String response,String source, String type, long millis) {
			final List<Double> beats = PanakoWebserviceClient.beatListFromResponse(response);
			
			
			double requestTime = (System.currentTimeMillis() - startQuery)/1000.0;
	        double systemLatency = 0.02; 
	        double totalOffset = requestTime  + systemLatency + queryDuration + matchStart;
	        
			double firstBeat = 0;
	        boolean first = true;
	        for(int i = 0 ; i < beats.size() ; i++){
	            double newTime= beats.get(i) - totalOffset;
	            if(newTime < 0.1){
	            	beats.remove(i);
	                i--;
	            }else{
	                if(first) {
	                    firstBeat = beats.get(i);
	                    first = false;
	                }
	                beats.set(i,newTime);
	            }
	        }
	        
	        AudioGenerator g = new AudioGenerator(32, 0);
	        g.addAudioProcessor(new AudioProcessor() {
				
	        	int i;
				@Override
				public void processingFinished() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					if(i< beats.size() && audioEvent.getTimeStamp() > beats.get(i)){
						System.out.println(";"+(System.currentTimeMillis() -  audioStart)/1000.0);
						i++;
						for(int i = 0 ; i < audioEvent.getFloatBuffer().length;i++){
							audioEvent.getFloatBuffer()[i]= (float) Math.random();
						}
					}
					return true;
				}
			});
	        try {
				g.addAudioProcessor(new AudioPlayer(g.getFormat()));
				g.addAudioProcessor(new AudioProcessor() {
					
					@Override
					public void processingFinished() {
						// TODO Auto-generated method stub
					}
					
					@Override
					public boolean process(AudioEvent audioEvent) {
						audioEvent.clearFloatBuffer();
						return true;
					}
				});
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        if(old !=null){
	        	old.stop();
	        }
	        old = g;
	        new Thread(g).start();
			System.out.println("Total offset time " + totalOffset + " s");
			System.out.println("recieved " + beats.size() + " beats");
			System.out.println("Total query time " + (System.currentTimeMillis()-startQuery) + " ms");
		}
	}
	
	
	private void handleFingerprints(final List<NFFTFingerprint> fingerprints){
		
			startQuery = System.currentTimeMillis();	
		
		
		 new Thread(new Runnable(){
			 private double timeDelta = stepSize/(double) sampleRate;//seconds
             @Override
             public void run() {
            	 String parsed = null;
                 try {
                     parsed = PanakoWebserviceClient.serializeFingerprintsToJson(fingerprints).toString();
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
                 client.match(new MatchResponseHandler(), parsed, queryLengthInAnalysisFrames * timeDelta, 0);
             }
         }).start();
	}

	@Override
	public String synopsis() {
		return "tap [audio.mp3]";
	}

	@Override
	public boolean needsStorage() {
		return true;
	}

	@Override
	public boolean writesToStorage() {
		return false;
	}
}
