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
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.{ApplicationManager, TransactionGuard}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.{EditorFontType, TextAttributesKey}
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.markup._
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util._
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget.{IconPresentation, WidgetPresentation}
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.openapi.wm.{StatusBar, StatusBarWidget, WindowManager}
import com.intellij.util.Consumer
import org.sireum.intellij.logika.LogikaConfigurable
import org.sireum.message.Level

import java.awt.event.MouseEvent
import java.awt.{Color, Font}
import java.util.concurrent._
import javax.swing.{DefaultListModel, Icon, JSplitPane}

object SireumClient {

  object EditorEnabled

  val groupId: Predef.String = "Sireum"
  val icons: Seq[Icon] = {
    var r = (0 to 6).map(n => IconLoader.getIcon(s"/icon/sireum-$n.png"))
    r ++= r.drop(1).reverse.drop(1)
    r
  }
  val icon: Icon = icons.head
  val gutterErrorIcon: Icon = IconLoader.getIcon("/icon/gutter-error.png")
  val gutterWarningIcon: Icon = IconLoader.getIcon("/icon/gutter-warning.png")
  val gutterInfoIcon: Icon = IconLoader.getIcon("/icon/gutter-info.png")
  val gutterHintIcon: Icon = IconLoader.getIcon("/icon/gutter-hint.png")
  val gutterSummoningIcon: Icon = IconLoader.getIcon("/icon/gutter-summoning.png")
  val verifiedInfoIcon: Icon = IconLoader.getIcon("/icon/logika-verified-info.png")
  val queue = new LinkedBlockingQueue[Vector[String]]()
  val editorMap: scala.collection.mutable.Map[org.sireum.ISZ[org.sireum.String], (Project, VirtualFile, Editor, String)] = scala.collection.mutable.Map()
  val sireumKey = new Key[EditorEnabled.type]("Sireum")
  val analysisDataKey = new Key[(Map[Int, Vector[RangeHighlighter]], DefaultListModel[Object], Map[Int, DefaultListModel[SummoningReportItem]], Map[Int, DefaultListModel[HintReportItem]])]("Logika Analysis Data")
  val statusKey = new Key[Boolean]("Sireum Analysis Status")
  val reportItemKey = new Key[ReportItem]("Sireum Report Item")
  var request: Option[Request] = None
  var processInit: Option[scala.sys.process.Process] = None
  var terminated = false
  var dividerWeight: Double = .2
  var tooltipMessageOpt: Option[String] = None
  var tooltipBalloonOpt: Option[Balloon] = None
  val tooltipDefaultBgColor: Color = new Color(0xff, 0xff, 0xcc)
  val tooltipDarculaBgColor: Color = new Color(0x5c, 0x5c, 0x42)
  val maxTooltipLength: Int = 1024

  final case class Request(time: Long, requestId: org.sireum.ISZ[org.sireum.String],
                           project: Project, file: VirtualFile, editor: Editor,
                           input: String, msgGen: () => Vector[String])

  def init(p: Project): Unit = editorMap.synchronized {
    if (processInit.isEmpty) {
      processInit =
        SireumApplicationComponent.getSireumProcess(p,
          queue, { s =>
            val trimmed = s.trim

            def err(): Unit = {
              val msg = s"Invalid server message: $trimmed"
              notifyHelper(scala.Some(p), scala.None,
                org.sireum.server.protocol.Report(org.sireum.ISZ(),
                  org.sireum.message.Message(org.sireum.message.Level.InternalError, org.sireum.None(),
                    "client", msg))
              )
            }

            if (trimmed.startsWith("""{  "type" : """)) {
              try {
                org.sireum.server.protocol.JSON.toResponse(trimmed) match {
                  case org.sireum.Either.Left(r) => processResult(r)
                  case org.sireum.Either.Right(_) => err()
                }
              } catch {
                case _: Throwable => err()
              }
            } else {
              //              val msg = s"Unrecognized server message: $trimmed"
              //              System.out.println(msg)
              //              System.out.flush()
            }
          }, "x", "server", "-m", "json")
      if (processInit.isEmpty) return
      val statusBar = WindowManager.getInstance().getStatusBar(p)
      var frame = 0
      val statusIdle = "Sireum is idle"
      val statusWaiting = "Sireum is waiting to work"
      val statusWorking = "Sireum is working"
      var statusTooltip = statusIdle
      var shutdown = false
      lazy val statusBarWidget: StatusBarWidget = new StatusBarWidget {

        override def ID(): String = "Sireum"

        override def install(statusBar: StatusBar): Unit = {}

        override def getPresentation: WidgetPresentation =
          new IconPresentation {
            override def getClickConsumer: Consumer[MouseEvent] = _ =>
              if (Messages.showYesNoDialog(
                p, "Shutdown Sireum background server?",
                "Sireum", null) == Messages.YES)
                editorMap.synchronized {
                  this.synchronized {
                    request = None
                    editorMap.clear()
                    processInit.foreach(_.destroy())
                    processInit = None
                    shutdown = true
                    statusBar.removeWidget(statusBarWidget.ID())
                  }
                }

            override def getTooltipText: String =
              statusTooltip + " (click to shutdown)."

            override def getIcon: Icon = icons(frame)
          }

        override def dispose(): Unit = {}
      }
      statusBar.addWidget(statusBarWidget)
      statusBar.updateWidget(statusBarWidget.ID())
      val t = new Thread {
        override def run(): Unit = {
          val defaultFrame = icons.length / 2 + 1
          var idle = false
          while (!terminated && !shutdown) {
            if (editorMap.nonEmpty || request.nonEmpty) {
              idle = false
              frame = (frame + 1) % icons.length
              statusTooltip =
                if (editorMap.nonEmpty) statusWorking
                else statusWaiting
              statusBar.updateWidget(statusBarWidget.ID())
            } else {
              if (!idle) {
                idle = true
                val f = frame
                frame = defaultFrame
                statusTooltip = statusIdle
                if (f != defaultFrame)
                  statusBar.updateWidget(statusBarWidget.ID())
              }
            }
            this.synchronized {
              request match {
                case Some(r: Request) =>
                  if (System.currentTimeMillis - r.time > SireumApplicationComponent.idle) {
                    request = None
                    editorMap.synchronized {
                      editorMap(r.requestId) = (r.project, r.file, r.editor, r.input)
                    }
                    queue.add(r.msgGen())
                  }
                case None =>
              }
            }
            Thread.sleep(175)
          }
          ApplicationManager.getApplication.invokeLater(
            () => statusBar.removeWidget(statusBarWidget.ID()))
        }
      }
      t.setDaemon(true)
      t.start()
    }
  }

  def isEnabled(editor: Editor): Boolean = EditorEnabled == editor.getUserData(sireumKey)

  def analyze(project: Project, file: VirtualFile, editor: Editor, isBackground: Boolean, hasLogika: Boolean, line: Int): Unit = {
    if (editor.isDisposed || !isEnabled(editor)) return
    init(project)
    val input = editor.getDocument.getText
    val (t, requestId) = {
      val t = System.currentTimeMillis
      val id = org.sireum.ISZ(org.sireum.String(t.toString))
      (t, id)
    }

    def f(): Vector[String] = {
      import org.sireum.server.protocol._
      Vector(
        org.sireum.server.protocol.JSON.fromRequest(Logika.Verify.Config(
          LogikaConfigurable.hint, LogikaConfigurable.inscribeSummonings,
          org.sireum.server.service.LogikaService.defaultConfig(
            sat = LogikaConfigurable.checkSat,
            defaultLoopBound = LogikaConfigurable.loopBound,
            timeoutInMs = LogikaConfigurable.timeout
          )
        ), true).value,
        org.sireum.server.protocol.JSON.fromRequest(Slang.CheckScript(
          isBackground = isBackground,
          logikaEnabled = !isBackground ||
            (SireumApplicationComponent.backgroundAnalysis && LogikaConfigurable.backgroundAnalysis),
          id = requestId,
          uriOpt = org.sireum.Some(org.sireum.String(file.toNioPath.toUri.toASCIIString)),
          content = input,
          line = line
        ), true).value
      )
    }

    editorMap.synchronized {
      val cancels = for (rid <- editorMap.keys.toVector) yield {
        editorMap -= rid
        org.sireum.server.protocol.JSON.fromRequest(org.sireum.server.protocol.Cancel(rid), true).value
      }
      if (cancels.nonEmpty) queue.add(cancels)
    }


    if (isBackground) {
      this.synchronized {
        request = Some(Request(t, requestId, project, file, editor, input, f _))
      }
    } else {
      editorMap.synchronized {
        this.synchronized {
          request match {
            case Some(r: Request) =>
              editorMap -= r.requestId
            case _ =>
          }
          request = None
        }
        editorMap(requestId) = (project, file, editor, input)
      }
      queue.add(f())
    }
  }

  def analyzeOpt(project: Project, file: VirtualFile, editor: Editor, line: Int, isBackground: Boolean): Unit = {
    val (isSireum, isLogika) = Util.isSireumOrLogikaFile(project)
    if (isLogika) {
      if (SireumApplicationComponent.backgroundAnalysis)
        scala.util.Try(analyze(project, file, editor, isBackground = isBackground,
          hasLogika = LogikaConfigurable.backgroundAnalysis, line))
    } else if (isSireum) {
      if (SireumApplicationComponent.backgroundAnalysis)
        scala.util.Try(analyze(project, file, editor, isBackground = isBackground, hasLogika = false, line))
    }
  }

  def getCurrentLine(editor: Editor): Int = {
    val offset = editor.getCaretModel.getPrimaryCaret.getOffset
    return editor.getDocument.getLineNumber(offset) + 1
  }

  def enableEditor(project: Project, file: VirtualFile, editor: Editor): Unit = {
    if (editor.getUserData(sireumKey) != null) return
    editor.putUserData(sireumKey, EditorEnabled)
    editor.getDocument.addDocumentListener(new DocumentListener {
      override def documentChanged(event: DocumentEvent): Unit = {
        analyzeOpt(project, file, editor, getCurrentLine(editor), true)
      }

      override def beforeDocumentChange(event: DocumentEvent): Unit = {}
    })
  }

  def editorClosed(project: Project): Unit = {
    resetSireumView(project, None)
  }

  def editorOpened(project: Project, file: VirtualFile, editor: Editor): Unit = {
    val (hasSireum, _) = Util.isSireumOrLogikaFile(project)
    if (hasSireum) {
      enableEditor(project, file, editor)
      editor.putUserData(statusKey, false)
      analyzeOpt(project, file, editor, 0, isBackground = false)
    }
  }

  def notifyHelper(projectOpt: Option[Project], editorOpt: Option[Editor],
                   r: org.sireum.server.protocol.Response): Unit = {
    import org.sireum.message.Level
    import org.sireum.server.protocol._
    val project = projectOpt.orNull
    val statusOpt = editorOpt.map(_.getUserData(statusKey))
    r match {
      case r: Logika.Verify.End =>
        if (r.numOfErrors > 0 || r.numOfInternalErrors > 0) {
          if (!r.isBackground || statusOpt.getOrElse(true)) {
            if (r.hasLogika) {
              if (r.isIllFormed) {
                Util.notify(new Notification(
                  groupId, "Slang Error",
                  s"Ill-formed program with ${r.numOfErrors} error(s)",
                  NotificationType.ERROR), project, shouldExpire = true)
              }
              else {
                Util.notify(new Notification(
                  groupId, "Logika Error",
                  s"Programming logic proof is rejected with ${r.numOfErrors} error(s)",
                  NotificationType.ERROR), project, shouldExpire = true)
              }
            }
          }
          editorOpt.foreach(_.putUserData(statusKey, false))
        } else if (r.hasLogika && r.numOfWarnings > 0) {
          Util.notify(new Notification(
            groupId, "Logika Warning",
            s"Programming logic proof is accepted with ${r.numOfWarnings} warning(s)",
            NotificationType.WARNING, null), project, shouldExpire = true)
          editorOpt.foreach(_.putUserData(statusKey, true))
        } else if (!r.wasCancelled) {
          val title = "Logika Verified"
          val icon = verifiedInfoIcon
          if (r.hasLogika && (!r.isBackground || !(statusOpt.getOrElse(false)))) {
            Util.notify(new Notification(groupId, title, "Programming logic proof is accepted",
              NotificationType.INFORMATION, null) {
              override def getIcon: Icon = icon
            }, project, shouldExpire = true)
          }
          editorOpt.foreach(_.putUserData(statusKey, true))
        } else {
          editorOpt.foreach(_.putUserData(statusKey, false))
        }
      case r: Report =>
        r.message.level match {
          case Level.InternalError =>
            Util.notify(new Notification(
              groupId, "Logika Internal Error",
              r.message.text.value,
              NotificationType.ERROR), project, shouldExpire = true)
            editorOpt.foreach(_.putUserData(statusKey, false))
          case _ =>
        }
      case _ =>
    }
  }

  private[intellij] sealed trait ReportItem

  private[intellij] final case class ConsoleReportItem(project: Project,
                                                       file: VirtualFile,
                                                       level: org.sireum.message.Level.Type,
                                                       line: Int,
                                                       column: Int,
                                                       offset: Int,
                                                       length: Int,
                                                       message: String) extends ReportItem {
    override val toString: String = s"[$line, $column] $message"
  }

  private[intellij] final case class HintReportItem(project: Project,
                                                    file: VirtualFile,
                                                    offset: Int,
                                                    message: String) extends ReportItem

  private[intellij] final case class SummoningReportItem(project: Project,
                                                         file: VirtualFile,
                                                         messageHeader: String,
                                                         offset: Int,
                                                         message: String) extends ReportItem {
    override def toString: String = messageHeader
  }

  private def processReport(iproject: Project,
                            file: VirtualFile,
                            r: org.sireum.server.protocol.Response): Option[(Int, ReportItem)] = {
    import org.sireum.server.protocol._
    r match {
      case r: Report =>
        val msg = r.message
        val (line, column, offset, length) = r.message.posOpt match {
          case org.sireum.Some(pos) => (pos.beginLine.toInt, pos.beginColumn.toInt, pos.offset.toInt, pos.length.toInt)
          case _ => (1, 1, -1, -1)
        }
        msg.level match {
          case Level.InternalError =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Error, line, column, offset, length, msg.text.value)))
          case Level.Error =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Error, line, column, offset, length, msg.text.value)))
          case Level.Warning =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Warning, line, column, offset, length, msg.text.value)))
          case Level.Info =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Info, line, column, offset, length, msg.text.value)))
        }
      case r: Logika.Verify.Smt2Query =>
        val text = r.result.query.value
        val line = r.pos.beginLine.toInt
        val offset = r.pos.offset.toInt
        val header = text.lines().limit(2).map(line => line.replace(';', ' ').
          replace("Result:", "").trim).toArray.mkString(": ")
        return Some((line, SummoningReportItem(iproject, file, header, offset, text)))
      case r: Logika.Verify.State =>
        import org.sireum._
        val sts = org.sireum.logika.State.Claim.claimsSTs(r.state.claims, org.sireum.logika.ClaimDefs.empty)
        val text = normalizeChars(
          st"""{
              |  ${(sts, ",\n")}
              |}""".render.value)
        val pos = r.posOpt.get
        val line = pos.beginLine.toInt
        val offset = pos.offset.toInt
        return scala.Some((line, HintReportItem(iproject, file, offset, text)))
      case _ =>
    }
    None
  }

  def gutterIconRenderer(tooltipText: String, icon: Icon, action: AnAction): GutterIconRenderer = {
    new GutterIconRenderer {
      val ttext: String = if (tooltipText.length > maxTooltipLength) tooltipText.substring(0, maxTooltipLength) + "..." else tooltipText

      override val getTooltipText: String = ttext

      override def getIcon: Icon = icon

      override def equals(other: Any): Boolean = false

      override def hashCode: Int = System.identityHashCode(this)

      override def getClickAction: AnAction = action
    }
  }

  def saveSetDividerLocation(divider: JSplitPane, weight: Double): Unit = {
    val w = divider.getResizeWeight
    if (w != 0.0 && w != 1.0) {
      dividerWeight = w
    }
    divider.setDividerLocation(weight)
  }

  def sireumToolWindowFactory(project: Project,
                              g: SireumToolWindowFactory.Forms => Unit): Unit =
    Option(SireumToolWindowFactory.windows.get(project)).foreach(g)

  def toASCII(message: String): String = {
    val sb = new StringBuilder
    var i = 0
    val len = message.length
    while (i < len) {
      val c = message(i)

      def isPrevWhitespace =
        if (i - 1 >= 0) message(i - 1).isWhitespace else true

      def isNextWhitespace =
        if (i + 1 < len) message(i + 1).isWhitespace else true

      c match {
        case '⊢' => sb.append("|-")
        case '∧' => sb.append('&')
        case '∨' => sb.append('|')
        case '¬' => sb.append('!')
        case '→' => sb.append("->:")
        case '⟶' => sb.append("-->:")
        case '∀' => sb.append("All")
        case '∃' => sb.append("Exists")
        case '⊤' => sb.append("T")
        case '⊥' => sb.append("F")
        case '≤' => sb.append("<=")
        case '≥' => sb.append(">=")
        case '≠' => sb.append("!=")
        case _ => sb.append(c)
      }
      i += 1
    }
    sb.toString
  }

  def resetSireumView(project: Project, editorOpt: Option[Editor]): Unit = {
    sireumToolWindowFactory(project, f => {
      val list = f.logika.logikaList
      list.synchronized {
        for (lsl <- list.getListSelectionListeners) {
          list.removeListSelectionListener(lsl)
        }
        list.setModel(new DefaultListModel[Object]())
        list.addListSelectionListener(_ => list.synchronized {
          val i = list.getSelectedIndex
          if (0 <= i && i < list.getModel.getSize)
            list.getModel.getElementAt(i) match {
              case sri: SummoningReportItem =>
                f.logika.logikaToolSplitPane.setDividerLocation(dividerWeight)
                f.logika.logikaTextArea.setText(normalizeChars(sri.message))
                f.logika.logikaTextArea.setCaretPosition(0)
                for (editor <- editorOpt if !editor.isDisposed)
                  TransactionGuard.submitTransaction(project, (() =>
                    FileEditorManager.getInstance(project).openTextEditor(
                      new OpenFileDescriptor(sri.project, sri.file, sri.offset), true)): Runnable)
              case cri: ConsoleReportItem =>
                for (editor <- editorOpt if !editor.isDisposed)
                  TransactionGuard.submitTransaction(project, (() =>
                    FileEditorManager.getInstance(project).openTextEditor(
                      new OpenFileDescriptor(cri.project, cri.file, cri.offset), true)): Runnable)
              case hri: HintReportItem =>
                f.logika.logikaToolSplitPane.setDividerLocation(dividerWeight)
                f.logika.logikaTextArea.setText(normalizeChars(hri.message))
                f.logika.logikaTextArea.setCaretPosition(0)
                for (editor <- editorOpt if !editor.isDisposed)
                  TransactionGuard.submitTransaction(project, (() =>
                    FileEditorManager.getInstance(project).openTextEditor(
                      new OpenFileDescriptor(hri.project, hri.file, hri.offset), true)): Runnable)
            }
        })
      }
      f.logika.logikaTextArea.setText("")
      saveSetDividerLocation(f.logika.logikaToolSplitPane, 1.0)
    })
  }

  def normalizeChars(text: String): String = {
    if (!LogikaConfigurable.hintUnicode) toASCII(text)
    else text
  }

  def processResult(r: org.sireum.server.protocol.Response): Unit =
    ApplicationManager.getApplication.invokeLater(() => analysisDataKey.synchronized {
      import org.sireum._
      val (project, file, editor, input) = editorMap.synchronized {
        editorMap.get(r.id) match {
          case scala.Some(pe) =>
            r match {
              case r: org.sireum.server.protocol.Logika.Verify.End => editorMap -= r.id
              case _: server.protocol.Logika.Verify.Start => resetSireumView(pe._1, scala.Some(pe._3))
              case r: org.sireum.server.protocol.Report if r.message.level == Level.InternalError =>
                notifyHelper(scala.Some(pe._1), if (pe._3.isDisposed) scala.None else scala.Some(pe._3), r)
              case _ =>
            }
            pe
          case _ =>
            notifyHelper(scala.None, scala.None, r)
            return
        }
      }
      if (!editor.isDisposed) {
        val mm = editor.getMarkupModel
        val tOpt = scala.Option(editor.getUserData(analysisDataKey))
        var (rhs, listModel, summoningListModelMap, hintListModelMap) = tOpt.getOrElse((null, null, null, null))
        r match {
          case _: server.protocol.Logika.Verify.Start =>
            sireumToolWindowFactory(project, f => {
              f.logika.logikaTextArea.setFont(
                editor.getColorsScheme.getFont(EditorFontType.PLAIN))
              f.logika.logikaTextArea.setText("")
            })
            editor.getContentComponent.setToolTipText(null)
            for (rh <- mm.getAllHighlighters if rh.getUserData(reportItemKey) != null) {
              mm.removeHighlighter(rh)
            }
            editor.putUserData(analysisDataKey, null)
            rhs = scala.collection.immutable.Map[Int, Vector[RangeHighlighter]]()
            listModel = new DefaultListModel[Object]()
            summoningListModelMap = scala.collection.immutable.Map[Int, DefaultListModel[SummoningReportItem]]()
            hintListModelMap = scala.collection.immutable.Map[Int, DefaultListModel[HintReportItem]]()
          case r: server.protocol.Logika.Verify.End =>
            notifyHelper(scala.Some(project), scala.Some(editor), r)
            return
          case _ =>
        }
        if (input != editor.getDocument.getText) return
        val cs = editor.getColorsScheme
        val layer = 1000000
        val tooltipSep = "<hr>"

        def consoleReportItems(ci: ConsoleReportItem, line: Int): Unit = {
          var level = ci.level
          try {
            val (message, rhl): (Predef.String, Vector[RangeHighlighter]) = rhs.get(line) match {
              case scala.Some(rhv) =>
                var msg = ci.message
                var newRhv = Vector[RangeHighlighter]()
                for (rh <- rhv) {
                  rh.getUserData(reportItemKey) match {
                    case cri: ConsoleReportItem =>
                      mm.removeHighlighter(rh)
                      msg = cri.message + tooltipSep + ci.message
                      if (cri.level.ordinal < level.ordinal) {
                        level = cri.level
                      }
                    case _ => newRhv = newRhv :+ rh
                  }
                }
                (msg, newRhv)
              case _ => (ci.message, Vector())
            }
            val (icon, color) = level match {
              case Level.InternalError =>
                (gutterErrorIcon, cs.getAttributes(TextAttributesKey.find("ERRORS_ATTRIBUTES")).getErrorStripeColor)
              case Level.Error => (gutterErrorIcon, cs.getAttributes(TextAttributesKey.find("ERRORS_ATTRIBUTES")).getErrorStripeColor)
              case Level.Warning => (gutterWarningIcon, cs.getAttributes(TextAttributesKey.find("WARNING_ATTRIBUTES")).getErrorStripeColor)
              case Level.Info => (gutterInfoIcon, cs.getAttributes(TextAttributesKey.find("TYPO")).getEffectColor)
            }
            val attr = new TextAttributes(null, null, color, EffectType.WAVE_UNDERSCORE, Font.PLAIN)
            val end = scala.math.min(ci.offset + ci.length, editor.getDocument.getTextLength)
            val rhLine = mm.addLineHighlighter(line - 1, layer, null)
            rhLine.putUserData(reportItemKey, ci)
            rhLine.setThinErrorStripeMark(false)
            rhLine.setErrorStripeMarkColor(color)
            rhLine.setGutterIconRenderer(gutterIconRenderer(message,
              icon, _ => sireumToolWindowFactory(project, f => {
                val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
                tw.activate(() => {
                  saveSetDividerLocation(f.logika.logikaToolSplitPane, 1.0)
                  val list = f.logika.logikaList
                  list.synchronized(list.setModel(listModel))
                  tw.getContentManager.setSelectedContent(tw.getContentManager.findContent("Output"))
                })
              })))
            rhLine.putUserData(reportItemKey, ci)
            if (ci.offset != -1) {
              val rh = mm.addRangeHighlighter(ci.offset, end, layer, attr, HighlighterTargetArea.EXACT_RANGE)
              rh.putUserData(reportItemKey, ci)
              rh.setErrorStripeTooltip(ci.message)
              rh.setThinErrorStripeMark(false)
              rh.setErrorStripeMarkColor(color)
              listModel.addElement(ci)
              rhs = rhs + ((line, rhl :+ rhLine :+ rh))
            } else {
              rhs = rhs + ((line, rhl :+ rhLine))

            }
          } catch {
            case t: Throwable =>
              t.printStackTrace()
          }
        }

        for ((line, ri) <- processReport(project, file, r)) {
          ri match {
            case ri: ConsoleReportItem => consoleReportItems(ri, line)
            case ri: HintReportItem =>
              scala.util.Try {
                val rhl: Vector[RangeHighlighter] = rhs.get(line) match {
                  case scala.Some(rhv) =>
                    var newRhv = Vector[RangeHighlighter]()
                    for (rh <- rhv) {
                      rh.getUserData(reportItemKey) match {
                        case _: HintReportItem => mm.removeHighlighter(rh)
                        case _ => newRhv = newRhv :+ rh
                      }
                    }
                    newRhv
                  case _ => Vector()
                }
                val hintListModel = hintListModelMap.get(line) match {
                  case scala.Some(l) => l
                  case _ =>
                    val l = new DefaultListModel[HintReportItem]()
                    hintListModelMap = hintListModelMap + ((line, l))
                    l
                }
                hintListModel.addElement(ri)
                val rhLine = mm.addLineHighlighter(line - 1, layer, null)
                rhLine.putUserData(reportItemKey, ri)
                rhLine.setThinErrorStripeMark(false)
                rhLine.setGutterIconRenderer(gutterIconRenderer("Click to show some hints",
                  gutterHintIcon, _ => sireumToolWindowFactory(project, f => {
                    val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
                    tw.activate(() => {
                      saveSetDividerLocation(f.logika.logikaToolSplitPane, 0.0)
                      f.logika.logikaTextArea.setText(normalizeChars(ri.message))
                      f.logika.logikaTextArea.setCaretPosition(f.logika.logikaTextArea.getDocument.getLength)
                      tw.getContentManager.setSelectedContent(tw.getContentManager.findContent("Output"))
                    })
                  })
                ))
                rhs = rhs + ((line, rhl :+ rhLine))
              }
            case ri: SummoningReportItem =>
              scala.util.Try {
                val rhl: Vector[RangeHighlighter] = rhs.get(line) match {
                  case scala.Some(rhv) =>
                    var newRhv = Vector[RangeHighlighter]()
                    for (rh <- rhv) {
                      rh.getUserData(reportItemKey) match {
                        case _: SummoningReportItem => mm.removeHighlighter(rh)
                        case _ => newRhv = newRhv :+ rh
                      }
                    }
                    newRhv
                  case _ => Vector()
                }
                val summoningListModel = summoningListModelMap.get(line) match {
                  case scala.Some(l) => l
                  case _ =>
                    val l = new DefaultListModel[SummoningReportItem]()
                    summoningListModelMap = summoningListModelMap + ((line, l))
                    l
                }
                summoningListModel.addElement(ri)
                val rhLine = mm.addLineHighlighter(line - 1, layer, null)
                rhLine.putUserData(reportItemKey, ri)
                rhLine.setThinErrorStripeMark(false)
                rhLine.setGutterIconRenderer(gutterIconRenderer("Click to show scribed incantations",
                  gutterSummoningIcon, _ => sireumToolWindowFactory(project, f => {
                    val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
                    tw.activate(() => {
                      val list = f.logika.logikaList
                      list.synchronized {
                        list.setModel(summoningListModel.asInstanceOf[DefaultListModel[Object]])
                        var selection = 0
                        var i = 0
                        while (i < summoningListModel.size && selection == 0) {
                          if (summoningListModel.elementAt(i).messageHeader.contains("Invalid")) {
                            selection = i
                          }
                          i += 1
                        }
                        i = 0
                        while (i < summoningListModel.size && selection == 0) {
                          if (summoningListModel.elementAt(i).messageHeader.contains("Don't Know")) {
                            selection = i
                          }
                          i += 1
                        }
                        i = 0
                        while (i < summoningListModel.size && selection == 0) {
                          if (summoningListModel.elementAt(i).messageHeader.contains("Timeout")) {
                            selection = i
                          }
                          i += 1
                        }
                        i = 0
                        while (i < summoningListModel.size && selection == 0) {
                          if (summoningListModel.elementAt(i).messageHeader.contains("Error")) {
                            selection = i
                          }
                          i += 1
                        }
                        list.setSelectedIndex(selection)
                      }
                      tw.getContentManager.setSelectedContent(tw.getContentManager.findContent("Output"))
                    })
                  })))
                rhs = rhs + ((line, rhl :+ rhLine))
              }
          }
        }
        editor.putUserData(analysisDataKey, (rhs, listModel, summoningListModelMap, hintListModelMap))
      }
    })
}
