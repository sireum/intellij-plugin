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

import com.intellij.notification.{Notification, Notifications}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

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

  def isProyek(project: Project): Boolean = {
    val root = org.sireum.Os.path(project.getBasePath)
    return (root / "bin" / "project.cmd").isFile
  }

  def recommendReload(iproject: Project): Boolean = {
    import org.sireum._
    if (!isProyek(iproject)) {
      return false
    }
    val root = Os.path(iproject.getBasePath)
    val sireumHome = SireumApplicationComponent.getSireumHome(iproject) match {
      case scala.Some(home) =>
        System.setProperty("org.sireum.home", home.string.value)
        home
      case _ => return false
    }
    val prjOpt = org.sireum.proyek.Proyek.getProject(sireumHome, root, None(), None())
    val errorF = root / ".idea" / "error.txt"
    errorF.removeAll()
    if (prjOpt.isEmpty) {
      errorF.writeOver("Could not load project\n")
      errorF.removeOnExit()
      return true
    }
    val prj = prjOpt.get
    val vsOpt = org.sireum.proyek.Proyek.getVersions(prj, root, ISZ(), SireumApi.versions.entries)
    if (vsOpt.isEmpty) {
      errorF.writeAppend("Could not load versions\n")
      errorF.removeOnExit()
      return true
    }
    val versions = vsOpt.get
    val projectJson = root / ".idea" / "project.json"
    val versionsJson = root / ".idea" / "versions.json"
    project.ProjectUtil.load(projectJson) match {
      case Some(prjCache) if prjCache == prj =>
      case _ =>
        if (projectJson.exists) {
          val backup = projectJson.up / "project.old.json"
          projectJson.moveTo(backup)
          backup.removeOnExit()
        }
        return true
    }
    proyek.Proyek.loadVersions(versionsJson) match {
      case Some(versionsCache) if versionsCache == versions =>
      case _ =>
        if (versionsJson.exists) {
          val backup = versionsJson.up / "versions.old.json"
          versionsJson.moveTo(backup)
          backup.removeOnExit()
        }
        return true
    }
    return false
  }

  def isSireumOrLogikaFile(path: org.sireum.Os.Path)(content: => org.sireum.String = path.read): (Boolean, Boolean) = {
    if (!path.exists) return (false, false)
    val p = path.string.value
    val (hasSireum, compactFirstLine, _) = org.sireum.lang.parser.SlangParser.detectSlang(org.sireum.Some(p), content)
    return (hasSireum, compactFirstLine.contains("#Logika"))
  }

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

  def isNotLinuxArm: Boolean = org.sireum.Os.kind match {
    case org.sireum.Os.Kind.LinuxArm => false
    case org.sireum.Os.Kind.Unsupported => false
    case _ => true
  }
}
