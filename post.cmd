::/*#! 2> /dev/null                                 #
@ 2>/dev/null # 2>nul & echo off & goto BOF         #
if [ -z ${SIREUM_HOME} ]; then                      #
  echo "Please set SIREUM_HOME env var"             #
  exit -1                                           #
fi                                                  #
exec ${SIREUM_HOME}/bin/sireum slang run "$0" "$@"  #
:BOF
setlocal
if not defined SIREUM_HOME (
  echo Please set SIREUM_HOME env var
  exit /B -1
)
%SIREUM_HOME%\bin\sireum.bat slang run "%0" %*
exit /B %errorlevel%
::!#*/
// #Sireum
import org.sireum._

val cwd = Os.slashDir
val pluginXml = Os.path("META-INF") / "plugin.xml"
val sireumPlugin = cwd / "sireum-intellij-plugin.zip"
val sipJar = Os.path("sireum-intellij-plugin") / "lib" / "sireum-intellij-plugin.jar"
(cwd / "intellij-plugin.zip").removeAll()
(cwd / "target" / "sireum-intellij-plugin-5.0.0-SNAPSHOT.zip").copyOverTo(cwd / "sireum-intellij-plugin.zip")
proc"7z d $sireumPlugin ${Os.path("sireum-intellij-plugin") / "lib" / "sireum.jar"}".at(cwd).runCheck()
val VER = ops.StringOps(proc"git log -n 1 --date=format:%Y%m%d --pretty=format:4.%cd.%h".at(cwd).runCheck().out).trim
proc"7z x $sireumPlugin $sipJar".at(cwd).runCheck()
proc"7z x $sipJar $pluginXml".at(cwd).runCheck()
pluginXml.writeOver(ops.StringOps(pluginXml.read).replaceAllLiterally("5.0.0-SNAPSHOT", VER))
proc"7z a $sipJar $pluginXml".at(cwd).runCheck()
proc"7z a $sireumPlugin $sipJar".at(cwd).runCheck()
pluginXml.removeAll()
Os.path("sireum-intellij-plugin").removeAll()
sireumPlugin.moveTo(cwd / "intellij-plugin.zip")
println()
println(s"Version: $VER")