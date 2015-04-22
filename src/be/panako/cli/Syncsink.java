package be.panako.cli;

import java.awt.Dimension;
import java.awt.Toolkit;
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
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
				
				//add the files
				int i = 0;
				for(File file : getFilesFromArguments(args)){
					frame.openFile(file,i);
					i++;
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
