package be.panako.util;


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class AudioFileUtils {
    private final static Logger LOG = Logger.getLogger(AudioFileUtils.class.getName());

    private static final String NEWLINE = System.getProperty("line.separator");

    public static String run(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder result = new StringBuilder(80);
        String line;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            while ((line = in.readLine()) != null) {
                result.append(line).append(NEWLINE);
            }
        }
        process.waitFor();

        return result.toString();
    }

    public static float audioFileDurationInSeconds(File audioFile){
        String command = Config.get(Key.AUDIO_DURATION_COMMAND);
        String path = audioFile.getAbsolutePath();
        command = command.replace("%resource%",path);

        float duration = -1;
        try{
            String runtime = Config.get(Key.DECODER_PIPE_ENVIRONMENT);
            String envArg = Config.get(Key.DECODER_PIPE_ENVIRONMENT_ARG);
            String result = run(runtime,envArg,command);
            duration = Float.valueOf(result);
            LOG.info(String.format("Executed external command '%s' to find duration of %.3f",command,duration));
        }catch (IOException | InterruptedException | NumberFormatException ex){
            // Get the total duration, very inefficiently by decoding the
            AudioDispatcher d = AudioDispatcherFactory.fromPipe(path, 8000, 2048, 0);
            d.run();
            duration = d.secondsProcessed();
            LOG.warning(String.format("External command for audio duration failed. Decoded audio to find duration of %.3f s for '%s'",duration,path));
        }
        return duration;
    }
}
