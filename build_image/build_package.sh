#!/bin/bash

JPACKAGER_MAC=jdk.packager-osx.zip
JPACKAGER_LINUX=jdk.packager-linux.zip
JAVA_FX_JMODS_FILE_NAME=javafx-jmods-11.0.2
CUSTOM_JVM_PATH=kafka_browser_jvm

VERSION_TO_BE_USED=$1

if [ -z ${VERSION_TO_BE_USED} ]; then echo "Version of package is not provided. Quit"; exit 1; else echo "Version to be used ${VERSION_TO_BE_USED}"; fi

preparePackager(){
    cd jpackager
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
         unzip ${JPACKAGER_LINUX} -d ../tmp/jpackager/
         cd ..
    elif [[ "$OSTYPE" == "darwin"* ]]; then
         unzip ${JPACKAGER_MAC} -d ../tmp/jpackager/
         cd ..
    else
         echo "Unknown OS. Exit script"
         exit 1
    fi
}

getIconFilePath(){
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
         echo $(pwd)/../src/main/deploy/package/kafka_browser.png
    elif [[ "$OSTYPE" == "darwin"* ]]; then
         echo $(pwd)/../src/main/deploy/package/kafka_browser.icns
    else
         echo "Unknown OS. Exit script"
         exit 1
    fi
}

ICON_FILE_PATH=$(getIconFilePath)

echo "Start building package"
echo "Icon used: ${ICON_FILE_PATH}"

preparePackager

./tmp/jpackager/jpackager create-installer -i $(pwd)/../build/libs \
     -o $(pwd)/../build/native/ \
     -n kafka-browser \
     --module-path $(pwd)/tmp/${JAVA_FX_JMODS_FILE_NAME} \
     --add-modules javafx.fxml,javafx.web,javafx.media,javafx.swing,java.base \
     --runtime-image tmp/${CUSTOM_JVM_PATH} \
     --icon ${ICON_FILE_PATH} \
     --version ${VERSION_TO_BE_USED} \
     --arguments ${VERSION_TO_BE_USED} \
     --main-jar kafka-browser-1.0-SNAPSHOT.jar
