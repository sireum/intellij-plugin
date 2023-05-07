mkdir -p lib
mkdir -p target/plugin/sireum-intellij-plugin/lib
rm -f lib/sireum.jar target/plugin/sireum-intellij-plugin/lib/sireum.jar
ln -s $SIREUM_HOME/bin/sireum.jar lib/sireum.jar
ln -s $SIREUM_HOME/bin/sireum.jar target/plugin/sireum-intellij-plugin/lib/sireum.jar
