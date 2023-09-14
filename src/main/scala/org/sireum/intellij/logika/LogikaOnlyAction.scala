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

package org.sireum.intellij.logika

import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager}
import com.intellij.openapi.vfs.VirtualFile
import org.sireum.intellij.{SireumAction, SireumApplicationComponent, SireumClient, Util}
import org.sireum.logika.options.OptionsUtil

trait LogikaOnlyAction extends SireumAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null && Util.isLogikaSupportedPlatform &&
      Util.isSireumOrLogikaFile(project)(org.sireum.String(editor.getDocument.getText))._2)
  }
}

trait LogikaInsertOptionsAction extends SireumAction {
  def updateText(editor: Editor, options: String): Unit
  def isInterprocedural: Boolean
  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(false)
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor == null) return
    SireumApplicationComponent.getSireumHome(project) match {
      case Some(sireumHome) =>
        val nameExePathMap = org.sireum.logika.Smt2Invoke.nameExePathMap(sireumHome)
        val isScript = editor.getVirtualFile.getExtension == "sc"
        val options = OptionsUtil.fromConfig(isScript, org.sireum.Os.numOfProcessors,
          nameExePathMap, SireumClient.getLogikaConfig(project, isBackground = false, isScript = isScript,
            isInterprocedural = isInterprocedural))
        updateText(editor, options.value)
      case _ =>
    }
  }
}

trait LogikaInsertOptionsActionFileCommon extends LogikaInsertOptionsAction {
  def updateText(editor: Editor, options: String): Unit = ApplicationManager.getApplication.invokeLater { () =>
    if (editor == null) return
    val text = editor.getDocument.getText
    val i = text.indexOf('\n')
    if (i < 0) return
    editor.getDocument.setText(s"${text.substring(0, i + 1)}//@${OptionsUtil.logika}: $options\n${text.substring(i + 1, text.length)}")
  }
}

final class LogikaInsertOptionsActionFile extends LogikaInsertOptionsActionFileCommon {
  def isInterprocedural: Boolean = false
}

final class LogikaInsertOptionsActionFileInterp extends LogikaInsertOptionsActionFileCommon {
  def isInterprocedural: Boolean = true
}

trait LogikaInsertOptionsActionLineCommon extends LogikaInsertOptionsAction {
  def updateText(editor: Editor, options: String): Unit = ApplicationManager.getApplication.invokeLater { () =>
    if (editor == null) return
    val text = editor.getDocument.getText
    val line = SireumClient.getCurrentLine(editor)
    val lineOffset = SireumClient.getLineOffset(editor, line)
    var i = lineOffset
    while (text(i).isWhitespace) i += 1
    val indent = (for (_ <- 0 until i - lineOffset) yield ' ').mkString
    val tqs = "\"\"\""
    val stmt = s"""${org.sireum.LibUtil.setOptions}("${OptionsUtil.logika}", $tqs$options$tqs)"""
    editor.getDocument.setText(s"${text.substring(0, lineOffset)}$indent$stmt\n${text.substring(lineOffset, text.length)}")
  }
}

final class LogikaInsertOptionsActionLine extends LogikaInsertOptionsActionLineCommon {
  def isInterprocedural: Boolean = false
}

final class LogikaInsertOptionsActionLineInterp extends LogikaInsertOptionsActionLineCommon {
  def isInterprocedural: Boolean = true
}

trait LogikaCheckAction extends LogikaOnlyAction {

  // init
  {
    getTemplatePresentation.setIcon(SireumClient.icon)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(false)
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
    if (editor == null) return
    SireumClient.enableEditor(project, file, editor)
    SireumClient.analyze(project, file, editor, getLine(editor), isBackground = false, isInterprocedural = isInterprocedural)
    e.getPresentation.setEnabled(true)
  }

  def getLine(editor: Editor): Int

  def isInterprocedural: Boolean
}

final class LogikaCheckActionFile extends LogikaCheckAction {
  def getLine(editor: Editor): Int = 0
  def isInterprocedural: Boolean = false
}

final class LogikaCheckActionLine extends LogikaCheckAction {
  def getLine(editor: Editor): Int = SireumClient.getCurrentLine(editor)
  def isInterprocedural: Boolean = false

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null && Util.isLogikaSupportedPlatform &&
      Util.isSireumOrLogikaFile(project)(org.sireum.String(editor.getDocument.getText)) == (true, true))
  }
}

final class LogikaCheckActionInterprocedural extends LogikaCheckAction {
  def getLine(editor: Editor): Int = SireumClient.getCurrentLine(editor)
  def isInterprocedural: Boolean = true

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null && Util.isLogikaSupportedPlatform &&
      Util.isSireumOrLogikaFile(project)(org.sireum.String(editor.getDocument.getText))._1)
  }
}

final class LogikaSmt2Action extends LogikaOnlyAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) {
      val text = editor.getDocument.getText
      e.getPresentation.setEnabledAndVisible(project != null &&
        editor.getVirtualFile.getExtension == "smt2" &&
        (text.contains(SireumClient.smt2SolverPrefix) && text.contains(SireumClient.smt2SolverArgsPrefix) ||
          text.contains(SireumClient.smt2SolverAndArgsPrefix)))
    }
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = FileEditorManager.getInstance(e.getProject).getSelectedTextEditor
    ApplicationManager.getApplication.invokeAndWait(() =>
      if (editor != null) FileDocumentManager.getInstance().saveDocument(editor.getDocument))
    SireumClient.launchSMT2Solver(e.getProject, editor)
  }
}