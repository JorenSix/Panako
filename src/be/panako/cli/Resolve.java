package be.panako.cli;

import be.panako.strategy.Strategy;

public class Resolve extends Application {

	@Override
	public void run(String... args) {
		Strategy strat = Strategy.getInstance();
		System.out.println(strat.resolve(args[0]));
	}

	@Override
	public String description() {
		return "Resolves the filename and returns the internal identifier.";
	}

	@Override
	public String synopsis() {
		return "resolve filemp3";
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
