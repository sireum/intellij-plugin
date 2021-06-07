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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager

trait LogikaInsertSymbol extends LogikaAction {
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

final class LogikaInsertForAll extends LogikaInsertSymbol {
  val symbol: String = "∀"
}

final class LogikaInsertExists extends LogikaInsertSymbol {
  val symbol: String = "∃"
}

final class LogikaInsertSequent extends LogikaInsertSymbol {
  val symbol: String = "⊢"
}