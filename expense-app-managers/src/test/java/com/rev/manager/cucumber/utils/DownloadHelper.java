package com.rev.manager.cucumber.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class DownloadHelper {
    // private static final Path DOWNLOAD_DIR = Paths.get(System.getProperty("user.dir"),
    //                   "target",
    //                   "downloads");
    private static final Path DOWNLOAD_DIR = Paths.get("/downloads");

    public static void clearDownloadDirectory() {

        Path downloadDirectory = DriverFactory.getDownloadDirectory();

        try {
            Files.createDirectories(downloadDirectory);

            try (Stream<Path> paths = Files.list(downloadDirectory)) {

                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(
                                        "Unable to delete: " + path, e);
                            }
                        });
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to clear download directory.", e);
        }
    }
    
    // public static File waitForCsvDownload() {

    //     File folder = DOWNLOAD_DIR.toFile();

    //     long timeout = System.currentTimeMillis() + 10000;

    //     while (System.currentTimeMillis() < timeout) {

    //         File[] csvFiles = folder.listFiles(
    //                 (dir, name) -> name.endsWith(".csv"));

    //         if (csvFiles != null && csvFiles.length > 0) {
    //             return csvFiles[0];
    //         }
    //     }

    //     throw new RuntimeException("CSV was not downloaded.");
    // }
    public static File waitForCsvDownload(String filename) {

        File file = DOWNLOAD_DIR.resolve(filename).toFile();

        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout) {

            if (file.exists() && file.length() > 0) {
                return file;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException(
            "CSV was not downloaded to /downloads: " + filename
        );
    }
}
