#!/bin/bash

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
./build_package.sh
