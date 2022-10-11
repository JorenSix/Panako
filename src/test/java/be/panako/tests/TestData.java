package be.panako.tests;

import be.panako.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class TestData {

    private static final String DATASET_URL = "https://panako.be/releases/Panako-test-dataset/";

    public static List<File> referenceFiles(){
        String[] references = {"1051039.mp3" ,
                "1071559.mp3" ,
                "1075784.mp3" ,
                "11266.mp3" ,
                "147199.mp3" ,
                "173050.mp3" ,
                "189211.mp3" ,
                "297888.mp3" ,
                "612409.mp3" ,
                "852601.mp3" ,};
        return downloadDataset(references,"reference");
    }

    public static List<File> queryFiles(){
        String[] queries = {"1024035_55s-75s.mp3",
                "1051039_34s-54s.mp3",
                "1071559_60s-80s.mp3",
                "1075784_78s-98s.mp3",
                "11266_69s-89s.mp3",
                "132755_137s-157s.mp3",
                "147199_115s-135s.mp3",
                "173050_86s-106s.mp3",
                "189211_60s-80s.mp3",
                "295781_88s-108s.mp3",
                "297888_45s-65s.mp3",
                "361430_180s-200s.mp3",
                "371009_187s-207s.mp3",
                "378501_59s-79s.mp3",
                "384991_294s-314s.mp3",
                "432279_81s-101s.mp3",
                "43383_224s-244s.mp3",
                "478466_24s-44s.mp3",
                "602848_242s-262s.mp3",
                "604705_154s-174s.mp3",
                "612409_73s-93s.mp3",
                "824093_182s-202s.mp3",
                "84302_232s-252s.mp3",
                "852601_43s-63s.mp3",
                "96644_84s-104s.mp3"};
        return downloadDataset(queries,"queries");
    }

    private static List<File> downloadDataset(String filenames[] , String foldername){
        List<File> files = new ArrayList<>();
        for(String f : filenames) {
            String path = FileUtils.combine(FileUtils.temporaryDirectory(),f);

            if(cacheDownloadedFile(DATASET_URL + foldername + "/" + f , path))
                System.out.println("Successfully download " + f);
            else
                System.err.println("Failed to download " + f);

            files.add(new File(path));
        }

        return files;

    }
    private static boolean cacheDownloadedFile(String url,String targetLocation){
        if (!FileUtils.exists(targetLocation) || new File(targetLocation).length() < 100)
            return downloadFile(url, targetLocation);
        else{
            System.out.println("Using cached audio file " + targetLocation);
        }
        return true;
    }

    private static boolean downloadFile(String url,String targetLocation){

        System.out.println("Downloading " + url + " to " + targetLocation + " ...");
        URL uri = null;
        try {
            uri = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.err.println("Failed to download " + url);
            return false;
        }

        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(uri.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(targetLocation);
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileOutputStream.getChannel()
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (FileNotFoundException e) {
            System.err.println("Failed to download " + url);
            return false;
        } catch (IOException e) {
            System.err.println("Failed to download " + url);
            return false;
        }
        long fileSize = new File(targetLocation).length()/1024/1024;
        System.out.println("Downloaded " + url + " to " + targetLocation + " size " + fileSize + "MB" );
        return true;
    }

}
