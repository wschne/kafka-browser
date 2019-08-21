package com.rewe.digital.utils.kafka

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.SocatContainer
import org.testcontainers.utility.Base58

class SecuredKafkaContainer extends GenericContainer<SecuredKafkaContainer> {
    public static final int ZOOKEEPER_PORT = 2181;
    public static final int PLAINTEXT_PORT = 9092
    public static final int SSL_PORT = 9093
    public static final int SASL_SSL_PORT = 9193

    protected SocatContainer proxy;

    SecuredKafkaContainer() {
        super('wurstmeister/kafka:0.10.2.1');
        withNetwork(Network.newNetwork());
        withNetworkAliases("kafka-" + Base58.randomString(6));

        this.withExposedPorts(PLAINTEXT_PORT, SSL_PORT, SASL_SSL_PORT)

        this.withEnv('KAFKA_SSL_KEYSTORE_LOCATION', "/certs/docker.kafka.server.keystore.jks")
        this.withEnv('KAFKA_SSL_TRUSTSTORE_LOCATION', "/certs/docker.kafka.server.truststore.jks")
        this.withEnv('KAFKA_SSL_KEYSTORE_PASSWORD', "nodesinek")
        this.withEnv('KAFKA_SSL_KEY_PASSWORD', "nodesinek")
        this.withEnv('KAFKA_SSL_TRUSTSTORE_PASSWORD', "nodesinek")
        this.withEnv('KAFKA_SSL_CLIENT_AUTH', "required")
        this.withEnv('KAFKA_SECURITY_INTER_BROKER_PROTOCOL', "SASL_SSL")
        this.withEnv('KAFKA_CREATE_TOPICS', "test:1:1")
        this.withEnv('KAFKA_AUTO_CREATE_TOPICS_ENABLE', "true")
        this.withEnv('KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL', "PLAIN")
        this.withEnv('KAFKA_SASL_ENABLED_MECHANISMS', "PLAIN")
        this.withEnv('KAFKA_JMX_OPTS', "-Djava.security.auth.login.config=/etc/kafka/server-jaas.conf")
        this.withEnv('KAFKA_LISTENERS', "PLAINTEXT://:9092,SSL://:9093,SASL_SSL://:9193")
        this.withEnv('KAFKA_ADVERTISED_LISTENERS', "PLAINTEXT://localhost:9092,SSL://localhost:9093,SASL_SSL://localhost:9193")
        this.withEnv('KAFKA_ZOOKEEPER_CONNECT', 'zookeeper:2181')

        this.withClasspathResourceMapping('kafka_server_conf/certs', '/certs', BindMode.READ_ONLY)
        this.withClasspathResourceMapping('kafka_server_conf/server-jaas.conf', '/etc/kafka/server-jaas.conf', BindMode.READ_ONLY)
    }

    public String getSaslSslBootstrapServers() {
        def address = proxy.getContainerIpAddress()
        def plaintextPort = this.getMappedPort(SASL_SSL_PORT)
        return "${address}:${plaintextPort}"
    }

    public String getPlaintextBootstrapServers() {
        def address = proxy.getContainerIpAddress()
        def plaintextPort = this.getMappedPort(PLAINTEXT_PORT)
        return "${address}:${plaintextPort}"
    }

    @Override
    protected void doStart() {
        String networkAlias = getNetworkAliases().get(0);
        proxy = new SocatContainer()
                .withNetwork(getNetwork())
                .withTarget(PLAINTEXT_PORT, networkAlias)
                .withTarget(SSL_PORT, networkAlias)
                .withTarget(SASL_SSL_PORT, networkAlias)
                .withTarget(ZOOKEEPER_PORT, networkAlias);

        proxy.start();

        super.doStart();
    }

    @Override
    public void stop() {
        super.stop()
        proxy.stop()
    }

    @Override
    void close() {
        super.close()
    }
}
