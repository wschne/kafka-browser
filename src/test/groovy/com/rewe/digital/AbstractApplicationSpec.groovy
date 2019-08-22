package com.rewe.digital

import com.gluonhq.ignite.guice.GuiceContext
import com.google.common.eventbus.EventBus
import com.rewe.digital.configuration.FileStorageRepository
import com.rewe.digital.gui.StageFactory
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.testfx.framework.spock.ApplicationSpec
import spock.util.concurrent.PollingConditions

abstract class AbstractApplicationSpec extends ApplicationSpec {
    public static final String USER_HOME_DIR = '/tmp/k-tests'

    static PollingConditions conditions = new PollingConditions()

    static GuiceContext context = new GuiceContext(this, { [new KafkaBrowserMain.GuiceModule()] });
    static StageFactory factory
    static EventBus eventBus
    static FileStorageRepository fileStorageRepository

    Stage stage
    Scene scene
    FXMLLoader loader

    abstract String getSceneFileToTest()

    static {
        System.setProperty("user.home", USER_HOME_DIR)

//        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("headless.geometry", "1600x1200-32");
//        }
    }

    @Override
    void start(Stage stage) throws Exception {
        this.stage = stage

        Stage stageToTest = factory.createStage(getSceneFileToTest(),
                "styles.css",
                "Consumer configuration");

        scene = stageToTest.getScene()
        loader = (FXMLLoader) scene.getUserData();

        stageToTest.show();
    }

    @Override
    void stop() throws Exception {
        super.stop()
        stage?.close()
    }

    def setupSpec() {
        context.init()
        factory = context.getInstance(StageFactory)
        eventBus = context.getInstance(EventBus)
        fileStorageRepository = context.getInstance(FileStorageRepository)
    }

    def setup() {
        new File(USER_HOME_DIR).deleteDir()
    }
}
