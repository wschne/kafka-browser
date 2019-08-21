package com.rewe.digital.model.connection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rewe.digital.configuration.EncryptionUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrokerSecuritySettings {
    private final static String KEY = "7o3djviewourztdfghbjdweliubdhjvejwngkuvzhjdsb";

    private BrokerSecurityType securityType;
    private String saslMechanism;
    private String sslTruststoreLocation;
    private String sslTruststorePassword;
    private String sslKeystoreLocation;
    private String sslKeystorePassword;
    private String sslKeyPassword;
    private String loginUser;
    private String loginPassword;

    public BrokerSecuritySettings(final BrokerSecurityType securityType,
                                  final String saslMechanism,
                                  final String sslTruststoreLocation,
                                  final String sslTruststorePassword,
                                  final String sslKeystoreLocation,
                                  final String sslKeystorePassword,
                                  final String sslKeyPassword,
                                  final String loginUser,
                                  final String loginPassword) {
        this.securityType = securityType;
        this.saslMechanism = saslMechanism;
        this.sslTruststoreLocation = sslTruststoreLocation;
        this.sslTruststorePassword = EncryptionUtils.encrypt(sslTruststorePassword, KEY);
        this.sslKeystoreLocation = sslKeystoreLocation;
        this.sslKeystorePassword = EncryptionUtils.encrypt(sslKeystorePassword, KEY);
        this.sslKeyPassword = EncryptionUtils.encrypt(sslKeyPassword, KEY);
        this.loginUser = loginUser;
        this.loginPassword = EncryptionUtils.encrypt(loginPassword, KEY);
    }

    @JsonIgnore
    public String getPlainSslTruststorePassword() {
        return EncryptionUtils.decrypt(sslTruststorePassword, KEY);
    }

    @JsonIgnore
    public String getPlainSslKeystorePassword() {
        return EncryptionUtils.decrypt(sslKeystorePassword, KEY);
    }

    @JsonIgnore
    public String getPlainSslKeyPassword() {
        return EncryptionUtils.decrypt(sslKeyPassword, KEY);
    }

    @JsonIgnore
    public String getPlainLoginPassword() {
        return EncryptionUtils.decrypt(loginPassword, KEY);
    }
}
