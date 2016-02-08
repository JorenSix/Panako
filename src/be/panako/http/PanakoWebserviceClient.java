package be.panako.http;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import be.panako.strategy.nfft.NFFTFingerprint;

public class PanakoWebserviceClient {
    private final HTTPClient client = new HTTPClient();
    private final String matchURL = "http://api.panako.be/v1.0/match";
    private final String metadataURL = "http://api.panako.be/v1.0/metadata";

    public void match(ResponseHandler responseHandler,String fingerprints,double queryDuration,double queryOffset){

        try {
            JSONObject object = new JSONObject();
            object.put("query_duration", queryDuration);
            object.put("query_offset", queryOffset);
            String body = object.toString();
            body = body.replace("{", "{\"fingerprints\":" + fingerprints + ",");
            client.post(matchURL,body , responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void metadata(ResponseHandler responseHandler,String identifier){
        try {
            JSONObject object = new JSONObject();
            object.put("id", identifier);
            client.post(metadataURL, object.toString(), responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<NFFTFingerprint> deserializeFingerprintsFromJson(String fingerprints){
		JSONArray array = new JSONArray(fingerprints);
		List<NFFTFingerprint> fingerprintArray = new ArrayList<NFFTFingerprint>();
		for(int i = 0 ; i < array.length();i++){
			JSONObject obj = array.getJSONObject(i);
			int f1 = obj.getInt("f1");
			int f2 = obj.getInt("f2");
			int t1 = obj.getInt("t1");
			int t2 = obj.getInt("t2");
			float f1e = (float) obj.getDouble("f1e");
			float f2e = (float) obj.getDouble("f2e");
			fingerprintArray.add(new NFFTFingerprint(t1, f1,f1e, t2, f2,f2e));
		}
		return fingerprintArray;
	}
    
    public static List<Double> beatListFromResponse(String response){
        List<Double> beatList = new ArrayList<Double>();
        try{
            JSONObject metadata = new JSONObject(response);
            JSONArray beats = metadata.getJSONObject("rhythm").getJSONArray("beats_position");
            for(int i = 0 ; i < beats.length();i++) {
                double tapAtTime = (beats.getDouble(i));
                beatList.add(tapAtTime);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return beatList;
    }

    public static String serializeFingerprintsToJson(List<NFFTFingerprint> fingerprints) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for(NFFTFingerprint fingerprint : fingerprints){
            JSONObject fingerprintJSON = new JSONObject();
            fingerprintJSON.put("f1", fingerprint.f1);
            fingerprintJSON.put("f2", fingerprint.f2);
            fingerprintJSON.put("t1", fingerprint.t1);
            fingerprintJSON.put("t2", fingerprint.t2);
            fingerprintJSON.put("f1e", fingerprint.f1Estimate);
            fingerprintJSON.put("f2e", fingerprint.f2Estimate);
            jsonArray.put(fingerprintJSON);
        }
        return jsonArray.toString();
    }
}
