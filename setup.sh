mkdir -p lib
mkdir -p target/plugin/sireum-intellij-plugin/lib
rm -f lib/sireum.jar target/plugin/sireum-intellij-plugin/lib/sireum.jar
ln -s $SIREUM_HOME/bin/sireum.jar lib/sireum.jar
ln -s $SIREUM_HOME/bin/sireum.jar target/plugin/sireum-intellij-plugin/lib/sireum.jar
rm -fR src/main/java/org/antlr
mkdir -p src/main/java/org/antlr
cp -a antlr4-intellij-adaptor/src/main/java/org/antlr/intellij src/main/java/org/antlr/
