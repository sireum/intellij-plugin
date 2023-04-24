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

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.{ActionManager, AnAction}
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.JBColor
import com.intellij.ui.content.ContentFactory
import org.sireum.intellij.logika.LogikaToolWindowForm

import java.awt.Color
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.DefaultHighlighter

object SireumToolWindowFactory {

  final case class Forms(toolWindow: ToolWindow, logika: LogikaToolWindowForm, consoleView: ConsoleView)

  val windows = new ConcurrentHashMap[Project, Forms]()
  val hpainter = new DefaultHighlighter.DefaultHighlightPainter(
    new JBColor(new Color(129, 62, 200, 64), new Color(129, 62, 200, 256 - 64)))

  def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    toolWindow.setAutoHide(false)
    toolWindow.setIcon(SireumClient.sireumGrayIcon)
    toolWindow match {
      case toolWindow: ToolWindowEx =>
        val list = new java.util.ArrayList[AnAction]
        list.add(ActionManager.getInstance().getAction("ProyekSyncAction"))
        toolWindow.setTitleActions(list)
      case _ =>
    }
    val contentFactory = ContentFactory.SERVICE.getInstance
    val logikaForm = new LogikaToolWindowForm()
    toolWindow.getContentManager.addContent(
      contentFactory.createContent(logikaForm.logikaToolWindowPanel, "Output", false))
    val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole
    console.requestScrollingToEnd()
    toolWindow.getContentManager.addContent(
      contentFactory.createContent(console.getComponent, "Console", false))
    logikaForm.logikaTextArea.setEditable(false)

    logikaForm.logikaToolTextExportButton.addActionListener((_: ActionEvent) => {
      var text = logikaForm.logikaTextArea.getText

      val ext = text.headOption match {
        case Some(';') =>
          def updateIfInvalid(): Unit = {
            val resultPrefix = "; Result"
            var i = text.indexOf(resultPrefix)
            if (i < 0) return
            i = text.indexOf(':', i + resultPrefix.length)
            if (i < 0) return
            val j = text.indexOf('\n', i + 1)
            if (j < 0) return
            if ("Invalid" != text.substring(i + 1, j).trim) return
            val checkSat = "(check-sat)"
            i = text.lastIndexOf(checkSat)
            if (i < 0) return
            val k = i + checkSat.length
            text = s"${text.substring(0, k)}\n(get-model)${text.substring(k, text.length)}"
          }
          updateIfInvalid()
          ".smt2"
        case _ => ".txt"
      }
      import org.sireum.Os._
      val f = tempFix("logika-", ext)
      f.writeOver(text)
      TransactionGuard.submitTransaction(project, {
        () =>
          val editor = FileEditorManager.getInstance(project).openTextEditor(
            new OpenFileDescriptor(project, LocalFileSystem.getInstance().findFileByPath(f.canon.string.value)), true)

          SireumClient.launchSMT2Solver(project, editor)
      }: Runnable)
    })

    logikaForm.logikaToolTextField.setPlaceholderColor(new JBColor(Color.darkGray, Color.gray))
    logikaForm.logikaToolTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(documentEvent: DocumentEvent): Unit = update(documentEvent)
      override def removeUpdate(documentEvent: DocumentEvent): Unit = update(documentEvent)
      override def changedUpdate(documentEvent: DocumentEvent): Unit = update(documentEvent)

      def update(documentEvent: DocumentEvent): Unit = {
        val document = documentEvent.getDocument
        val text = document.getProperty("Logika").asInstanceOf[String]
        if (text == null) {
          return
        }
        val content = document.getText(0, documentEvent.getDocument.getLength)
        SireumClient.singleExecutor.schedule({ () =>
          if (content == document.getText(0, documentEvent.getDocument.getLength)) {
            text(0) match {
              case '{' =>
                if (content.isEmpty) {
                  logikaForm.logikaTextArea.setText(text)
                  logikaForm.logikaTextArea.getHighlighter.removeAllHighlights()
                } else {
                  val lines = text.split('\n')
                  val size = lines.length
                  var newLines = List[String]()
                  var i = 1
                  while (i < size) {
                    var j = i
                    var found = false
                    while (j < size && !found) {
                      if (lines(j).last == ';') {
                        found = true
                        var hasContent = false
                        for (k <- i to j if lines(k).contains(content)) hasContent = true
                        if (hasContent) {
                          for (k <- i to j) newLines = newLines :+ lines(k)
                        }
                      } else if (lines(j).head == '}') {
                        found = true
                        var hasContent = false
                        for (k <- i until j if lines(k).contains(content)) hasContent = true
                        if (hasContent) for (k <- i until j) newLines = newLines :+ lines(k)
                      }
                      j = j + 1
                    }
                    i = j
                  }
                  val newText = s"// Filtered by: $content\n{\n${newLines.mkString("\n")}\n}"
                  logikaForm.logikaTextArea.setText(newText)
                  val highlighter = logikaForm.logikaTextArea.getHighlighter
                  highlighter.removeAllHighlights()
                  i = newText.indexOf("{") + 1
                  var offset = newText.indexOf(content, i)
                  while (offset > 0) {
                    i = offset + content.length
                    highlighter.addHighlight(offset, i, hpainter)
                    offset = newText.indexOf(content, i)
                  }
                }
              case _ =>
                val highlighter = logikaForm.logikaTextArea.getHighlighter
                highlighter.removeAllHighlights()
                var i = 0
                var offset = text.indexOf(content, i)
                while (offset > 0) {
                  i = offset + content.length
                  highlighter.addHighlight(offset, i, hpainter)
                  offset = text.indexOf(content, i)
                }
            }
          }
        }: Runnable, 100, TimeUnit.MILLISECONDS)
      }
    })

    windows.put(project, Forms(toolWindow, logikaForm, console))
  }

  def removeToolWindow(project: Project): Unit = {
    windows.remove(project)
  }
}
