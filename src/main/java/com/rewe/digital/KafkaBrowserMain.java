package com.rewe.digital;

import com.gluonhq.ignite.guice.GuiceContext;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.gui.launcher.AppLauncher;
import com.rewe.digital.messaging.KafkaMessageHandler;
import com.rewe.digital.messaging.MessageHandler;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.sql.SparkSession;
import org.fuin.ext4logback.LogbackStandalone;
import org.fuin.ext4logback.NewLogConfigFileParams;

import java.io.File;
import java.util.Arrays;

@Slf4j
public class KafkaBrowserMain extends Application {

    public static String APPLICATION_VERSION;

    public static void main(String[] args) {
        if (args != null && args.length >= 1) {
            APPLICATION_VERSION = args[0];
        }

        setupLogging();
        launch(args);
    }

    @Override
    public void start(Stage initStage) throws Exception {
        val appLauncher = new AppLauncher();
        appLauncher.initialize();

        val loadContextTask = new Task<GuiceContext>() {
            @Override
            protected GuiceContext call() throws Exception {
                updateMessage("Loading context.");
                GuiceContext context = new GuiceContext(this, () -> Arrays.asList(new GuiceModule()));
                context.init();

                return context;
            }
        };

        appLauncher.showSplash(initStage, loadContextTask, () -> showMainStage(loadContextTask.valueProperty()) );
        new Thread(loadContextTask).start();
    }

    private void showMainStage(ReadOnlyObjectProperty<GuiceContext> context) {
        val guiceContext = context.get();
        val stageFactory = guiceContext.getInstance(StageFactory.class);
        stageFactory.openOverallConnectionsStage(KafkaBrowserMain.APPLICATION_VERSION);
    }

    class GuiceModule extends AbstractModule {
        @Override
        protected void configure() {
            System.setProperty("spark.ui.enabled", "false");
            SparkSession sparkSession = SparkSession
                    .builder()
                    .master("local[*]")
                    .appName("Kafka-Browser")
                    .config("spark.driver.host", "localhost")
                    .config("spark.driver.bindAddress", "127.0.0.1")
                    .config("spark.sql.codegen.wholeStage", "false")
                    .getOrCreate();
            bind(SparkSession.class).toInstance(sparkSession);
            bind(EventBus.class).toInstance(new EventBus());
            bind(MessageHandler.class).to(KafkaMessageHandler.class).asEagerSingleton();
        }
    }

    private static void setupLogging() {
        val logFileFileName = "kafka-browser";
        val logbackXmlFile = new File("logback.xml");
        new LogbackStandalone().init(logbackXmlFile, new NewLogConfigFileParams("com.rewe.digital", logFileFileName));

        val r = Runtime.getRuntime();

        r.addShutdownHook(new Thread(() -> {
            final File logFile = new File(logFileFileName + ".log");
            if(logFile.exists()){
                logFile.delete();
            }
            if (logbackXmlFile.exists()) {
                logbackXmlFile.delete();
            }
        }) );
    }
}
