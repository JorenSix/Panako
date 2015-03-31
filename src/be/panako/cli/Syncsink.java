package be.panako.cli;

import java.io.File;

import javax.swing.UIManager;

import be.panako.ui.syncsink.SyncSinkFrame;

public class Syncsink extends Application {
	
	@Override
	public void run(final String... args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
				}
				SyncSinkFrame frame = new SyncSinkFrame();
				frame.setSize(800, 600);
				frame.setVisible(true);
				
				//add the files
				for(File file : getFilesFromArguments(args)){
					frame.openFile(file);
				}
			}
		});
	}
	
	@Override
	public String description() {
		return "Shows a user interface to synchronize audio/video/data files with respect to the reference.";
	}

	@Override
	public String synopsis() {
		return "[reference] [others...]";
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
