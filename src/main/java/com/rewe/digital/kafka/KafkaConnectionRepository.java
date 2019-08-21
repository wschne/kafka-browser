package com.rewe.digital.kafka;

import com.rewe.digital.configuration.FileStorageRepository;
import com.rewe.digital.model.connection.ConnectionSettings;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@Named
public class KafkaConnectionRepository {
    private static final String CURRENTLY_USED_CONNECTION_NAME = "currently_used_connection.tmp";
    private static final String CONNECTIONS_DIR = "connections";

    private final FileStorageRepository fileStorageRepository;

    @Inject
    public KafkaConnectionRepository(FileStorageRepository fileStorageRepository) {
        this.fileStorageRepository = fileStorageRepository;
    }

    public void save(final ConnectionSettings connectionSettings) {
        save(connectionSettings, connectionSettings.getFileName());
    }

    public void setCurrentConnectionSettings(ConnectionSettings selectedSettings) {
        save(selectedSettings, CURRENTLY_USED_CONNECTION_NAME);
    }

    private void save(final ConnectionSettings connectionSettings, final String fileName) {
        this.fileStorageRepository.writeDataToFile(CONNECTIONS_DIR, fileName, connectionSettings);
    }

    public List<ConnectionSettings> getAll(){
        return fileStorageRepository.getDirectoryContent(CONNECTIONS_DIR, "json", ConnectionSettings.class);
    }

    public void delete(ConnectionSettings connectionSettings) {
        fileStorageRepository.deleteFile(CONNECTIONS_DIR, connectionSettings.getFileName());
    }

    public Optional<ConnectionSettings> getCurrentConnectionSettings() {
        return fileStorageRepository.getDataFromFile(CONNECTIONS_DIR, CURRENTLY_USED_CONNECTION_NAME, ConnectionSettings.class);
    }
}
