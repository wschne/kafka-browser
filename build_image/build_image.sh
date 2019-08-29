#!/bin/bash

VERSION_TO_BE_USED=$1

if [ -z ${VERSION_TO_BE_USED} ]; then echo "Version of package is not provided. Quit"; exit 1; else echo "Version to be used ${VERSION_TO_BE_USED}"; fi

echo "Cleanup old artifacts"

rm -rf tmp/
mkdir tmp/

echo "Download JMODS"
./download_jmods.sh

echo "Build application"
cd ..
./gradlew clean assemble -x test
cd build_image/

./create_jvm.sh
./build_package.sh ${VERSION_TO_BE_USED}
