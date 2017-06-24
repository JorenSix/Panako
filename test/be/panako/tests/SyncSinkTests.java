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

package be.panako.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import be.panako.strategy.nfft.NFFTStreamSync;
import be.panako.strategy.nfft.NFFTSyncMatch;
import be.panako.util.Config;
import be.panako.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.PipeDecoder;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class SyncSinkTests {
	
	@Test
	public void testCrossCovarianceOnExactAudio(){
		for(DataSetTestCase testCase : DataSetTestCase.getCases()){
			File reference = testCase.referenceFile;
			assertTrue("Size of reference should be > 0 bytes", reference.length() > 0);	
			float[] referenceBuffer = TestUtilities.getAudioBuffer(reference, 10.0, 10.45);
			for(int expectedLag = 0 ; expectedLag < 441 * 10 ; expectedLag += 441){
				double startTime = 10.0 + expectedLag/44100.0;
				float[] queryBuffer = TestUtilities.getAudioBuffer(reference,startTime , 10.2);
				for(int i = 0 ; i < queryBuffer.length ; i++){
					assertEquals("Buffers should be equal for lag: " + expectedLag,queryBuffer[i], referenceBuffer[i+expectedLag], 0.0000000001);
				}
				int foundLag = NFFTStreamSync.bestCrossCovarianceLag(referenceBuffer,queryBuffer);
				assertTrue("Expected " + expectedLag + " found " + foundLag + " for: " + reference.getName(),foundLag==expectedLag);
			}	
		}
	}
	
	@Test
	public void testCrossCovarianceOnExactAudioPipeDecoder(){
		for(DataSetTestCase testCase : DataSetTestCase.getCases()){
			File reference = testCase.referenceFile;
			assertTrue("Size of reference should be > 0 bytes", reference.length() > 0);
			float[] referenceBuffer = NFFTStreamSync.getAudioData(reference.getAbsolutePath(), 44100, 10.0, 0.45);
			for(int expectedLag = 0 ; expectedLag < 441 * 10 ; expectedLag += 441){
				double startTime = 10.0 + expectedLag/44100.0;
				float[] queryBuffer = NFFTStreamSync.getAudioData(reference.getAbsolutePath(), 44100, startTime, 0.2);
				for(int i = 0 ; i < queryBuffer.length ; i++){
					assertEquals("Buffers should be equal for lag: " + expectedLag,queryBuffer[i], referenceBuffer[i+expectedLag], 0.0000000001);
				}
				int foundLag = NFFTStreamSync.bestCrossCovarianceLag(referenceBuffer,queryBuffer);
				assertTrue("Expected " + expectedLag + " found " + foundLag + " for: " + reference.getName(),foundLag==expectedLag);
			}
		}
	}
	
	@Test
	public void testCrossCovarianceOnCompressedAudio(){
		for(DataSetTestCase testCase : DataSetTestCase.getCases()){
			double offset = testCase.offset;
			File reference = testCase.referenceFile;
			File compressedQuery = testCase.queryFile;
			float[] referenceBuffer = TestUtilities.getAudioBuffer(reference, offset-0.1, offset + 0.1);
			float[] queryBuffer = TestUtilities.getAudioBuffer(compressedQuery,0,0.1);
			int expectedLag = 4410;
			int foundLag = NFFTStreamSync.bestCrossCovarianceLag(referenceBuffer,queryBuffer);
			assertTrue("Expected " + expectedLag + " found : " + foundLag + " for: " + compressedQuery.getName(),foundLag==expectedLag);
		}
	}
	
	
	@Test
	public void testStreamSyncExactAudio(){
		for(DataSetTestCase testCase : DataSetTestCase.getCases()){
			File reference = testCase.referenceFile;
			String other = reference.getAbsolutePath();
			NFFTStreamSync syncer = new NFFTStreamSync(testCase.referenceFile.getAbsolutePath(),other);
			syncer.synchronize();
			NFFTSyncMatch  match = syncer.getMatch();
			float expectedLag = 0;
			assertEquals("Buffers should be equal for lag: " + expectedLag, expectedLag,match.getRoughOffset(), 0.0001);
		}
	}
	
	@Test
	public void testStreamSyncCompressedAudio(){
		for(DataSetTestCase testCase : DataSetTestCase.getCases()){
			
			String other = testCase.queryFile.getAbsolutePath();
			NFFTStreamSync syncer = new NFFTStreamSync(testCase.referenceFile.getAbsolutePath(),other);
			syncer.synchronize();
			NFFTSyncMatch  match = syncer.getMatch();
			double expectedOffset = testCase.offset;
			assertEquals("Buffers should be equal for lag: " + expectedOffset, expectedOffset,match.getRefinedOffset(), 0.0001);
		
		}
	}
	
	@Test
	public void testStreamSyncCompressedAudioSanity(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("----------------------------------------\n");
		sb.append("Configuration currently in use: \n");
		for(Key key : Key.values()){
			sb.append("\t");
			sb.append(key.name());
			sb.append("=");
			sb.append(Config.get(key));
			sb.append("\n");
		}
		sb.append("----------------------------------------\n");
		System.out.println(sb.toString());
		
		String reference = "/home/joren/workspace/Panako/test/be/panako/tests/res/dataset/195297.wav";
		String other = "/home/joren/workspace/Panako/test/be/panako/tests/res/query/72_195297.wav";		
		NFFTStreamSync syncer = new NFFTStreamSync(reference,other);
		syncer.synchronize();
		NFFTSyncMatch  match = syncer.getMatch();
	
		assertEquals("Buffers should be equal for lag: " + 72, 72,match.getRefinedOffset(), 0.0001);
	}
	
	
	
	@Test
	public void testPipeDecoder(){
		File reference = TestUtilities.getResource("dataset/61198.wav");
		File referenceFile = TestUtilities.getResource("dataset/61198.wav");
		final float[] referenceBuffer = TestUtilities.getAudioBuffer(reference,1.0,1.5);
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe(referenceFile.getAbsolutePath(), 44100, 22050, 0,1.0,0.5);
		d.addAudioProcessor(new AudioProcessor() {
			boolean ran = false;
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				if(!ran){
					float[] otherBuffer = audioEvent.getFloatBuffer();
					assertEquals("Buffers should be equal in length", referenceBuffer.length, otherBuffer.length); 
					for(int i = 0 ; i < otherBuffer.length; i++){
						assertEquals("Buffers should have the same content", referenceBuffer[i], otherBuffer[i],0.0000001);
					}
				}
				ran = true;
				return true;
			}
		});
		d.run();		
	}
	
	@Test
	public void testPipeDecoderDuration(){
		File reference = TestUtilities.getResource("dataset/61198.wav");
		new PipeDecoder().getDuration(reference.getAbsolutePath());		
	}
		
	private static class DataSetTestCase{
		File referenceFile;
		File queryFile;
		double offset;
		
		public static List<DataSetTestCase> getCases(){
			File[] queryFiles = TestUtilities.getResources("query","wav");
			File[] referenceFiles = TestUtilities.getResources("dataset","wav");
			ArrayList<DataSetTestCase> cases = new ArrayList<>();
			for(File reference: referenceFiles){
				File compressedQuery = null;
				double expectedOffset = 0;
				for(File queryFile : queryFiles){
					String id = queryFile.getName().split("_")[1];
					String offset = queryFile.getName().split("_")[0];
					if(reference.getName().equals(id)){
						compressedQuery = queryFile;
						expectedOffset = Double.valueOf(offset);
					}
				}
				DataSetTestCase testCase = new DataSetTestCase();
				testCase.offset = expectedOffset;
				testCase.referenceFile = reference;
				testCase.queryFile = compressedQuery;
				cases.add(testCase);
			}
			return cases;
		}
	}
}
