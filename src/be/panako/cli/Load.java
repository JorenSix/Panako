package be.panako.cli;


import be.panako.strategy.olaf.OlafStrategy;

public class Load extends Application {

	@Override
	public void run(String... args) {
		new OlafStrategy().load();
	}

	@Override
	public String description() {
		return "Bulk load sorted fingerprints in an empty! LMDB database";
	}

	@Override
	public String synopsis() {
		return "load";
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
