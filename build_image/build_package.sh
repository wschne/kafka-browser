#!/bin/bash

JPACKAGER_MAC=jdk.packager-osx.zip
JPACKAGER_LINUX=jdk.packager-linux.zip
JAVA_FX_JMODS_FILE_NAME=javafx-jmods-11.0.2
CUSTOM_JVM_PATH=kafka_browser_jvm

cd jpackager
if [[ "$OSTYPE" == "linux-gnu" ]]; then
     unzip ${JPACKAGER_LINUX} -d ../tmp/jpackager/
elif [[ "$OSTYPE" == "darwin"* ]]; then
     unzip ${JPACKAGER_MAC} -d ../tmp/jpackager/
else
     echo "Unknown OS. Exit script"
     exit 1
fi

cd ..

echo "Start building package"
./tmp/jpackager/jpackager create-installer -i $(pwd)/../build/libs -o $(pwd)/../build/native/ -n kafka-browser --module-path $(pwd)/tmp/${JAVA_FX_JMODS_FILE_NAME} --add-modules javafx.fxml,javafx.web,javafx.media,javafx.swing,java.base --runtime-image tmp/${CUSTOM_JVM_PATH} --main-jar kafka-browser-1.0-SNAPSHOT.jar
