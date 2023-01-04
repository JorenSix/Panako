package be.panako.strategy.panako;

import be.panako.cli.Panako;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Use an external script to extract event points, e.g. using the GPU.
 */
public class PanakoGPUEventPointProcessor {

    /**
     * Default constructor
     */
    public PanakoGPUEventPointProcessor(){

    }

    /**
     * Extracts fingerprints by calling a socket.
     * @param resource The resource to extract prints from
     * @return A list of fingerprints
     */
    public List<PanakoFingerprint> extractFingerprints(String resource){
        List<PanakoFingerprint> fingerprints = new ArrayList<>();
        try {
            Socket extractorSocket = null;
            extractorSocket = new Socket("127.0.0.1", 6677);
            BufferedReader in =  new BufferedReader(new InputStreamReader(extractorSocket.getInputStream()));
            PrintWriter out =  new PrintWriter(extractorSocket.getOutputStream(), true);
            out.println(resource);
            List<PanakoEventPoint> eventPoints = new ArrayList<>();
            String line = null;
             while ((line = in.readLine()) != null){
                 String[] data = line.split(" ");
                 int t = Integer.valueOf(data[0]);
                 int f = Integer.valueOf(data[1]);
                 float m = Float.valueOf(data[2]);
                 eventPoints.add(new PanakoEventPoint(t,f,m));
             }
             PanakoEventPointProcessor.packEventPointsIntoFingerprints(eventPoints,fingerprints);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fingerprints;
    }
}
