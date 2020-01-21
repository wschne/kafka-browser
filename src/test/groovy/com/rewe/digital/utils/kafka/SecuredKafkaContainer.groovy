package com.rewe.digital.utils.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.SocatContainer
import org.testcontainers.utility.Base58

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG

class SecuredKafkaContainer extends GenericContainer<SecuredKafkaContainer> {
    public static final int ZOOKEEPER_PORT = 2181;
    public static final int PLAINTEXT_PORT = 9091
    public static final int SSL_PORT = 9093
    public static final int SASL_SSL_PORT = 9193

    protected SocatContainer proxy;

    private final int plaintextPort;
    private final int zookeeperPort;
    private final int sslPort;
    private final int saslSslPort;
    private final List<String> testTopics = []
    private KafkaConsumer consumer

    SecuredKafkaContainer(plaintextPort = PLAINTEXT_PORT,
                          zookeeperPort = ZOOKEEPER_PORT,
                          sslPort = SSL_PORT,
                          saslSslPort = SASL_SSL_PORT,
                          String testTopics = 'test:1:1') {
        super('wurstmeister/kafka:2.12-2.4.0');

        this.testTopics = testTopics.split(',')
        this.plaintextPort = plaintextPort;
        this.zookeeperPort = zookeeperPort;
        this.sslPort = sslPort;
        this.saslSslPort = saslSslPort;

        withNetwork(Network.newNetwork());
        withNetworkAliases("kafka-" + Base58.randomString(6));

        this.withExposedPorts(plaintextPort, sslPort, saslSslPort)

        this.withEnv('KAFKA_SSL_KEYSTORE_LOCATION', "/certs/docker.kafka.server.keystore.jks")
        this.withEnv('KAFKA_SSL_TRUSTSTORE_LOCATION', "/certs/docker.kafka.server.truststore.jks")
        this.withEnv('KAFKA_SSL_KEYSTORE_PASSWORD', "nodesinek")
        this.withEnv('KAFKA_SSL_KEY_PASSWORD', "nodesinek")
        this.withEnv('KAFKA_SSL_TRUSTSTORE_PASSWORD', "nodesinek")
        this.withEnv('KAFKA_SSL_CLIENT_AUTH', "required")
        this.withEnv('KAFKA_PORT', plaintextPort as String)
        this.withEnv('KAFKA_CREATE_TOPICS', testTopics as String)
        this.withEnv('KAFKA_AUTO_CREATE_TOPICS_ENABLE', "true")
        this.withEnv('KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL', "PLAIN")
        this.withEnv('KAFKA_SASL_ENABLED_MECHANISMS', "PLAIN")
        this.withEnv('KAFKA_JMX_OPTS', "-Djava.security.auth.login.config=/etc/kafka/server-jaas.conf")
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_SSL:SASL_SSL");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
        this.withEnv('KAFKA_LISTENERS', "PLAINTEXT://0.0.0.0:${plaintextPort},SSL://0.0.0.0:${sslPort},SASL_SSL://0.0.0.0:${saslSslPort},BROKER://0.0.0.0:9092")
        this.withEnv('KAFKA_ZOOKEEPER_CONNECT', "zookeeper:${zookeeperPort}")

        this.withClasspathResourceMapping('kafka_server_conf/certs', '/certs', BindMode.READ_ONLY)
        this.withClasspathResourceMapping('kafka_server_conf/server-jaas.conf', '/etc/kafka/server-jaas.conf', BindMode.READ_ONLY)
    }

    public String getSaslSslBootstrapServers() {
        def address = proxy.getContainerIpAddress()
        def plaintextPort = proxy.getMappedPort(this.saslSslPort)
        return "${address}:${plaintextPort}"
    }

    public String getSslBootstrapServers() {
        def address = proxy.getContainerIpAddress()
        def plaintextPort = proxy.getMappedPort(sslPort)
        return "${address}:${plaintextPort}"
    }

    public String getPlaintextBootstrapServers() {
        def address = proxy.getContainerIpAddress()
        def plaintextPort = proxy.getFirstMappedPort()
        return "${address}:${plaintextPort}"
    }

    public boolean isReady() {
        try {
            def topics = getConsumer().listTopics()
            this.running && topics.size() == testTopics.size()
        } catch (ConnectException) {
            return false
        }
    }

    private KafkaConsumer getConsumer() {
        Properties properties = new Properties();
        properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, this.getPlaintextBootstrapServers());

        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        this.consumer = new KafkaConsumer(properties)
        consumer
    }

    @Override
    protected void doStart() {
        String networkAlias = getNetworkAliases().get(0);
        proxy = new SocatContainer()
                .withNetwork(getNetwork())
                .withTarget(this.plaintextPort, networkAlias)
                .withTarget(this.sslPort, networkAlias)
                .withTarget(this.saslSslPort, networkAlias)
                .withTarget(this.zookeeperPort, networkAlias);

        proxy.start();
        withEnv("KAFKA_ADVERTISED_LISTENERS", "BROKER://" + networkAlias + ":9092" + "," + "PLAINTEXT://${getPlaintextBootstrapServers()},SSL://${getSslBootstrapServers()},SASL_SSL://${getSaslSslBootstrapServers()}");

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
