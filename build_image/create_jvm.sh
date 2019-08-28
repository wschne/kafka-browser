#!/bin/bash

JAVA_FX_JMODS_FILE_NAME=javafx-jmods-11.0.2
CUSTOM_JVM_PATH=kafka_browser_jvm

$JAVA_HOME/bin/jlink --module-path $(pwd)/tmp/${JAVA_FX_JMODS_FILE_NAME} --add-modules java.se,javafx.fxml,javafx.web,javafx.media,javafx.swing --bind-services --output tmp/${CUSTOM_JVM_PATH}

