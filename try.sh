#!/bin/bash -xe
DIR=`pwd`
cd $SIREUM_HOME/.settings/.SireumIVE/plugins/
rm -fR sireum-intellij-plugin
unzip $DIR/intellij-plugin.zip
cd sireum-intellij-plugin/lib
ln -s $SIREUM_HOME/bin/sireum.jar .