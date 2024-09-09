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

import com.intellij.notification.{Notification, NotificationListener, NotificationType, Notifications}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import javax.swing.event.HyperlinkEvent

object Util {
  def getPath(file: VirtualFile): Option[org.sireum.Os.Path] =
    try Some(org.sireum.Os.path(file.toNioPath.toFile.getCanonicalPath))
    catch { case _: Throwable => return None }

  def getFilePath(project: Project): Option[org.sireum.Os.Path] = {
    if (project.isDisposed) {
      return None
    }
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor == null) return None
    val fdm = FileDocumentManager.getInstance
    val file = fdm.getFile(editor.getDocument)
    if (file == null) return None
    getPath(file)
  }

  def getFileExt(project: Project): String = {
    getFilePath(project) match {
      case Some(path) => path.ext.value
      case _ => ""
    }
  }

  def isSireumOrLogikaFile(project: Project)(content: => org.sireum.String): (Boolean, Boolean) =
    getFilePath(project) match {
      case Some(p) => isSireumOrLogikaFile(p)(content)
      case _ => (false, false)
    }

  def isSysMLv2File(project: Project): Boolean =
    getFilePath(project) match {
      case Some(p) => p.ext.value == "sysml"
      case _ => false
    }

  def isProyek(project: Project): Boolean = {
    val root = org.sireum.Os.path(project.getBasePath)
    return (root / "bin" / "project.cmd").isFile
  }

  def genProyek(iproject: Project, root: org.sireum.Os.Path): Unit = {
    import org.sireum._
    val prjCmd = root / "bin" / "project.cmd"
    if (!prjCmd.exists) {
      prjCmd.up.mkdirAll()
      val src = root / "src"
      if (!src.exists) {
        src.mkdirAll()
      }
      val name = iproject.getName
      val bslash = "\\"
      val text =
        st"""::/*#! 2> /dev/null                                 #
            |@ 2>/dev/null # 2>nul & echo off & goto BOF         #
            |if [ -z $${SIREUM_HOME} ]; then                      #
            |  echo "Please set SIREUM_HOME env var"             #
            |  exit -1                                           #
            |fi                                                  #
            |exec $${SIREUM_HOME}/bin/sireum slang run "$$0" "$$@"  #
            |:BOF
            |setlocal
            |if not defined SIREUM_HOME (
            |  echo Please set SIREUM_HOME env var
            |  exit /B -1
            |)
            |%SIREUM_HOME%${bslash}bin${bslash}sireum.bat slang run "%0" %*
            |exit /B %errorlevel%
            |::!#*/
            |// #Sireum
            |
            |import org.sireum._
            |import org.sireum.project.{Module, Project, Target}
            |
            |val home = Os.slashDir.up.canon
            |
            |val m = Module(
            |  id = "$name",
            |  basePath = home.string,
            |  subPathOpt = None(),
            |  deps = ISZ(),
            |  targets = ISZ(Target.Jvm),
            |  ivyDeps = ISZ("org.sireum.kekinian::library:"),
            |  sources = ISZ(Os.path("src").string),
            |  resources = ISZ(),
            |  testSources = ISZ(),
            |  testResources = ISZ(),
            |  publishInfoOpt = None()
            |)
            |
            |val prj = Project.empty + m
            |
            |println(project.JSON.fromProject(prj, T))""".render
      prjCmd.write(text.value.replace("\n", "\r\n"))
      prjCmd.chmod("+x")
    }
  }

  def hasSlang(root: org.sireum.Os.Path): Boolean = {
    import org.sireum._
    var found = F
    def rec(dir: Os.Path): Unit = {
      for (p <- dir.list if !found) {
        if (p.isDir) {
          rec(p)
        } else {
          if (p.ext.value == "sc" || p.ext.value == "scala") {
            for (line <- p.readLineStream.take(1)) {
              if (lang.parser.SlangParser.detectSlang(Some(p.toUri), line)._1) {
                found = T
              }
            }
          }
        }
      }
    }
    rec(root)
    found
  }

  def recommendReload(iproject: Project): Boolean = {
    import org.sireum._
    val root = Os.path(iproject.getBasePath)
    if (!isProyek(iproject)) {
      if (!(root / "build.sbt").exists && !(root / "build.sc").exists && hasSlang(root)) {
        Util.notify(Util.notification(SireumClient.groupId, "Generate proyek?",
          """<p>Proyek definition is not defined. <a href="">Generate</a>?</p>""",
          NotificationType.INFORMATION, (_: Notification, _: HyperlinkEvent) => {
            genProyek(iproject, root)
            ProyekSyncAction.sync(iproject)
          }), iproject, scala.None)
      }
      return false
    }
    val sireumHome = SireumApplicationComponent.getSireumHome(iproject) match {
      case scala.Some(home) =>
        System.setProperty("org.sireum.home", home.string.value)
        home
      case _ => return false
    }
    val prjOpt = org.sireum.proyek.Proyek.getProject(sireumHome, root, None(), None())
    val projectF = root / ".idea" / "project.txt"
    projectF.removeAll()
    if (prjOpt.isEmpty) {
      projectF.writeOver("Could not load project\n")
      projectF.removeOnExit()
      return true
    }
    val prj = prjOpt.get
    val vsOpt = org.sireum.proyek.Proyek.getVersions(prj, root, ISZ(), SireumApi.versions.entries)
    if (vsOpt.isEmpty) {
      projectF.writeAppend("Could not load versions\n")
      projectF.removeOnExit()
      return true
    }
    val versions = vsOpt.get
    val projectJson = root / ".idea" / "project.json"
    val versionsJson = root / ".idea" / "versions.json"
    project.ProjectUtil.load(projectJson) match {
      case Some(prjCache) =>
        if (prjCache != prj) {
          projectF.writeOver({
            import org.sireum._
            st"""Project definition changes detected:
                |Cached: $prjCache
                |Loaded: $prj""".render
          })
          val backup = projectJson.up / "project.old.json"
          projectJson.moveOverTo(backup)
          return true
        }
      case _ =>
        projectF.writeOver({
          import org.sireum._
          st"""No existing project definition cache at: $projectJson""".render
        })
        return true
    }
    proyek.Proyek.loadVersions(versionsJson) match {
      case Some(versionsCache) =>
        if (versionsCache != versions) {
          projectF.writeOver({
            import org.sireum._
            st"""Versions changes detected:
                |Cached: $versionsCache
                |Loaded: $versions""".render
          })
          val backup = versionsJson.up / "versions.old.json"
          versionsJson.moveOverTo(backup)
          return true
        }
      case _ =>
        projectF.writeOver({
          import org.sireum._
          st"""No versions cache at: $versionsJson""".render
        })
        return true
    }
    false
  }

  def isSireumOrLogikaFile(path: org.sireum.Os.Path)(content: => org.sireum.String = path.read): (Boolean, Boolean) = {
    if (!path.exists) return (false, false)
    val p = path.string.value
    val (hasSireum, compactFirstLine, _) = org.sireum.lang.parser.SlangParser.detectSlang(org.sireum.Some(p), content)
    return (hasSireum, compactFirstLine.contains("#Logika") || path.ext.value == "logika")
  }

  def notifyDebug(content: String, project: Project): Unit = notify(new Notification(SireumClient.groupId, content,
    NotificationType.INFORMATION), project, None)

  def notify(n: Notification, project: Project, shouldExpire: Boolean): Unit =
    notify(n, project, if (shouldExpire) Some(5000) else None)

  def notify(n: Notification, project: Project, expireOpt: Option[Int]): Unit =
    expireOpt match {
      case Some(time) =>
        new Thread() {
          override def run(): Unit = {
            Notifications.Bus.notify(n, project)
            Thread.sleep(time)
            ApplicationManager.getApplication.invokeLater(() => n.expire())
          }
        }.start()
      case _ =>
        Notifications.Bus.notify(n, project)
    }

  def notification(groupId: String,
                   title: String,
                   content: String,
                   tipe: NotificationType,
                   listener: NotificationListener): Notification = {
    val n = new Notification(groupId, title, content, tipe)
    n.setListener(listener)
    n
  }

  val queue: java.util.concurrent.LinkedBlockingQueue[Option[() => Unit]] = new java.util.concurrent.LinkedBlockingQueue
  private var threadOpt: Option[Thread] = None

  def async(f: () => Unit): Unit = synchronized {
    threadOpt match {
      case Some(_) =>
      case _ =>
        val t = new Thread {
          override def run(): Unit = {
            var terminated = false
            while (!terminated) {
              queue.take() match {
                case Some(f) => f()
                case _ => terminated = true
              }
            }
          }
        }
        t.start()
        threadOpt = Some(t)
    }
    queue.add(Some(f))
  }

  def finalise(): Unit = {
    queue.add(None)
  }

  lazy val isLogikaSupportedPlatform: Boolean = org.sireum.Os.kind match {
    case org.sireum.Os.Kind.Unsupported => false
    case _ => true
  }
}
