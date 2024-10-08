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

import com.intellij.notification.{Notification, NotificationListener, NotificationType}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener, TextEditor}
import com.intellij.openapi.progress.impl.CoreProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.{ToolWindowAnchor, ToolWindowManager, WindowManager}

import javax.swing.event.HyperlinkEvent

class SireumProjectComponent(iproject: Project) extends ProjectComponent {
  override def projectClosed(): Unit = {
    SireumToolWindowFactory.removeToolWindow(iproject)
  }

  override def projectOpened(): Unit = {

    ToolWindowManager.getInstance(iproject).invokeLater(() => {
      val tw = ToolWindowManager.getInstance(iproject).
        registerToolWindow("Sireum", false, ToolWindowAnchor.RIGHT)
      SireumToolWindowFactory.createToolWindowContent(iproject, tw)

      iproject.getMessageBus.connect(iproject).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerListener {
          override def fileClosed(source: FileEditorManager,
                                  file: VirtualFile): Unit = {
            SireumClient.editorClosed(iproject)
          }

          override def fileOpened(source: FileEditorManager,
                                  file: VirtualFile): Unit =
            ApplicationManager.getApplication.invokeLater { () =>
              try {
                Util.getPath(file)
                for (fileEditor <- source.getAllEditors) {
                  fileEditor match {
                    case fileEditor: TextEditor if fileEditor.getFile == file =>
                      SireumClient.editorOpened(iproject, file, fileEditor.getEditor)
                    case _ =>
                  }
                }
              } catch {
                case _: Throwable =>
              }
            }

          override def selectionChanged(event: FileEditorManagerEvent): Unit = {}
        })
    })

    new Thread(() => {
      Thread.sleep(5000)

      if (SireumApplicationComponent.startup) {
        SireumClient.init(iproject)
      }

      while (!CoreProgressManager.getCurrentIndicators.isEmpty) {
        Thread.sleep(1000)
      }

      if (Util.recommendReload(iproject)) {
        Util.notify(Util.notification(SireumClient.groupId, "Proyek reload?",
          """<p>Project definition and/or version dependencies have changed. <a href="">Reload</a>?</p>""",
          NotificationType.INFORMATION, (_: Notification, _: HyperlinkEvent) => {
            ProyekSyncAction.sync(iproject)
          }), iproject, scala.None)
      }
    }).start()
  }

  override def initComponent(): Unit = {}

  override def disposeComponent(): Unit = {}

  override def getComponentName: String = "Sireum Project"
}
