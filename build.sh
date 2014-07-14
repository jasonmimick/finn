rm -fr bin/**/*
javac -d bin -cp lib/uk.co.mmscomputing.device.twain.jar source/finn/*.java

jar cf bin/finn.jar -C bin .
