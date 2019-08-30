#!/bin/bash

JAVA_FX_JMODS_MAC=javafx-13-ea-jmods-mac
JAVA_FX_JMODS_LINUX=javafx-13-ea-jmods-linux

downloadAndExtractFile()
{
     name=$1
     wget -q -O tmp/${name}.zip http://gluonhq.com/download/${name}
     unzip -d tmp/ tmp/${name}.zip
     rm -rf tmp/${name}.zip
}

if [[ "$OSTYPE" == "linux-gnu" ]]; then
     downloadAndExtractFile ${JAVA_FX_JMODS_LINUX}
elif [[ "$OSTYPE" == "darwin"* ]]; then
     downloadAndExtractFile ${JAVA_FX_JMODS_MAC}
else
     echo "Unknown OS. Exit script"
     exit 1
fi

