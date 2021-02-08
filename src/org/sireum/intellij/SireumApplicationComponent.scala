/*
 Copyright (c) 2021, Robby, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sireum.intellij

import java.io._
import java.util.concurrent.BlockingQueue

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.components._
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileChooser._
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import org.sireum.intellij.logika.LogikaConfigurable

object SireumApplicationComponent {
  private val sireumKey = "org.sireum."
  private val sireumHomeKey = sireumKey.dropRight(1)
  private val sireumVarArgsKey = sireumKey + "vmargs"
  private val sireumEnvVarsKey = sireumKey + "envvars"
  private val sireumPluginVersionKey = sireumKey + "plugin.version"
  private val isDev: Boolean = "true" == System.getProperty("org.sireum.ive.dev")
  private val dev: String = if (isDev) "-dev" else ""

  private lazy val currentPluginVersion =
    PluginManager.getPlugin(
      PluginId.getId("org.sireum.intellij")).getVersion
  private[intellij] var sireumHomeOpt: Option[File] = None
  private[intellij] var vmArgs: Seq[String] = Vector("-Xss2m")
  private[intellij] var envVars = scala.collection.mutable.LinkedHashMap[String, String]()
  private[intellij] var pluginVersion: String = ""

  private var terminated: Boolean = false

  final def getSireumHome(project: Project = null): Option[File] = {
    if (sireumHomeOpt.isEmpty) {
      val env = System.getenv("SIREUM_HOME")
      sireumHomeOpt = checkSireumDir(env)
      if (sireumHomeOpt.isEmpty && SystemInfo.isWindows)
        sireumHomeOpt = checkSireumDir("C:\\Sireum" + dev)
      if (sireumHomeOpt.isEmpty && SystemInfo.isMac) {
        val appResources = s"/Applications/Sireum$dev.app/Contents/Resources/sireum-v3"
        sireumHomeOpt = checkSireumDir(appResources)
        if (sireumHomeOpt.isEmpty)
          sireumHomeOpt = checkSireumDir(System.getProperty("user.home") + appResources)
      }
      if (sireumHomeOpt.isEmpty && SystemInfo.isLinux)
        sireumHomeOpt = checkSireumDir(System.getProperty("user.home") + "/Applications/Sireum" + dev)
      if (sireumHomeOpt.isEmpty) {
        browseSireumHome(project).foreach(p =>
          sireumHomeOpt = checkSireumDir(p))
      }
    }
    sireumHomeOpt match {
      case Some(homeDir) =>
        checkSireumInSync(homeDir)
        saveConfiguration()
      case _ =>
    }
    sireumHomeOpt
  }

  private def isSource(homeDir: File): Boolean =
    currentPluginVersion.contains("-SNAPSHOT") ||
      new File(homeDir, "bin/detect-build.sh").exists

  private def checkSireumInSync(homeDir: File): Unit = {
    if (currentPluginVersion.contains("-SNAPSHOT")) {
      pluginVersion = ""
      return
    }
    if ("" == pluginVersion) pluginVersion = currentPluginVersion
    if (currentPluginVersion != pluginVersion && isSource(homeDir)) {
      Messages.showInfoMessage(
        s"""The Sireum IntelliJ plugin has been updated.
           |Please update Sireum through the command-line:
           |(1) do a git pull, and
           |(2) run Sireum again.""".stripMargin,
        "Sireum May Need Updating")
    }
    pluginVersion = currentPluginVersion
  }

  def sireumHomeString: String = sireumHomeOpt.map(_.getAbsolutePath).getOrElse("")

  def envVarsString: String = envVars.map(p => s"${p._1}=${p._2}").
    mkString(scala.util.Properties.lineSeparator)

  def vmArgsString: String = vmArgs.mkString(" ")

  def browseSireumHome(project: Project = null): Option[String] = {
    var pathOpt: Option[String] = None
    val desc = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    desc.setTitle("Select Sireum v3 directory")
    FileChooser.chooseFile(
      desc,
      project, null, t => pathOpt = Some(t.getCanonicalPath))
    pathOpt.foreach(path =>
      if (checkSireumDir(path).isEmpty)
        Messages.showMessageDialog(project, sireumInvalid(path),
          "Invalid Sireum Configuration", null)
    )
    pathOpt
  }

  def sireumInvalid(path: String): String =
    s"""Could not confirm a working Sireum installation in $path (with the specified VM arguments and environment variables in the settings).
       |Make sure to run Sireum at least once from the command-line.""".stripMargin

  def runSireum(project: Project, input: Option[String], args: String*): Option[String] =
    getSireumHome(project) match {
      case Some(d) => runSireum(d.getAbsolutePath, vmArgs, envVars, input, args)
      case _ => None
    }

  def getSireumProcess(project: Project,
                       queue: BlockingQueue[String],
                       processOutput: String => Unit,
                       args: String*): Option[scala.sys.process.Process] =
    getSireumHome(project) match {
      case Some(d) =>
        val javaPath = new File(d, "platform/java/bin/java").getAbsolutePath
        val sireumJarPath = new File(d, "bin/sireum.jar").getAbsolutePath
        None
        /* TODO
        Some(new Exec().process((javaPath +: vmArgs) ++
          Seq("-Dfile.encoding=UTF-8", "-jar", sireumJarPath) ++
          args, { os =>
          try {
            val w = new OutputStreamWriter(os)
            val lineSep = scala.util.Properties.lineSeparator
            while (!terminated) {
              val m = queue.take()
              w.write(m)
              w.write(lineSep)
              w.flush()
            }
          } catch {
            case _: InterruptedException =>
          } finally os.close()

        }
        , { is =>
          try {
            val r = new BufferedReader(new InputStreamReader(is))
            while (!terminated) {
              val line = r.readLine()
              if (line != null) {
                processOutput(line)
              }
            }
          } catch {
            case _: IOException =>
          } finally is.close()
        }, ("SIREUM_HOME", d.getAbsolutePath)))
         */
      case _ => None
    }

  private def runSireum(path: String,
                        vmArgs: Seq[String],
                        envVars: scala.collection.mutable.LinkedHashMap[String, String],
                        input: Option[String],
                        args: Seq[String]): Option[String] = {
    val d = new File(path)
    val javaPath = new File(d, "platform/java/bin/java").getAbsolutePath
    val sireumJarPath = new File(d, "bin/sireum.jar").getAbsolutePath
    if (path.trim == "") None
    else None
      /* TODO
      new Exec().run(0,
      (javaPath +: vmArgs) ++ Seq("-Dfile.encoding=UTF-8", "-jar",
        sireumJarPath) ++ args,
      input, envVars.toSeq :+ ("SIREUM_HOME", path): _*) match {
      case Exec.StringResult(s, _) => Some(s)
      case _ => None
    }

       */
  }

  private[intellij] final def
  checkSireumDir(path: String,
                 vmArgs: Seq[String] = this.vmArgs,
                 envVars: scala.collection.mutable.LinkedHashMap[String, String] = this.envVars): Option[File] = {
    if (path == null) return None
    runSireum(path, vmArgs, envVars, None, Seq()) match {
      case Some(s) =>
        if (s.linesIterator.exists(
          _.trim == "Sireum: A Software Analysis Platform (v3)")) {
          Some(new File(path))
        } else None
      case _ => None
    }
  }

  def parseEnvVars(text: String): Option[scala.collection.mutable.LinkedHashMap[String, String]] = {
    var r = scala.collection.mutable.LinkedHashMap[String, String]()
    for (l <- text.split('\n')) {
      val kv = l.split('=')
      if (kv.length != 2) return None
      val Array(k, v) = kv
      if (k.charAt(0).isDigit) return None
      if (!k.forall(c => c == '_' || c.isLetterOrDigit)) return None
      r += k -> v
    }
    Some(r)
  }

  def parseVmArgs(text: String): Option[Seq[String]] =
    if (text.trim == "") Some(Vector())
    else {
      val r = text.split(' ').toVector
      if (r.forall(_.head == '-')) Some(r)
      else None
    }

  def loadConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    envVars = Option(pc.getValue(sireumEnvVarsKey)).flatMap(parseEnvVars).getOrElse(scala.collection.mutable.LinkedHashMap())
    vmArgs = Option(pc.getValue(sireumVarArgsKey)).flatMap(parseVmArgs).getOrElse(Seq())
    sireumHomeOpt = Option(pc.getValue(sireumHomeKey)).flatMap(p => checkSireumDir(p, vmArgs, envVars))
    pluginVersion = Option(pc.getValue(sireumPluginVersionKey)).getOrElse("")
  }

  def saveConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    pc.setValue(sireumHomeKey, sireumHomeOpt.map(_.getAbsolutePath).orNull)
    pc.setValue(sireumEnvVarsKey, envVarsString)
    pc.setValue(sireumVarArgsKey, vmArgs.mkString(" "))
    pc.setValue(sireumPluginVersionKey, pluginVersion)
  }
}

import SireumApplicationComponent._

class SireumApplicationComponent extends ApplicationComponent {
  override val getComponentName: String = "Sireum Application"

  override def initComponent(): Unit = {
    SireumApplicationComponent.loadConfiguration()
    LogikaConfigurable.loadConfiguration()

    val suffix = "options/notifications.xml"
    Option(System.getProperty("org.sireum.ive")) match {
      case Some(name) =>
        val homeDir = new File(System.getProperty("user.home"))
        val notificationFileOpt =
          if (SystemInfo.isMac) {
            val f = new File(homeDir, s"Library/Preferences/$name/$suffix")
            if (f.exists) None else Some(f)
          } else if (SystemInfo.isLinux || SystemInfo.isWindows) {
            val f = new File(homeDir, s".$name/config/$suffix")
            if (f.exists) None else Some(f)
          } else None
        for (f <- notificationFileOpt) try {
          val fw = new java.io.FileWriter(f)
          fw.write(
            """<application>
              |  <component name="NotificationConfiguration">
              |    <notification groupId="Platform and Plugin Updates" displayType="STICKY_BALLOON" shouldLog="false" />
              |  </component>
              |</application>
              |""".stripMargin)
          fw.close()
        } catch {
          case _: Throwable =>
        }
      case _ =>
    }

    SireumApplicationComponent.sireumHomeOpt match {
      case Some(homeDir) =>
        if (!isSource(homeDir)) {
          val reinstall = try {
            /* TODO
            import org.sireum.util.jvm._
            val localVer = FileUtil.readFile(FileUtil.toUri(new File(homeDir, "bin/VER")))._1.trim
            val onlineVer = scala.io.Source.fromURL(s"http://files.sireum.org/sireum-v3$dev-VER").mkString.trim
            localVer != onlineVer
             */
            false
          } catch {
            case _: Throwable => false
          }
          if (reinstall)
            new Thread() {
              override def run(): Unit = {
                Thread.sleep(5000)
                Util.notify(new Notification("Sireum Logika", "Sireum Update",
                  s"A newer Sireum$dev version is available; please re-download/install.",
                  NotificationType.INFORMATION),
                  null, shouldExpire = false)
              }
            }.start()
        }
      case _ =>
    }
  }

  override def disposeComponent(): Unit = {
    SireumApplicationComponent.terminated = true
  }
}
