package com.rewe.digital

import com.gluonhq.ignite.guice.GuiceContext
import com.google.common.eventbus.EventBus
import com.rewe.digital.gui.StageFactory
import com.rewe.digital.kafka.KafkaConnectionRepository
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
    static KafkaConnectionRepository connectionRepository

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
        this.stage = factory.createStage(getSceneFileToTest(),
                "styles.css",
                "Name: ${getSceneFileToTest()}");

        scene = this.stage.getScene()
        loader = (FXMLLoader) scene.getUserData();

        this.stage.show();
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
        connectionRepository = context.getInstance(KafkaConnectionRepository)
    }

    def setup() {
        new File(USER_HOME_DIR).deleteDir()
    }
}
