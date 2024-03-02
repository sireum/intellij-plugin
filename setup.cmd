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

val sireumJar = Os.path(Os.env("SIREUM_HOME").get) / "bin" / "sireum.jar"
val cwd = Os.slashDir
val lib = cwd / "lib"
val targetLib = cwd / "target" / "plugin" / "sireum-intellij-plugin" / "lib"
val libSireum = lib / sireumJar.name
val targetSireum = targetLib / sireumJar.name
val antlr = cwd / "src" / "main" / "java" / "org" / "antlr" / "intellij"
lib.mkdirAll()
targetLib.mkdirAll()
libSireum.removeAll()
targetSireum.removeAll()
libSireum.mklink(sireumJar)
targetSireum.mklink(sireumJar)
antlr.removeAll()
antlr.up.mkdirAll()
(cwd / "antlr4-intellij-adaptor" / "src" / "main" / "java" / "org" / "antlr" / "intellij").copyTo(antlr)
