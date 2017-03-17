package be.panako.strategy.rafs;

import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;

public class Rafs extends Strategy{

	@Override
	public double store(String resource, String description) {
		
		return 0;
	}

	@Override
	public void query(String query, int maxNumberOfResults, QueryResultHandler handler) {
		
	}

	@Override
	public void monitor(String query, int maxNumberOfReqults, QueryResultHandler handler) {
		//Not implemented
		
	}

	@Override
	public boolean hasResource(String resource) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStorageAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void printStorageStatistics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String resolve(String filename) {
		return null;
	}
	
	
	
}
