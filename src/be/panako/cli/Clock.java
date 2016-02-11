package be.panako.cli;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Clock {

	private static class Waiter implements Runnable {
		
		private final int millisecondsToWait;
		public Waiter(int milliseconds){
			millisecondsToWait = milliseconds;
		}
		
	    @Override
	    public void run() {
	    	long nanoStart = System.nanoTime();
	        try {
				Thread.sleep(millisecondsToWait);
				System.out.println((System.nanoTime()-nanoStart)/1000000.0);
			} catch (InterruptedException e) {
				
			}
	    }
	};
	
	public static void main(String[] args) {
		 
	     ExecutorService es = Executors.newFixedThreadPool(50);
	     for(int i = 100 ; i < 13500 ; i+=200){
	    	 es.execute(new Waiter(i));
	     }
	     es.shutdownNow();
	     for(int i = 100 ; i < 13500 ; i+=200){
	    	 es.execute(new Waiter(i));
	     }
	}

}
