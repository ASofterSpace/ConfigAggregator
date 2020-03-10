#!/bin/bash

echo "Re-building with target Java 7 (such that the compiled .class files will be compatible with as many JVMs as possible)..."

cd src

# build build build!
javac -encoding utf8 -d ../bin -bootclasspath ../other/java7_rt.jar -source 1.7 -target 1.7 @sourcefiles.list

cd ..



echo "Creating the release file configAggregator.zip..."

mkdir release

cd release

mkdir configAggregator

# copy the main files
cp -R ../bin configAggregator
cp ../UNLICENSE configAggregator
cp ../README.md configAggregator
cp ../configAggregator.sh configAggregator
cp ../configAggregator.bat configAggregator

# convert \n to \r\n for the Windows files!
cd configAggregator
awk 1 ORS='\r\n' configAggregator.bat > rn
mv rn configAggregator.bat
cd ..

# create a version tag right in the zip file
cd configAggregator
version=$(./configAggregator.sh --version_for_zip)
echo "$version" > "$version"
cd ..

# zip it all up
zip -rq configAggregator.zip configAggregator

mv configAggregator.zip ..

cd ..
rm -rf release

echo "The file configAggregator.zip has been created in $(pwd)"
