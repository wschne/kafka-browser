package com.rewe.digital.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@Named
public class FileStorageRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final File rootDir = new File(System.getProperty("user.home"), ".kafka-browser");

    public <T> void writeDataToFile(final String subdirectory, final String fileName, final T data) {
        final File destinationDir = new File(rootDir, subdirectory);
        writeDataToFile(fileName, data, destinationDir);
    }

    public <T> void writeDataToFile(final String fileName, final T data) {
        writeDataToFile(fileName, data, rootDir);
    }

    public <T> List<T> getDirectoryContent(final String subdirectory,
                                           final String fileType,
                                           final Class<T> objectType) {
        final File sourceDir = new File(rootDir, subdirectory);
        if (sourceDir.exists()) {
            File[] connectionFiles = sourceDir.listFiles((dir, name) -> name.endsWith(fileType));
            return Arrays.stream(connectionFiles)
                    .map(file -> toObject(file, objectType))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public <T> Optional<T> getDataFromFile(final String fileName,
                                           final Class<T> objectType) {

        final File sourceFile = new File(rootDir, fileName);
        return getFileContent(objectType, sourceFile);
    }

    public <T> Optional<T> getDataFromFile(final String subdirectory,
                                           final String fileName,
                                           final Class<T> objectType) {

        final File sourceFile = new File(new File(rootDir, subdirectory), fileName);
        return getFileContent(objectType, sourceFile);
    }

    public void deleteFile(final String subdirectory, final String fileName) {
        final File sourceDir = new File(rootDir, subdirectory);
        if (sourceDir.exists()) {
            new File(sourceDir, fileName).delete();
        }
    }

    private <T> void writeDataToFile(final String fileName,
                                     final T data,
                                     final File destinationDir) {
        try {
            final String settingsJson = objectMapper.writeValueAsString(data);

            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(destinationDir, fileName)));
            writer.write(settingsJson);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <T> Optional<T> getFileContent(Class<T> objectType, File sourceFile) {
        if (sourceFile.exists()) {
            return toObject(sourceFile, objectType);
        } else {
            return Optional.empty();
        }
    }

    private <T> Optional<T> toObject(final File file, final Class<T> objectType) {
        String settingsJson = getFileContent(file);
        try {
            return Optional.of(objectMapper.readValue(settingsJson, objectType));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private String getFileContent(final File file) {
        StringBuilder content = new StringBuilder();
        try {
            Scanner sc = new Scanner(file);

            while (sc.hasNextLine())
                content.append(sc.nextLine());

            return content.toString();
        } catch (FileNotFoundException e) {
            return "";
        }
    }
}
