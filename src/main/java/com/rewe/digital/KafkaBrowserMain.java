package com.rewe.digital;

import com.gluonhq.ignite.guice.GuiceContext;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.rewe.digital.messaging.KafkaMessageHandler;
import com.rewe.digital.messaging.MessageHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.spark.sql.SparkSession;
import org.fuin.ext4logback.LogbackStandalone;
import org.fuin.ext4logback.NewLogConfigFileParams;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

public class KafkaBrowserMain extends Application {

    @Inject
    private FXMLLoader fxmlLoader;

    private GuiceContext context = new GuiceContext(this, () -> Arrays.asList(new GuiceModule()));

    @Override
    public void start(Stage stage) throws Exception {
        context.init();

        final URL location = Objects.requireNonNull(getClass().getClassLoader().getResource("scenes/connections/overall_connections.fxml"));
        fxmlLoader.setLocation(location);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("styles.css").toExternalForm());

        new LogbackStandalone().init(new File("logback.xml"), new NewLogConfigFileParams("com.rewe.digital", "KafkaBrowserMain"));

        stage.setTitle("Kafka-Browser [Connections] v0.0.1");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    class GuiceModule extends AbstractModule {
        @Override
        protected void configure() {
            SparkSession sparkSession = SparkSession
                    .builder()
                    .master("local[*]")
                    .appName("Kafka-Browser")
                    .config("spark.driver.host", "localhost")
                    .config("spark.driver.bindAddress","127.0.0.1")
                    .getOrCreate();
            bind(SparkSession.class).toInstance(sparkSession);
            bind(EventBus.class).toInstance(new EventBus());
            bind(MessageHandler.class).to(KafkaMessageHandler.class).asEagerSingleton();
        }
    }
}
