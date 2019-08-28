    #!/bin/bash

JPACKAGER_MAC=jdk.packager-osx.zip
JPACKAGER_LINUX=jdk.packager-linux.zip
JAVA_FX_JMODS_FILE_NAME=javafx-jmods-11.0.2
CUSTOM_JVM_PATH=kafka_browser_jvm

cd jpackager
if [[ "$OSTYPE" == "linux-gnu" ]]; then
     unzip ${JPACKAGER_LINUX} -d ../tmp/jpackager/

     cd ..

     echo "Start building package"
     ./tmp/jpackager/jpackager create-installer -i $(pwd)/../build/libs -o $(pwd)/../build/native/ -n kafka-browser --module-path $(pwd)/tmp/${JAVA_FX_JMODS_FILE_NAME} --add-modules javafx.fxml,javafx.web,javafx.media,javafx.swing,java.base --runtime-image tmp/${CUSTOM_JVM_PATH} --icon $(pwd)/../src/main/deploy/package/kafka_browser.png --main-jar kafka-browser-1.0-SNAPSHOT.jar
elif [[ "$OSTYPE" == "darwin"* ]]; then
     unzip ${JPACKAGER_MAC} -d ../tmp/jpackager/

     cd ..

     echo "Start building package"
     ./tmp/jpackager/jpackager create-installer -i $(pwd)/../build/libs -o $(pwd)/../build/native/ -n kafka-browser --module-path $(pwd)/tmp/${JAVA_FX_JMODS_FILE_NAME} --add-modules javafx.fxml,javafx.web,javafx.media,javafx.swing,java.base --runtime-image tmp/${CUSTOM_JVM_PATH} --icon $(pwd)/../src/main/deploy/package/kafka_browser.icns --main-jar kafka-browser-1.0-SNAPSHOT.jar
else
     echo "Unknown OS. Exit script"
     exit 1
fi
