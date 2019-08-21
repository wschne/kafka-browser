package com.rewe.digital.model.connection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionSettings {
    private UUID id;
    private String name;
    private String bootstrapServer;
    private BrokerSecuritySettings securitySettings;

    public ConnectionSettings(final String name,
                              final String bootstrapServer) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.bootstrapServer = bootstrapServer;
    }

    public String getFileName() {
        return name.trim().replace(" ", "_") + ".json";
    }
}
