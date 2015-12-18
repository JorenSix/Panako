package be.panako.strategy.nfft.storage;

import java.util.List;

import be.panako.strategy.nfft.NFFTFingerprint;

public interface Storage {

	public abstract void addAudio(int identifier, String description);

	public abstract void audioObjectAdded(int numberOfSeconds);

	public abstract int getNumberOfFingerprints();

	public abstract String getAudioDescription(int identifier);

	public abstract int getNumberOfAudioObjects();

	public abstract double getNumberOfSeconds();

	public abstract boolean hasDescription(String description);

	public abstract float addFingerprint(int identifier, int time,
			int landmarkHash);

	public abstract List<NFFTFingerprintQueryMatch> getMatches(
			List<NFFTFingerprint> fingerprints, int size);

}