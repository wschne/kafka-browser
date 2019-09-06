package com.rewe.digital.kafka

import com.rewe.digital.model.connection.BrokerSecuritySettings
import com.rewe.digital.model.connection.BrokerSecurityType
import com.rewe.digital.model.connection.ConnectionSettings
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import spock.lang.Specification

class KafkaPropertiesBuilderSpec extends Specification {
    def kafkaPropertiesBuilder = new KafkaPropertiesBuilder()

    def "Get properties for a plaintext configuration"() {
        given:
        def connectionSettings = new ConnectionSettings('simple_connection', 'localhost:2182')

        when:
        def properties = kafkaPropertiesBuilder.getKafkaProperties(connectionSettings)

        then:
        properties.size() == 4
        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] == connectionSettings.bootstrapServer
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] == "org.apache.kafka.common.serialization.StringDeserializer"
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] == "org.apache.kafka.common.serialization.StringDeserializer"
        properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] == "false"
    }

    def "Get properties for an sasl_ssl configuration"() {
        given:
        def securitySettings = new BrokerSecuritySettings(BrokerSecurityType.SASL_SSL,
                'PLAINTEXT',
                'trLocation',
                'trPw',
                'sslKeyLoc',
                'sslKeystorePw',
                'sslKeyPw',
                'login_user',
                'login_pw')
        def connectionSettings = new ConnectionSettings(UUID.randomUUID(),
                'simple_connection',
                'localhost:2182',
                securitySettings)

        when:
        def properties = kafkaPropertiesBuilder.getKafkaProperties(connectionSettings)

        then:
        properties.size() == 12
        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] == connectionSettings.bootstrapServer
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] == "org.apache.kafka.common.serialization.StringDeserializer"
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] == "org.apache.kafka.common.serialization.StringDeserializer"
        properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] == "false"
        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] == "SASL_SSL"
        properties[SaslConfigs.SASL_MECHANISM] == securitySettings.saslMechanism
        properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] == securitySettings.sslTruststoreLocation
        properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] == securitySettings.plainSslTruststorePassword
        properties[SaslConfigs.SASL_JAAS_CONFIG] == "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${securitySettings.loginUser}\" password=\"${securitySettings.plainLoginPassword}\";"
        properties[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] == securitySettings.sslKeystoreLocation
        properties[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] == securitySettings.plainSslKeystorePassword
        properties[SslConfigs.SSL_KEY_PASSWORD_CONFIG] == securitySettings.plainSslKeyPassword
    }

    def "Certain sasl_ssl properties can be empty, do not set them as kafka-properties"() {
        given:
        def securitySettings = new BrokerSecuritySettings(BrokerSecurityType.SASL_SSL,
                'PLAINTEXT',
                'trLocation',
                'trPw',
                '',
                '',
                '',
                'login_user',
                'login_pw')
        def connectionSettings = new ConnectionSettings(UUID.randomUUID(),
                'simple_connection',
                'localhost:2182',
                securitySettings)

        when:
        def properties = kafkaPropertiesBuilder.getKafkaProperties(connectionSettings)

        then:
        properties.size() == 9
        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] == connectionSettings.bootstrapServer
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] == "org.apache.kafka.common.serialization.StringDeserializer"
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] == "org.apache.kafka.common.serialization.StringDeserializer"
        properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] == "false"
        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] == "SASL_SSL"
        properties[SaslConfigs.SASL_MECHANISM] == securitySettings.saslMechanism
        properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] == securitySettings.sslTruststoreLocation
        properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] == securitySettings.plainSslTruststorePassword
        properties[SaslConfigs.SASL_JAAS_CONFIG] == "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${securitySettings.loginUser}\" password=\"${securitySettings.plainLoginPassword}\";"
        properties[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] == null
        properties[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] == null
        properties[SslConfigs.SSL_KEY_PASSWORD_CONFIG] == null
    }
}



