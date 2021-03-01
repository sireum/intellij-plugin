::#! 2> /dev/null                                             #
@ 2>/dev/null # 2>nul & echo off & goto BOF                   #
if [ -z ${SIREUM_HOME} ]; then                                #
  echo "Please set SIREUM_HOME env var"                       #
  exit -1                                                     #
fi                                                            #
exec ${SIREUM_HOME}/bin/sireum slang run "$0" "$@"            #
:BOF
if not defined SIREUM_HOME (
  echo Please set SIREUM_HOME env var
  exit /B -1
)
%SIREUM_HOME%\bin\sireum.bat slang run "%0" %*
exit /B %errorlevel%
::!#
// #Sireum

import org.sireum._

val iconDir = Os.cwd / "resources" / "icon"
for (i <- 0 to 6) {
  val j = 6 - i
  (iconDir / s"sireum-$i@2x.png").copyOverTo(iconDir / s"sireum-$j@2x_dark.png")
  (iconDir / s"sireum-$i.png").copyOverTo(iconDir / s"sireum-${j}_dark.png")
}