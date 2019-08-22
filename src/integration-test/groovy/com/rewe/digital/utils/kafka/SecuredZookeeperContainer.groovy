package com.rewe.digital.utils.kafka

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer

class SecuredZookeeperContainer extends GenericContainer<SecuredZookeeperContainer> {
    SecuredZookeeperContainer() {
        super('wurstmeister/zookeeper:latest');
        containerName='zookeeper'
        withExposedPorts(2181)
        withEnv('JMX_OPTS', "-Djava.security.auth.login.config=/etc/kafka/server-jaas.conf")
        this.withClasspathResourceMapping('kafka_server_conf/server-jaas.conf', '/etc/kafka/server-jaas.conf', BindMode.READ_ONLY)
    }

    @Override
    public void stop() {
        super.stop()
    }

    @Override
    void close() {
        super.close()
    }
}
