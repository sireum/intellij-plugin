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
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.actionSystem.{ActionManager, AnAction}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.JBColor
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.tree.AbstractTreeModel
import org.antlr.v4.runtime.{BaseErrorListener, CharStreams, CommonTokenStream, RecognitionException, Recognizer}
import org.sireum.intellij.logika.LogikaToolWindowForm
import org.sireum.smtlib.parser.{SMTLIBv2Lexer, SMTLIBv2Parser}

import java.awt.{Color, Component}
import java.awt.event.{ActionEvent, ComponentAdapter, ComponentEvent}
import java.io.PrintWriter
import javax.swing.{DefaultListCellRenderer, DefaultListModel, JList, JTextArea, JTree}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.DefaultHighlighter

object SireumToolWindowFactory {

  final case class Problem(project: Project, value: org.sireum.server.protocol.Report) {
    override def toString: String = {
      import org.sireum._
      val level = value.message.level match {
        case message.Level.Error => "Error"
        case message.Level.InternalError => "Internal Error"
        case message.Level.Warning => "Warning"
        case message.Level.Info => "Info"
      }
      value.posOpt match {
        case Some(pos) =>
          pos.uriOpt match {
            case Some(uri) =>
              s"[${Os.Path.fromUri(uri).name}, ${pos.beginLine}, ${pos.beginColumn}] $level: ${value.message.text}"
            case _ =>
              s"[${pos.beginLine}, ${pos.beginColumn}] $level: ${value.message.text}"
          }
        case _ =>
          s"$level: ${value.message.text}"
      }
    }
  }

  final class ListCellRenderer extends DefaultListCellRenderer {
    val ta = new JTextArea
    val border = com.intellij.util.ui.JBUI.Borders.customLineBottom(new JBColor(Color.lightGray, Color.darkGray))
    override def getListCellRendererComponent(list: JList[_ <: AnyRef], value: scala.Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component = {
      ta.setBorder(border)
      if (isSelected) {
        ta.setBackground(selectedListItemColor)
      } else {
        ta.setBackground(list.getBackground)
      }
      ta.setForeground(list.getForeground)
      ta.setText(value.toString)
      val width = list.getWidth
      if (width > 0)
        ta.setSize(width, Short.MaxValue)
      ta
    }
  }

  final class ListComponentAdapter(list: JList[_]) extends ComponentAdapter {
    override def componentResized(e: ComponentEvent): Unit = {
      list.setFixedCellHeight(10)
      list.setFixedCellHeight(-1)
    }
  }

  final case class Forms(toolWindow: ToolWindow,
                         problemList: JList[Problem],
                         logika: LogikaToolWindowForm,
                         consoleView: ConsoleView,
                         astTree: JTree)

  object SlangAstTreeModel {
    final class Node(val text: String, val isLeaf: Boolean, val value: Object) {
      override def toString: String = {
        val className = value.getClass.getName.replace('$', '.')
        if (isLeaf) s"$text = $value ($className)" else s"$text ($className)"
      }
    }
  }
  final class SlangAstTreeModel(val project: Project, root: Object) extends AbstractTreeModel {
    import org.sireum._

    override def getRoot: AnyRef = root

    override def getChild(parent: Any, index: Int): AnyRef = parent match {
      case Some(x) => new SlangAstTreeModel.Node("value", isLeaf(x), x.asInstanceOf[Object])
      case parent: IS[_, _] =>
        val value = parent.atZ(index).asInstanceOf[Object]
        new SlangAstTreeModel.Node(index.toString, isLeaf(value), value)
      case parent: List[_] =>
        val value = parent(index).asInstanceOf[Object]
        new SlangAstTreeModel.Node(index.toString, isLeaf(value), value)
      case parent: DatatypeSig =>
        val (name, value) = parent.$content(index + 1)
        val isLeaf = this.isLeaf(value)
        new SlangAstTreeModel.Node(name, isLeaf, value.asInstanceOf[Object])
      case parent: SlangAstTreeModel.Node => getChild(parent.value, index)
    }

    override def getChildCount(parent: Any): Int = parent match {
      case _: None[_] => 0
      case _: Some[_] => 1
      case parent: IS[_, _] => parent.size.toInt
      case parent: List[_] =>
        parent.length
      case parent: DatatypeSig =>
        parent.$content.length - 1
      case parent: SlangAstTreeModel.Node => getChildCount(parent.value)
      case _ => 0
    }

    override def isLeaf(node: Any): Boolean = getChildCount(node) == 0

    override def getIndexOfChild(parent: Any, child: Any): Int = ???
  }

  val windows = new ConcurrentHashMap[Project, Forms]()
  val hpainterColor = new Color(129, 62, 200, 64)
  val hpainterDarkColor = new Color(129, 62, 200, 256 - 64)
  val hpainter = new DefaultHighlighter.DefaultHighlightPainter(new JBColor(hpainterColor, hpainterDarkColor))
  val selectedListItemColor = new JBColor(
    hpainterColor.brighter().brighter().brighter(),
    hpainterDarkColor.darker().darker().darker())

  def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = try {
    toolWindow.setAutoHide(false)
    toolWindow.setIcon(SireumClient.sireumGrayIcon)
    toolWindow match {
      case toolWindow: ToolWindowEx =>
        val list = new java.util.ArrayList[AnAction]
        list.add(ActionManager.getInstance().getAction("ProyekSyncAction"))
        toolWindow.setTitleActions(list)
      case _ =>
    }
    val contentFactory = ContentFactory.getInstance

    val problemForm = new ProblemToolWindowForm()
    toolWindow.getContentManager.addContent(
      contentFactory.createContent(problemForm.problemPanel, "Problems", false))
    problemForm.problemList.setCellRenderer(new ListCellRenderer())
    problemForm.problemList.addComponentListener(new ListComponentAdapter(problemForm.problemList))
    problemForm.problemList.setModel(new DefaultListModel[Problem]())
    problemForm.problemList.addListSelectionListener(e => {
      val problem = problemForm.problemList.getModel.getElementAt(e.getFirstIndex)
      problem.value.posOpt match {
        case org.sireum.Some(pos) =>
          pos.uriOpt match {
            case org.sireum.Some(uri) =>
              val file = LocalFileSystem.getInstance().findFileByPath(org.sireum.Os.Path.fromUri(uri).string.value)
              FileEditorManager.getInstance(project).openTextEditor(
                new OpenFileDescriptor(problem.project, file, pos.offset.toInt), true)
            case _ =>
          }
        case _ =>
      }
    })

    val logikaForm = new LogikaToolWindowForm()
    toolWindow.getContentManager.addContent(
      contentFactory.createContent(logikaForm.logikaToolWindowPanel, "Output", false))
    logikaForm.logikaTextArea.setEditable(false)
    logikaForm.logikaList.setCellRenderer(new ListCellRenderer())
    logikaForm.logikaList.addComponentListener(new ListComponentAdapter(logikaForm.logikaList))

    val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole
    console.requestScrollingToEnd()
    toolWindow.getContentManager.addContent(
      contentFactory.createContent(console.getComponent, "Console", false))

    val slangAstForm = new SlangAstToolWindowForm()
    slangAstForm.slangAstTree.setModel(new SlangAstTreeModel(project, null))
    slangAstForm.slangAstTree.addTreeSelectionListener(_ => {
      import org.sireum._
      slangAstForm.slangAstTree.getLastSelectedPathComponent match {
        case node: SlangAstTreeModel.Node =>
          node.value match {
            case o: DatatypeSig =>
              def openPosOpt(posOpt: Option[message.Position]): Unit = {
                posOpt match {
                  case Some(pos) =>
                    val project = slangAstForm.slangAstTree.getModel.asInstanceOf[SlangAstTreeModel].project
                    val file = LocalFileSystem.getInstance.findFileByPath(org.sireum.Os.uriToPath(pos.uriOpt.get).string.value)
                    FileEditorManager.getInstance(project).openTextEditor(
                      new OpenFileDescriptor(project, file, pos.offset.toInt), true)
                  case _ =>
                }
              }
              for ((_, attr) <- o.$content) {
                attr match {
                  case attr: lang.ast.Attr => openPosOpt(attr.posOpt)
                  case attr: lang.ast.TypedAttr => openPosOpt(attr.posOpt)
                  case attr: lang.ast.ResolvedAttr => openPosOpt(attr.posOpt)
                  case _ =>
                }
              }
            case _ =>
          }
        case _ =>
      }
    })
    toolWindow.getContentManager.addContent(
      contentFactory.createContent(slangAstForm.slangAstPanel, "Slang AST", false))

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
      ApplicationManager.getApplication.invokeLaterOnWriteThread({
        () =>
          val editor = FileEditorManager.getInstance(project).openTextEditor(
            new OpenFileDescriptor(project, LocalFileSystem.getInstance().findFileByPath(f.canon.string.value)), true)

          SireumClient.launchSMT2Solver(project, editor)
      })
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
        val kind = document.getProperty("Logika Kind").asInstanceOf[String]
        val content = document.getText(0, documentEvent.getDocument.getLength)
        SireumClient.singleExecutor.schedule({ () =>
          if (content == document.getText(0, documentEvent.getDocument.getLength)) {
            if (content.isEmpty) {
              logikaForm.logikaTextArea.setText(text)
              logikaForm.logikaTextArea.getHighlighter.removeAllHighlights()
            } else {
              kind match {
                case "claims " =>
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
                case "trace " =>
                  val lines = text.lines.toArray
                  var chunks = Vector[String]()
                  var i = 0
                  while (i < lines.length) {
                    val line = lines(i).toString
                    if (line.startsWith("by [") || line.startsWith("∴") || line.startsWith("Begin") || line.startsWith("info [")) {
                      var j = i + 1
                      var l = lines(j).toString
                      var ls = Vector(line)
                      while (l.nonEmpty) {
                        ls = ls :+ l
                        j = j + 1
                        l = if (j < lines.length) lines(j).toString else ""
                      }
                      ls = ls :+ ""
                      val chunk = ls.mkString("\n")
                      if (chunk.contains(content)) {
                        chunks = chunks :+ chunk
                      }
                    }
                    i = i + 1
                  }
                  val newText = s"// Filtered by: $content\n\n${chunks.mkString("\n")}"
                  logikaForm.logikaTextArea.setText(newText)
                  val highlighter = logikaForm.logikaTextArea.getHighlighter
                  highlighter.removeAllHighlights()
                  i = 0
                  var offset = newText.indexOf(content, i)
                  while (offset > 0) {
                    i = offset + content.length
                    highlighter.addHighlight(offset, i, hpainter)
                    offset = newText.indexOf(content, i)
                  }
                case _ =>
                  val cs = CharStreams.fromString(text)
                  val lexer = new SMTLIBv2Lexer(cs)
                  val cts = new CommonTokenStream(lexer)
                  val parser = new SMTLIBv2Parser(cts)
                  parser.removeErrorListeners()
                  parser.addErrorListener(new BaseErrorListener() {
                    override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException): Unit = {
                      throw new RuntimeException()
                    }
                  })
                  var newText = text
                  try {
                    val script = parser.start().script()
                    var chunks = Vector[String]()
                    for (i <- 0 until script.getChildCount) {
                      val command = script.command(i)
                      val commandText = newText.substring(command.start.getStartIndex, command.stop.getStopIndex + 1)
                      if (commandText.contains(content)) {
                        chunks = chunks :+ commandText
                      }
                    }
                    newText = s"// Filtered by: $content\n\n${chunks.mkString("\n")}"
                  } catch {
                    case _: RuntimeException =>
                      newText = s"// Could not filter by (only highlight): $content\n\n$newText}"
                  }
                  logikaForm.logikaTextArea.setText(newText)
                  val highlighter = logikaForm.logikaTextArea.getHighlighter
                  highlighter.removeAllHighlights()
                  var i = 0
                  var offset = newText.indexOf(content, i)
                  while (offset > 0) {
                    i = offset + content.length
                    highlighter.addHighlight(offset, i, hpainter)
                    offset = newText.indexOf(content, i)
                  }
              }
            }
          }
        }: Runnable, 100, TimeUnit.MILLISECONDS)
      }
    })

    windows.put(project, Forms(toolWindow, problemForm.problemList, logikaForm, console, slangAstForm.slangAstTree))
  } catch {
    case t: Throwable =>
      val sw = new java.io.StringWriter
      val pw = new PrintWriter(sw)
      t.printStackTrace(pw)
      Util.notify(new Notification(
        SireumClient.groupId, s"Could not create Sireum toolwindow\n${sw.toString}",
        NotificationType.ERROR), project, shouldExpire = true)
  }

  def removeToolWindow(project: Project): Unit = {
    windows.remove(project)
  }
}
