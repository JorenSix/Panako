package be.panako.cli;

import java.io.File;
import java.util.List;

import be.panako.strategy.Strategy;
import be.panako.strategy.nfft.NFFTStrategy;

/**
 * Compares audio files in answers the following question:
 *  when does the same exact audio reappear in another file?
 *
 */
public class Compare extends Application {

	@Override
	public void run(String... args) {
		
		Strategy strategy = Strategy.getInstance();
		if(strategy instanceof NFFTStrategy){
			NFFTStrategy strat = (NFFTStrategy) strategy;
			List<File> files = this.getFilesFromArguments(args);
			if(files.size()==1){
				strat.compareFingerprints(files.get(0),files.get(0));
			}else{
				for(int i = 1 ; i < files.size();i++){
					strat.compareFingerprints(files.get(0),files.get(1));
					System.out.println("");
				}
			}	
		}else{
			System.out.println("Currently only NFFT supports the print operation!");
			System.err.println("Currently only NFFT supports the print operation!");
		}		
	}

	@Override
	public String description() {
		
		return "Extracts fingerprints prints the times at which fingerprints occur twice.";
	}

	@Override
	public String synopsis() {
		return "[audio_files...]";
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
