package be.panako.tests;

import be.panako.android.Tapper;

public class Tappertest {
	
	public static void main (String[] args) throws InterruptedException{
		Tapper t = Tapper.getInstance();
		//double[] beats = {0,1,2.5,4,5,6,10,11,12,13,14,16,20,22,23};
		//double[] beats = {0,4,8};
		double[] beats = new double[25];
		for(int i = 0; i<beats.length ; i++){
			beats[i]=i*0.5;
		}
		t.setBeatList(beats);
		
		t.startTapper();
		long start = System.currentTimeMillis();
		t.startMillis = start;
		
		Thread.sleep((long) (beats[beats.length-1]*1000+500)/2);
		
		t.stopTapper();
		
		
		t.setBeatList(beats);
		t.startTapper();
		
		Thread.sleep((long) (beats[beats.length-1]*1000+500));

		t.stopTapper();
		
	}
}
