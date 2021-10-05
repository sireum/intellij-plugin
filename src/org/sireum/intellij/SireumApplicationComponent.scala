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

import com.intellij.AppTopics

import java.io._
import java.util.concurrent.BlockingQueue
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components._
import com.intellij.openapi.editor.{Document, Editor, EditorFactory}
import com.intellij.openapi.fileChooser._
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileDocumentManagerListener}
import com.intellij.openapi.project.{Project => IProject}
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import org.sireum.intellij.SireumClient.{getModifiedFiles, groupId, shutdownServer}
import org.sireum.intellij.logika.LogikaConfigurable

object SireumApplicationComponent {
  private val sireumKey = "org.sireum."
  private val sireumHomeKey = sireumKey.dropRight(1)
  private val sireumStartupKey = sireumKey + "startup"
  private val sireumVarArgsKey = sireumKey + "vmargs"
  private val sireumEnvVarsKey = sireumKey + "envvars"
  private val sireumCacheInputKey = sireumKey + "cacheInput"
  private val sireumCacheTypeKey = sireumKey + "cacheType"
  private val backgroundAnalysisKey = sireumKey + "background"
  private val idleKey = sireumKey + "idle"
  private val bgcoresKey = sireumKey + "bgcores"

  private val isDev: Boolean = "false" != System.getProperty("org.sireum.ive.dev")
  private val dev: String = if (isDev) "-dev" else ""

  private[intellij] lazy val maxCores: Int = Runtime.getRuntime.availableProcessors

  private[intellij] var sireumHomeOpt: Option[org.sireum.Os.Path] = None
  private[intellij] var vmArgs: Seq[String] = Vector("-Xss2m")
  private[intellij] var envVars = scala.collection.mutable.LinkedHashMap[String, String]()
  private[intellij] var backgroundAnalysis = 2
  private[intellij] var idle: Int = 1500
  private[intellij] var bgCores: Int = 1
  private[intellij] var cacheInput: Boolean = true
  private[intellij] var cacheType: Boolean = true
  private[intellij] var startup: Boolean = false

  private[intellij] val platform: String =
    if (scala.util.Properties.isMac) "mac"
    else if (scala.util.Properties.isLinux) "linux"
    else if (scala.util.Properties.isWin) "win"
    else "unsupported"

  private var terminated: Boolean = false

  final def getSireumHome(iproject: IProject = null): Option[org.sireum.Os.Path] = {
    import org.sireum._
    if (sireumHomeOpt.isEmpty) {
      val env = System.getenv("SIREUM_HOME")
      sireumHomeOpt = if (env == null) scala.None else checkSireumDir(Os.path(env))
      if (sireumHomeOpt.isEmpty && SystemInfo.isWindows) {
        sireumHomeOpt = checkSireumDir(Os.path("C:\\Sireum" + dev))
        if (sireumHomeOpt.isEmpty)
          sireumHomeOpt = checkSireumDir(Os.path(System.getProperty("user.home") + s"\\Applications\\Sireum$dev"))
      } else if (sireumHomeOpt.isEmpty && SystemInfo.isMac) {
        val appResources = s"/Applications/Sireum$dev.app/Contents/Resources/sireum"
        sireumHomeOpt = checkSireumDir(Os.path(appResources))
        if (sireumHomeOpt.isEmpty) {
          sireumHomeOpt = checkSireumDir(Os.path(System.getProperty("user.home") + appResources))
        }
      } else if (sireumHomeOpt.isEmpty && SystemInfo.isLinux) {
        sireumHomeOpt = checkSireumDir(Os.path(System.getProperty("user.home") + "/Applications/Sireum" + dev))
      }
      if (sireumHomeOpt.isEmpty) {
        browseSireumHome(iproject).foreach(p =>
          sireumHomeOpt = checkSireumDir(p))
      }
    }
    if (sireumHomeOpt.isDefined) saveConfiguration()
    sireumHomeOpt
  }

  def sireumHomeString: String = sireumHomeOpt.map(_.string.value).getOrElse("")

  def envVarsString: String = envVars.map(p => s"${p._1}=${p._2}").
    mkString(scala.util.Properties.lineSeparator)

  def vmArgsString: String = vmArgs.mkString(" ")

  def browseSireumHome(project: IProject = null): Option[org.sireum.Os.Path] = {
    var pathOpt: Option[org.sireum.Os.Path] = None
    val desc = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    desc.setTitle("Select Sireum directory")
    ApplicationManager.getApplication.invokeAndWait {
      () =>
        FileChooser.chooseFile(
          desc,
          project, null, (t: VirtualFile) => pathOpt = Some(Util.getPath(t)))
    }
    pathOpt.foreach(path =>
      if (checkSireumDir(path).isEmpty) {
        Util.notify(new Notification(
          groupId, "Invalid Sireum Configuration",
          sireumInvalid(path),
          NotificationType.ERROR), project, shouldExpire = true)
      }
    )
    pathOpt
  }

  def sireumInvalid(path: org.sireum.Os.Path): String =
    s"""Could not confirm a working Sireum installation in $path (with the specified VM arguments and environment variables in the settings).
       |Make sure to run Sireum at least once from the command-line.""".stripMargin

  def runSireum(project: IProject, input: Option[String], args: String*): Option[String] =
    getSireumHome(project) match {
      case Some(d) => runSireum(d, vmArgs, envVars, input, args)
      case _ => None
    }

  def getSireumProcess(project: IProject,
                       queue: BlockingQueue[Vector[String]],
                       processOutput: String => Unit,
                       args: Vector[String]): Option[scala.sys.process.Process] =
    getSireumHome(project) match {
      case Some(d) =>
        import org.sireum._
        val javaHome = d / "bin" / platform / "java"
        val javaPath = javaHome / "bin" / (if (Os.isWin) "java.exe" else "java")
        val sireumJarPath = d / "bin" / "sireum.jar"
        scala.Some(new Exec().process((javaPath.string.value +: vmArgs) ++
          Seq("-Dfile.encoding=UTF-8", "-Dorg.sireum.silenthalt=true", "-jar", sireumJarPath.string.value) ++
          args, { os =>
          try {
            val w = new OutputStreamWriter(os)
            val lineSep = scala.util.Properties.lineSeparator
            while (!terminated) {
              for (m <- queue.take()) {
                w.write(m)
                w.write(lineSep)
                w.flush()
              }
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
        }, ("SIREUM_HOME", d.string.value)))
      case _ => None
    }

  private def runSireum(d: org.sireum.Os.Path,
                        vmArgs: Seq[String],
                        envVars: scala.collection.mutable.LinkedHashMap[String, String],
                        input: Option[String],
                        args: Seq[String]): Option[String] = {
    import org.sireum._
    val javaHome = d / "bin" / platform / "java"
    val javaPath = javaHome / "bin" / (if (Os.isWin) "java.exe" else "java")
    val sireumJarPath = d / "bin" / "sireum.jar"
    if (d.string.value.trim == "") scala.None
    else {
      var proc = Os.proc(ISZ(((javaPath.string.value +: vmArgs) ++ Seq("-Dfile.encoding=UTF-8", "-jar",
        sireumJarPath.string.value) ++ args).map(String(_)): _*))
      proc = proc.env(ISZ(envVars.toSeq.map(p => (org.sireum.String(p._1), org.sireum.String(p._2))): _*) :+
        string"SIREUM_HOME" -> d.string)
      input match {
        case scala.Some(in) => proc = proc.input(in)
        case _ =>
      }
      val r = proc.run()
      if (r.ok) scala.Some(r.out.value) else scala.None
    }
  }

  private[intellij] final def
  checkSireumDir(path: org.sireum.Os.Path,
                 vmArgs: Seq[String] = this.vmArgs,
                 envVars: scala.collection.mutable.LinkedHashMap[String, String] = this.envVars): Option[org.sireum.Os.Path] = {
    if (path == null) return None
    runSireum(path, vmArgs, envVars, None, Seq()) match {
      case Some(s) =>
        if (s.linesIterator.exists(
          _.trim == "Sireum: A High Assurance System Engineering Platform")) {
          Some(path)
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
    sireumHomeOpt = Option(pc.getValue(sireumHomeKey)).flatMap(p => checkSireumDir(org.sireum.Os.path(p), vmArgs, envVars))
    startup = pc.getBoolean(sireumStartupKey, startup)
    vmArgs = Option(pc.getValue(sireumVarArgsKey)).flatMap(parseVmArgs).getOrElse(Seq())
    envVars = Option(pc.getValue(sireumEnvVarsKey)).flatMap(parseEnvVars).getOrElse(scala.collection.mutable.LinkedHashMap())
    cacheInput = pc.getBoolean(sireumCacheInputKey, cacheInput)
    cacheType = pc.getBoolean(sireumCacheTypeKey, cacheType)
    backgroundAnalysis = pc.getInt(backgroundAnalysisKey, backgroundAnalysis)
    idle = pc.getInt(idleKey, idle)
    bgCores = pc.getInt(bgcoresKey, bgCores)
  }

  def saveConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    pc.setValue(sireumHomeKey, sireumHomeOpt.map(_.string.value).orNull)
    pc.setValue(sireumStartupKey, startup.toString)
    pc.setValue(sireumVarArgsKey, vmArgs.mkString(" "))
    pc.setValue(sireumEnvVarsKey, envVarsString)
    pc.setValue(sireumCacheInputKey, cacheInput.toString)
    pc.setValue(sireumCacheTypeKey, cacheType.toString)
    pc.setValue(backgroundAnalysisKey, backgroundAnalysis.toString)
    pc.setValue(idleKey, idle.toString)
    pc.setValue(bgcoresKey, bgCores.toString)
  }
}

class SireumApplicationComponent extends ApplicationComponent {
  override val getComponentName: String = "Sireum Application"

  override def initComponent(): Unit = {
    SireumApplicationComponent.loadConfiguration()
    LogikaConfigurable.loadConfiguration()

    ApplicationManager.getApplication.getMessageBus.connect.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
        new FileDocumentManagerListener {
          override def beforeDocumentSaving(d: Document): Unit = if (SireumApplicationComponent.backgroundAnalysis == 1) {
            val file = getFile(d)
            val editor = getEditor(d)
            if (file == null || editor == null || !editor.getProject.isInitialized) return
            val project = editor.getProject
            SireumClient.analyzeOpt(project, file, editor, SireumClient.getCurrentLine(editor),
              getModifiedFiles(project, file), isBackground = true)
          }

          def getFile(d: Document): VirtualFile = {
            if (d == null) return null
            val instance = FileDocumentManager.getInstance
            if (instance == null) return null
            val f = instance.getFile(d)
            try {
              Util.getPath(f)
              f
            } catch {
              case _: Throwable => null
            }
          }

          def getEditor(d: Document): Editor = {
            val editors = EditorFactory.getInstance().getEditors(d)
            if (editors.nonEmpty) {
              return editors(0)
            }
            return null
          }
        }
      )

  }

  override def disposeComponent(): Unit = {
    SireumApplicationComponent.terminated = true
    shutdownServer()
    Util.finalise()
  }
}
