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

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.{ToolWindowAnchor, ToolWindowManager}
import org.sireum.intellij.lang.Slang
import org.sireum.intellij.logika.action.LogikaCheckAction

class SireumProjectComponent(project: Project) extends ProjectComponent {
  override def projectClosed(): Unit = {
    SireumToolWindowFactory.removeToolWindow(project)
  }

  override def projectOpened(): Unit = {
    val tw = ToolWindowManager.getInstance(project).
      registerToolWindow("Sireum", false, ToolWindowAnchor.RIGHT)
    SireumToolWindowFactory.createToolWindowContent(project, tw)

    project.getMessageBus.connect(project).
      subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerListener {
          override def fileClosed(source: FileEditorManager,
                                  file: VirtualFile): Unit = {
            LogikaCheckAction.editorClosed(project)
            Slang.editorClosed(project, file)
          }

          override def fileOpened(source: FileEditorManager,
                                  file: VirtualFile): Unit = {
            val editor = source.getSelectedTextEditor
            LogikaCheckAction.editorOpened(project, file, editor)
            Slang.editorOpened(project, file, editor)
          }

          override def
          selectionChanged(event: FileEditorManagerEvent): Unit = {}
        })
  }

  override def initComponent(): Unit = {}

  override def disposeComponent(): Unit = {}

  override def getComponentName: String = "Sireum Project"
}
