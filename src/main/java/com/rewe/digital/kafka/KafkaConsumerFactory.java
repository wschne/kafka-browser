package com.rewe.digital.kafka;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.rewe.digital.model.connection.ConnectionSettings;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Named
@Slf4j
public class KafkaConsumerFactory {

    private final KafkaConnectionRepository kafkaConnectionRepository;
    private final KafkaConsumerWrapper kafkaConsumerWrapper;
    private final KafkaPropertiesBuilder kafkaPropertiesBuilder;

    private static Map<String, KafkaConsumer> connectedConsumer = new HashMap<>();

    @Inject
    public KafkaConsumerFactory(final KafkaConnectionRepository kafkaConnectionRepository,
                                final KafkaConsumerWrapper kafkaConsumerWrapper,
                                final KafkaPropertiesBuilder kafkaPropertiesBuilder) {
        this.kafkaConnectionRepository = kafkaConnectionRepository;
        this.kafkaConsumerWrapper = kafkaConsumerWrapper;
        this.kafkaPropertiesBuilder = kafkaPropertiesBuilder;
    }

    public KafkaConsumer<String, String> get() {
        final ConnectionSettings connectionSettings = kafkaConnectionRepository.getCurrentConnectionSettings().orElseThrow(() -> {
            final String errorMessage = "Cant get connection properties without a valid connection selected";
            return new IllegalStateException(errorMessage);
        });
        val settingsKey = connectionSettings.getId().toString();
        if (!connectedConsumer.containsKey(settingsKey)) {
            val properties = kafkaPropertiesBuilder.getKafkaProperties(connectionSettings);
            val consumer = kafkaConsumerWrapper.getInstance(properties);
            connectedConsumer.put(settingsKey, consumer);
        }
        return connectedConsumer.get(settingsKey);
    }

    public boolean checkConnection(ConnectionSettings connectionSettings) {
        Properties properties = kafkaPropertiesBuilder.getKafkaProperties(connectionSettings);

        TimeLimiter limiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());
        try {
            return limiter.callWithTimeout(() -> {
                try {
                    KafkaConsumer<String, String> consumer = kafkaConsumerWrapper.getInstance(properties);
                    consumer.listTopics(Duration.ofSeconds(2));
                    consumer.close();
                    return true;
                } catch (Exception e) {
                    log.error("Error while trying to connect to a kafka-cluster.", e);
                    return false;
                }
            }, 15, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            log.error("Error while trying to connect to a kafka-cluster.", e);
            return false;
        }
    }
}
