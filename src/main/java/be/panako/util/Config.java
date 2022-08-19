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




package be.panako.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Writes and read the configuration values to and from a properties file.
 * 
 * @author Joren Six
 */
public class Config {
	
	/**
	 * The file on disk that is used to store the configuration values. 
	 * On Android this can be stored here: res/raw/
	 */
	private final String configrationFileName;
	
	/**
	 * The values are stored here, in memory.
	 */
	private final HashMap<Key,String> configrationStore;
	
	
	private final Preferences preferenceStore;
	
	/**
	 * Default constructor. Reads the configured values, or stores the defaults.
	 */
	public Config(){
		String path = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = "";
		preferenceStore = Preferences.userNodeForPackage(Config.class);
		
		try {
			decodedPath = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		configrationFileName = new File(new File(decodedPath).getParent(),"config.properties").getAbsolutePath();
			
		configrationStore = new HashMap<Key, String>();
		if(!FileUtils.exists(configrationFileName)){
			writeDefaultConfigration();
		}
		readConfigration();
	}
	
	/**
	 * Read configuration from properties file on disk.
	 */
	private void readConfigration() {
		Properties prop = new Properties();
    	try {
            //Loads a properties file.
    		FileInputStream inputstream = new FileInputStream(configrationFileName);
    		prop.load(inputstream);
    		inputstream.close();
    		for(Key key : Key.values()){
				String configuredValue = prop.getProperty(key.name());
				configrationStore.put(key, configuredValue);				
			}
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
	}

	private void writeDefaultConfigration(){
		Properties prop = new Properties();
		try {
			//Set the default properties value.
			for(Key key : Key.values()){
				prop.setProperty(key.name(), key.defaultValue);
			}
			//Save the properties to the configuration file.
			prop.store(new FileOutputStream(configrationFileName), null);
		} catch (IOException ex) {
			ex.printStackTrace();
	    }
	}
	
	/*
	 * Write the current configuration values to disk.
	
	public void saveCurrentConfigration(){
		Properties prop = new Properties();
		try {
			//Set the default properties value.
			for(Key key : Key.values()){
				prop.setProperty(key.name(), Config.get(key));
			}
			//Save the properties to the configuration file.
			prop.store(new FileOutputStream(configrationFileName), null);
		} catch (IOException ex) {
			ex.printStackTrace();
	    }
	}
	 */


	/**
	 * Write a value to the configuration storage
	 * @param preferenceKey the configuration key
	 * @param value the configuration value.
	 */
	public void writePreference(String preferenceKey,String value){
		preferenceStore.put(preferenceKey, value);
	}

	/**
	 * Read a preference for a certain key
	 * @param preferenceKey the configuration key
	 * @return The value associated to the configuration key or an empty string"".
	 */
	public String readPreference(String preferenceKey){
		return preferenceStore.get(preferenceKey,"");
	}
	
	private static Config instance;

	/**
	 * Configuration is meant to be used as a Singleton.
	 * @return Return the only instance of the configuration class.
	 */
	public static Config getInstance(){
		if(instance == null){
			instance = new Config();
		}
		return instance;
	}

	/**
	 * Read a configured value for a certain key
	 * @param key the configuration key
	 * @return The value associated to the configuration key or the default value associated to the key.
	 */
	public static String get(Key key){
		//re read configuration
		//getInstance().readConfigration();
		HashMap<Key,String> store = getInstance().configrationStore;
		final String value;
		if(store.get(key)!=null){
			value = store.get(key).trim();
		}else{
			value = key.getDefaultValue();
		}
		return value;
	}

	/**
	 * Read a configured integer for a certain key
	 * @param key the configuration key
	 * @return The value associated to the configuration key or the default value associated to the key.
	 */
	public static int getInt(Key key){
		return Integer.parseInt(get(key));
	}

	/**
	 * Read a configured float for a certain key
	 * @param key the configuration key
	 * @return The value associated to the configuration key or the default value associated to the key.
	 */
	public static float getFloat(Key key){
		return Float.parseFloat(get(key));
	}

	/**
	 * Read a configured boolean for a certain key
	 * @param key the configuration key
	 * @return The value associated to the configuration key or the default value associated to the key.
	 */
	public static boolean getBoolean(Key key){
		return get(key).equalsIgnoreCase("true");
	}
	
	/**
	 * Sets a configuration value to use during the runtime of the application.
	 * These configuration values are not persisted. To 
	 * @param key The key to set.
	 * @param value The value to use.
	 */
	public static void set(Key key, String value) {
		HashMap<Key,String> store = getInstance().configrationStore;
		store.put(key, value);
	}
	
	/**
	 * Use preferences to store configuration that changes during runtime
	 * and need to be persisted.
	 * @param key The key to store.
	 * @param value The value to store
	 */
	public static void setPreference(String key, String value){
		getInstance().writePreference(key, value);
	}

	/**
	 * Use preferences to store configuration that changes during runtime
	 * and need to be persisted.
	 * @param key The key to store.
	 * @return The configured preference value.
	 */
	public static String getPreference(String key){
		return getInstance().readPreference(key);
	}
	
}
