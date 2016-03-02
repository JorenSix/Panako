package be.panako.android;


/**
 * Created by joren on 7/1/15.
 */
public class Tapper {

    private static final String TAG = "Tapper" ;

    static {
        System.loadLibrary("eventor");
    }

    private final static Tapper tapper = new Tapper();
    private long millisAtStartQuery;
    public long startMillis;

    public static Tapper getInstance(){
        return tapper;
    }

    private Tapper(){
   
    }


    public native void startTapper();
    public native void setBeatList(double[] beats);
    public native void stopTapper();

    public void tap(){
    	long tap = System.currentTimeMillis();
    	long time = tap - startMillis;
    	
        System.out.println("Recieved tap at " + time );
    }
}
