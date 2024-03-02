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
val sireumHome = Os.path(Os.env("SIREUM_HOME").get)
val sireumJar = sireumHome / "bin" / "sireum.jar"
val plugins: Os.Path = Os.kind match {
  case Os.Kind.Mac => sireumHome / "bin" / "mac" / "idea" / "IVE.app" / "Contents" / "plugins"
  case Os.Kind.Linux => sireumHome / "bin" / "linux" / "idea" / "plugins"
  case Os.Kind.LinuxArm => sireumHome / "bin" / "linux" / "arm" / "idea" / "plugins"
  case Os.Kind.Win => sireumHome / "bin" / "win" / "idea" / "plugins"
  case _ => halt("Unsupported OS")
}
(cwd / "intellij-plugin.zip").unzipTo(plugins)
(plugins / "sireum-intellij-plugin" / "lib" / sireumJar.name).mklink(sireumJar)