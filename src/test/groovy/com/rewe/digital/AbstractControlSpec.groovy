package com.rewe.digital


import javafx.stage.Stage
import org.testfx.api.FxToolkit
import org.testfx.framework.spock.ApplicationSpec
import spock.util.concurrent.PollingConditions

abstract class AbstractControlSpec extends ApplicationSpec {
    PollingConditions pollingConditions = new PollingConditions()

    @Override
    void init() throws Exception {
        FxToolkit.registerStage { new Stage() }
    }

    @Override
    void stop() throws Exception {
        FxToolkit.hideStage()
    }

    static {
//        if (Boolean.getBoolean("headless")) {
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("headless.geometry", "1600x1200-32");
//        }
    }
}
