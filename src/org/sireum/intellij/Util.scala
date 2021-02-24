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

import com.intellij.notification.{Notifications, Notification}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager}
import com.intellij.openapi.project.Project

object Util {
  def getFilePath(project: Project): Option[org.sireum.Os.Path] = {
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor == null) return None
    val fdm = FileDocumentManager.getInstance
    val file = fdm.getFile(editor.getDocument)
    if (file == null) return None
    Some(org.sireum.Os.path(file.getCanonicalPath))
  }

  def getFileExt(project: Project): String = {
    getFilePath(project) match {
      case Some(path) => path.ext.value
      case _ => ""
    }
  }

  def isSireumOrLogikaFile(project: Project): (Boolean, Boolean) =
    getFilePath(project) match {
      case Some(p) => isSireumOrLogikaFile(p)
      case _ => (false, false)
    }

  def isSireumOrLogikaFile(path: org.sireum.Os.Path): (Boolean, Boolean) = {
    if (!path.exists) return (false, false)
    val p = path.string.value
    if (p.endsWith(".slang")) {
      return (true, true)
    } else if (p.endsWith(".scala") || p.endsWith(".sc")) {
      for (line <- path.readLineStream.take(1)) {
        val cline = line.value.replace(" ", "").replace("\t", "")
        return (cline.contains("#Sireum"), cline.contains("#Logika"))
      }
    }
    return (false, false)
  }

  def notify(n: Notification, project: Project, shouldExpire: Boolean): Unit =
    if (shouldExpire)
      new Thread() {
        override def run(): Unit = {
          Notifications.Bus.notify(n, project)

          Thread.sleep(5000)
          ApplicationManager.getApplication.invokeLater(() => n.expire())
        }
      }.start()
    else
      Notifications.Bus.notify(n, project)
}
