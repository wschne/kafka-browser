package com.rewe.digital.kafka;

import com.rewe.digital.model.connection.BrokerSecurityType;
import com.rewe.digital.model.connection.ConnectionSettings;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEY_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;

@Named
public class KafkaPropertiesBuilder {
    public Properties getKafkaProperties(final ConnectionSettings connectionSettings) {
        final String bootstrapServer = connectionSettings.getBootstrapServer();
        Properties properties = new Properties();
        properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);

        properties.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "false");

        if (connectionSettings.getSecuritySettings() != null &&
                connectionSettings.getSecuritySettings().getSecurityType() == BrokerSecurityType.SASL_SSL) {
            val jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
            val securitySettings = connectionSettings.getSecuritySettings();
            properties.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            properties.put(SASL_MECHANISM, securitySettings.getSaslMechanism());
            properties.put(SSL_TRUSTSTORE_LOCATION_CONFIG, securitySettings.getSslTruststoreLocation());
            properties.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, securitySettings.getPlainSslTruststorePassword());
            properties.put(SASL_JAAS_CONFIG, String.format(jaasConfig, securitySettings.getLoginUser(), securitySettings.getPlainLoginPassword()));
            if (StringUtils.isNotBlank(securitySettings.getSslKeystoreLocation())) {
                properties.put(SSL_KEYSTORE_LOCATION_CONFIG, securitySettings.getSslKeystoreLocation());
            }
            if (StringUtils.isNotBlank(securitySettings.getPlainSslKeystorePassword())) {
                properties.put(SSL_KEYSTORE_PASSWORD_CONFIG, securitySettings.getPlainSslKeystorePassword());
            }
            if (StringUtils.isNotBlank(securitySettings.getPlainSslKeyPassword())) {
                properties.put(SSL_KEY_PASSWORD_CONFIG, securitySettings.getPlainSslKeyPassword());
            }
        }

        return properties;
    }
}
