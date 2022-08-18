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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;


/**
 * Application is the abstract base class of applications with a command line interface.
 */
public abstract class Application {	
	private final static Logger LOG = Logger.getLogger(Application.class.getName());

	 /**
	  * Run a command line application with a set of arguments
     * @param args
     *            The arguments to start the program.
     */
    public abstract void run(final String... args);
    
    /**
     * The name of the application is the class lower case class name.
     * @return The name of the application.
     */
    public final String name(){
    	//fully qualified name
    	String name = this.getClass().getCanonicalName();
    	//unqualified name
    	name = name.substring(name.lastIndexOf('.')+1);
    	//lower case first letter
    	name = name.substring(0,1).toLowerCase() + name.substring(1);    
    	//join with parts with an underscore
    	return name;
    }
    
    /**
	 * Return the description of the command line application
     * @return The description of the application. What does it do?
     */
    public abstract String description();
    
    /**
     * The synopsis is a short description of the required or optional arguments.
     * @return The command line synopsis. 
     */
    public abstract String synopsis();
    
    
    /**
	 * Does this command line application need storage?
     * @return True if this application needs the (key/value) storage, false otherwise.
     */
    public abstract boolean needsStorage();
    
    
    
    
	
	/**
	 * Returns a list of exiting (checked) files in the argument list.
	 * If a text (with txt extension) file is found its contents is read and each line in
	 * the file interpreted as an absolute path to a file.
	 * @param arguments the list of command line arguments
	 * @return A list of checked file.
	 */
	public List<File> getFilesFromArguments(String[] arguments){
		final List<File> files = new ArrayList<File>();
		for(final String queryFile : arguments){
			//skip options, starting with a -
			if(!queryFile.startsWith("-")){
				if(queryFile.endsWith("txt")){
					//read the contents of the file.
					//it should an absolute path on each line of the file.
					String[] lines = FileUtils.readFile(queryFile).split("\n");
					for(String line : lines){
						if(checkFile(line)){
							files.add(new File(line));
						}
					}
				}else{
					if(checkFile(queryFile)){
						files.add(new File(queryFile));
					}
				}	
			}
		}
		return files;
	}
	
	/**
	 * Checks if a file exists and can be read.
	 * @param file The file to check.
	 * @return True if the file exists and can be read. False otherwise.
	 */
	protected boolean checkFile(String file){
		File f = new File(file);
		boolean fileOk = false;
		if(f.exists() && f.canRead()){
			fileOk = true;
		}else{
			String message = "Could not read " + f.getAbsolutePath() + " it does not exist or is not accesible at the moment.)";
			LOG.warning(message);
			System.out.println(message);
		}
		return fileOk;
	}
	
	/**
	 * Checks the configuration and returns either the number of configured 
	 * number of available processors to use or the maximum number of 
	 * available processors if the configured value is zero or negative.
	 * @return Either the number of configured 
	 * number of available processors to use or the maximum number of 
	 * available processors if the configured value is zero or negative.
	 */
	public static int availableProcessors(){
		int configuredValue = Config.getInt(Key.AVAILABLE_PROCESSORS);
		final int actualValue;
		if(configuredValue > 0 ){
			actualValue = configuredValue;
		}else{
			actualValue = Runtime.getRuntime().availableProcessors();
		}
		return actualValue;
	}
	
	/**
	 * Checks if an argument is present in a list of arguments.
	 * @param argument The argument to check for.
	 * @param strings The given arguments.
	 * @return True if the argument is in the given list. Case is ignored. False otherwise.
	 */
	protected boolean hasArgument(String argument, String... strings){
		boolean hasArgument = false;
		for(String arg:strings){
			if(arg.equalsIgnoreCase(argument)){
				hasArgument = true;
			}
		}
		return hasArgument;
	}
	
	/**
	 * Get the value of an integer option
	 * @param option The argument e.g. "-n".
	 * @param defaultValue The default value if the option is not provided.
	 * @param arguments The list of arguments to search in.
	 * @return The integer found or the default value.
	 */
	protected int getIntegerOption(String option,int defaultValue, String... arguments){
		return Integer.parseInt(getOption(option,String.valueOf(defaultValue),arguments));
	}
	
	/**
	 * Return the value of an option. E.g. calling this method with 
	 * option "-n" and default value 20 on the following <code>panako stats --n 10</code>
	 * returns 10. 
	 * @param option The option to look for
	 * @param defaultValue The default value
	 * @param arguments The arguments to search in
	 * @return The value of the option, or the provided default.
	 */
	protected String getOption(String option, String defaultValue, String...arguments){
		String value = defaultValue;
		for(int i = 0 ; i < arguments.length ; i++){
			if(arguments[i].equalsIgnoreCase(option)){
				value = arguments[i+1];
			}
		}
		return value;
	}

	/**
	 * Does this application need write access to the storage system?
	 * @return true if this application writes to storage.
	 */
	public abstract boolean writesToStorage() ;

	/**
	 * Print a help statement for the command line application.
	 */
	public void printHelp() {
		System.out.println("Name");
		System.out.println("\t" + name());
		System.out.println("Synopsis");
		System.out.println("\tpanako " + name() + " " + synopsis());
		System.out.println("Description");
		System.out.println("\t" + description());
	}
}
