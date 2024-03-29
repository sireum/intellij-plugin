#!/bin/bash -e
rm -fR intellij-plugin.zip
cp target/sireum-intellij-plugin-5.0.0-SNAPSHOT.zip sireum-intellij-plugin.zip
7z d sireum-intellij-plugin.zip sireum-intellij-plugin/lib/sireum.jar
VER=$(git log -n 1 --date=format:%Y%m%d --pretty=format:4.%cd.%h)
7z x sireum-intellij-plugin.zip sireum-intellij-plugin/lib/sireum-intellij-plugin.jar
7z x sireum-intellij-plugin/lib/sireum-intellij-plugin.jar META-INF/plugin.xml
sed -i.bak "s/5.0.0-SNAPSHOT/${VER}/g" META-INF/plugin.xml
7z a sireum-intellij-plugin/lib/sireum-intellij-plugin.jar META-INF/plugin.xml
7z a sireum-intellij-plugin.zip sireum-intellij-plugin/lib/sireum-intellij-plugin.jar
rm -fR META-INF sireum-intellij-plugin
mv sireum-intellij-plugin.zip intellij-plugin.zip
echo ""
echo "Version: ${VER}"