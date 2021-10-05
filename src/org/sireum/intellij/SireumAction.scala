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

import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

object SireumAction {
  val infoTitle: String = "Sireum Info"
  val warningTitle: String = "Sireum Warning"
  val errorTitle: String = "Sireum Error"
}

trait SireumAction extends AnAction

trait SireumOnlyAction extends SireumAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null &&
      Util.isSireumOrLogikaFile(project)(org.sireum.String(editor.getDocument.getText))._1)
  }
}

object SireumOnlyAction {
  def processSlangRewriteResponse(resp: org.sireum.server.protocol.Slang.Rewrite.Response,
                                  project: Project, editor: Editor): Unit = {
    WriteCommandAction.runWriteCommandAction(project,
      (() => {
        if (editor != null && !editor.isDisposed) {
          val (nt, title, msg) = resp.message.level match {
            case org.sireum.message.Level.Info =>
              resp.newTextOpt match {
                case org.sireum.Some(newText) if resp.numOfRewrites =!= 0 =>
                  editor.getDocument.setText(newText.value)
                case _ =>
              }
              (NotificationType.INFORMATION, SireumAction.infoTitle, resp.message.text.value)
            case org.sireum.message.Level.Error => (NotificationType.ERROR, SireumAction.errorTitle, resp.message.text.value)
            case org.sireum.message.Level.InternalError => (NotificationType.ERROR, SireumAction.errorTitle, resp.message.text.value)
            case org.sireum.message.Level.Warning => (NotificationType.WARNING, SireumAction.warningTitle, resp.message.text.value)
          }
          Util.notify(new Notification(SireumClient.groupId, title, msg, nt), project, shouldExpire = true)
        }
      }): Runnable)
  }
}

trait SlangRewriteAction extends SireumOnlyAction {

  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type

  final override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val fileUriOpt: org.sireum.Option[org.sireum.String] = Util.getFilePath(project) match {
      case Some(p) => org.sireum.Some(p.canon.string)
      case _ => org.sireum.None()
    }
    val document = editor.getDocument
    val text = document.getText
    val isWorksheet = fileUriOpt match {
      case org.sireum.Some(fileUri) => !fileUri.value.endsWith(".scala") && !fileUri.value.endsWith(".slang")
      case _ => true
    }
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
    SireumClient.addRequest(id =>
      Vector(org.sireum.server.protocol.Slang.Rewrite.Request(id, kind, isWorksheet, fileUriOpt, text)),
      project, file, editor, isBackground = false, text
    )
  }
}

class SlangInsertConstructorValsAction extends SlangRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.InsertConstructorVals
}

class SlangRenumberProofStepsAction extends SlangRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.RenumberProofSteps
}

class SlangReplaceEnumSymbolsAction extends SlangRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.ReplaceEnumSymbols
}


trait SireumInsertSymbol extends SireumAction {
  def symbol: String

  final override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor == null) return
    val document = editor.getDocument
    WriteCommandAction.runWriteCommandAction(project,
      (() => {
        val caret = editor.getCaretModel.getPrimaryCaret
        val offset = caret.getOffset
        document.insertString(offset, symbol)
        caret.moveToOffset(offset + 1)
      }): Runnable)
  }
}

final class SireumInsertForAll extends SireumInsertSymbol {
  val symbol: String = "∀"
}

final class SireumInsertExists extends SireumInsertSymbol {
  val symbol: String = "∃"
}

final class SireumInsertSequent extends SireumInsertSymbol {
  val symbol: String = "⊢"
}