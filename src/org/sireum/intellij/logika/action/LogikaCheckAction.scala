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

package org.sireum.intellij.logika.action

import java.awt.{Color, Font}
import java.awt.event.MouseEvent
import java.util.concurrent._
import javax.swing.{DefaultListModel, Icon, JSplitPane}
import com.intellij.openapi.editor.colors.{EditorFontType, TextAttributesKey}
import com.intellij.openapi.editor.event._
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.{ApplicationManager, TransactionGuard}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup._
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.{Balloon, JBPopupFactory}
import com.intellij.openapi.util._
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget.{IconPresentation, PlatformType, WidgetPresentation}
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.openapi.wm.{StatusBar, StatusBarWidget, WindowManager}
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import org.sireum.intellij.{SireumApplicationComponent, SireumToolWindowFactory, Util}
import org.sireum.intellij.logika.LogikaConfigurable
import org.sireum.message.Level

object LogikaCheckAction {

  object EditorEnabled

  val icons: Seq[Icon] = {
    var r = (0 to 6).map(n => IconLoader.getIcon(s"/logika/icon/logika-$n.png"))
    r = r.dropRight(1)
    r ++= r.reverse
    r = r.dropRight(1)
    r
  }
  val icon: Icon = icons.head
  val gutterErrorIcon: Icon = IconLoader.getIcon("/logika/icon/logika-gutter-error.png")
  val gutterWarningIcon: Icon = IconLoader.getIcon("/logika/icon/logika-gutter-warning.png")
  val gutterInfoIcon: Icon = IconLoader.getIcon("/logika/icon/logika-gutter-info.png")
  val gutterHintIcon: Icon = IconLoader.getIcon("/logika/icon/logika-gutter-hint.png")
  val gutterSummoningIcon: Icon = IconLoader.getIcon("/logika/icon/logika-gutter-summoning.png")
  val verifiedInfoIcon: Icon = IconLoader.getIcon("/logika/icon/logika-verified-info.png")
  val queue = new LinkedBlockingQueue[String]()
  val editorMap: scala.collection.mutable.Map[org.sireum.ISZ[org.sireum.String], (Project, VirtualFile, Editor, String)] = scala.collection.mutable.Map()
  val logikaKey = new Key[EditorEnabled.type]("Logika")
  val analysisDataKey = new Key[(Map[Int, RangeHighlighter], DefaultListModel[Object])]("Logika Analysis Data")
  val statusKey = new Key[Boolean]("Logika Analysis Status")
  val summoningKey = new Key[DefaultListModel[SummoningReportItem]]("Summoning list model")
  var request: Option[Request] = None
  var processInit: Option[scala.sys.process.Process] = None
  var terminated = false
  var dividerWeight: Double = .2
  var tooltipMessageOpt: Option[String] = None
  var tooltipBalloonOpt: Option[Balloon] = None
  val tooltipDefaultBgColor: Color = new Color(0xff, 0xff, 0xcc)
  val tooltipDarculaBgColor: Color = new Color(0x5c, 0x5c, 0x42)

  final case class Request(time: Long, requestId: org.sireum.ISZ[org.sireum.String],
                           project: Project, file: VirtualFile, editor: Editor,
                           input: String, msgGen: () => String)

  def init(p: Project): Unit = {
    if (processInit.isEmpty) {
      processInit =
        SireumApplicationComponent.getSireumProcess(p,
          queue, { s =>
            val trimmed = s.trim
            if (trimmed != "") {
              import org.sireum.server.protocol._
              CustomMessagePack.toResponse(trimmed) match {
                case org.sireum.Either.Left(r: ResponseId) => processResult(r)
              }
            }
          }, "x", "server", "-m", "msgpack")
      if (processInit.isEmpty) return
      val statusBar = WindowManager.getInstance().getStatusBar(p)
      var frame = 0
      val statusIdle = "Sireum Logika is idle"
      val statusWaiting = "Sireum Logika is waiting to work"
      val statusWorking = "Sireum Logika is working"
      var statusTooltip = statusIdle
      var shutdown = false
      lazy val statusBarWidget: StatusBarWidget = new StatusBarWidget {

        override def ID(): String = "Sireum Logika"

        override def install(statusBar: StatusBar): Unit = {}

        override def getPresentation: WidgetPresentation =
          new IconPresentation {
            override def getClickConsumer: Consumer[MouseEvent] = _ =>
              if (Messages.showYesNoDialog(
                p, "Shutdown Sireum Logika background server?",
                "Sireum Logika", null) == Messages.YES)
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
          while (!terminated && !shutdown) {
            if (editorMap.nonEmpty || request.nonEmpty) {
              frame = (frame + 1) % icons.length
              statusTooltip =
                if (editorMap.nonEmpty) statusWorking
                else statusWaiting
              statusBar.updateWidget(statusBarWidget.ID())
            } else {
              val f = frame
              frame = defaultFrame
              statusTooltip = statusIdle
              if (f != defaultFrame)
                statusBar.updateWidget(statusBarWidget.ID())
            }
            this.synchronized {
              request match {
                case Some(r: Request) =>
                  if (System.currentTimeMillis - r.time > LogikaConfigurable.idle) {
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

  def isEnabled(editor: Editor): Boolean = EditorEnabled == editor.getUserData(logikaKey)

  def analyze(project: Project, file: VirtualFile, editor: Editor, isBackground: Boolean): Unit = {
    if (editor.isDisposed || !isEnabled(editor)) return
    if (isBackground && !LogikaConfigurable.backgroundAnalysis) return
    init(project)
    val input = editor.getDocument.getText
    val (t, requestId) = {
      val t = System.currentTimeMillis
      val id = org.sireum.ISZ(org.sireum.String(t.toString))
      (t, id)
    }

    def f(): String = {
      import org.sireum.server.protocol._
      CustomMessagePack.fromRequest(Logika.Verify.CheckScript(
        isBackground = isBackground,
        id = requestId,
        uriOpt = org.sireum.Some(org.sireum.String(file.toNioPath.toUri.toASCIIString)),
        content = input
      )).value
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


  def enableEditor(project: Project, file: VirtualFile, editor: Editor): Unit = {
    if (editor.getUserData(logikaKey) != null) return
    editor.putUserData(logikaKey, EditorEnabled)
    editor.getDocument.addDocumentListener(new DocumentListener {
      override def documentChanged(event: DocumentEvent): Unit = {
        if (LogikaConfigurable.backgroundAnalysis)
          scala.util.Try(analyze(project, file, editor, isBackground = true))
      }

      override def beforeDocumentChange(event: DocumentEvent): Unit = {}
    })
    editor.addEditorMouseMotionListener(new EditorMouseMotionListener {
      override def mouseMoved(e: EditorMouseEvent): Unit = {
        if (!EditorMouseEventArea.EDITING_AREA.equals(e.getArea))
          return
        val (rhs, _) = editor.getUserData(analysisDataKey)
        if (rhs == null) return
        val component = editor.getContentComponent
        val point = e.getMouseEvent.getPoint
        val pos = editor.xyToLogicalPosition(point)
        val offset = editor.logicalPositionToOffset(pos)
        editor.synchronized {
          tooltipMessageOpt match {
            case Some(_) => tooltipMessageOpt = None
            case _ =>
          }
          tooltipBalloonOpt match {
            case Some(b) => b.hide(); b.dispose()
            case _ =>
          }
        }
        var msgs = Vector[String]()
        for ((_, rh) <- rhs if rh.getErrorStripeTooltip != null)
          if (rh.getStartOffset <= offset && offset <= rh.getEndOffset) {
            msgs :+= rh.getErrorStripeTooltip.toString
          }
        if (msgs.nonEmpty) {
          editor.synchronized {
            tooltipMessageOpt = Some(msgs.mkString("<hr>"))
          }
          new Thread() {
            override def run(): Unit = {
              val tbo = editor.synchronized(tooltipMessageOpt)
              Thread.sleep(500)
              editor.synchronized {
                if (tbo eq tooltipMessageOpt) tooltipMessageOpt match {
                  case Some(msg) =>
                    val color = if (UIUtil.isUnderDarcula) tooltipDarculaBgColor else tooltipDefaultBgColor
                    val builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
                      msg, null, color, null)
                    val b = builder.createBalloon()
                    tooltipBalloonOpt = Some(b)
                    ApplicationManager.getApplication.invokeLater(
                      { () =>
                        b.show(new RelativePoint(component, point), Balloon.Position.below)
                      }: Runnable,
                      ((_: Any) => b.isDisposed): Condition[Any])
                  case _ =>
                }
              }
            }
          }.start()
        }
      }

      override def mouseDragged(e: EditorMouseEvent): Unit = {}
    })
  }

  def editorClosed(project: Project): Unit = {
    resetSireumView(project)
  }

  def editorOpened(project: Project, file: VirtualFile, editor: Editor): Unit = {
    if (Util.isSireumOrLogikaFile(project) == (true, true)) {
      enableEditor(project, file, editor)
      editor.putUserData(statusKey, false)
      analyze(project, file, editor, isBackground = true)
    }
  }

  def notifyHelper(projectOpt: Option[Project], editorOpt: Option[Editor],
                   r: org.sireum.server.protocol.ResponseId): Unit = {
    import org.sireum.server.protocol._
    import org.sireum.message.Level
    val project = projectOpt.orNull
    val statusOpt = editorOpt.map(_.getUserData(statusKey))
    r match {
      case r: Logika.Verify.End =>
        if (r.numOfErrors > 0) {
          if (!r.isBackground || statusOpt.getOrElse(true))
            Util.notify(new Notification(
              "Sireum Logika", "Logika Error",
              s"Verification failed with ${r.numOfErrors} error(s)",
              NotificationType.ERROR), project, shouldExpire = true)
          editorOpt.foreach(_.putUserData(statusKey, false))
        } else if (r.numOfWarnings > 0) {
          Util.notify(new Notification(
            "Sireum Logika", "Logika Warning",
            s"Successful verification with ${r.numOfWarnings} warning(s)",
            NotificationType.WARNING, null), project, shouldExpire = true)
        } else {
          editorOpt.foreach(_.putUserData(statusKey, true))
          val title = "Logika Verified"
          val icon = verifiedInfoIcon
          if (!r.isBackground || !(statusOpt.getOrElse(false)))
            Util.notify(new Notification("Sireum Logika", title, "Successful verification",
              NotificationType.INFORMATION, null) {
              override def getIcon: Icon = icon
            }, project, shouldExpire = true)
        }
      case r: ReportId =>
        r.message.level match {
          case Level.InternalError =>
            Util.notify(new Notification(
              "Sireum Logika", "Logika Internal Error",
              r.message.text.value,
              NotificationType.ERROR), project, shouldExpire = true)
            editorOpt.foreach(_.putUserData(statusKey, false))
          case _ =>
        }
      case _ =>
    }
  }

  private[action] sealed trait ReportItem

  private[action] final case class ConsoleReportItem(project: Project,
                                             file: VirtualFile,
                                             level: org.sireum.message.Level.Type,
                                             line: Int,
                                             column: Int,
                                             offset: Int,
                                             length: Int,
                                             message: String) extends ReportItem {
    override val toString: String = s"[$line, $column] $message"
  }

  // TODO
  // private[action] final case class CheckSatReportItem(message: String) extends ReportItem

  private[action] final case class HintReportItem(message: String) extends ReportItem

  private[action] final case class SummoningReportItem(project: Project,
                                               file: VirtualFile,
                                               messageFirstLine: String,
                                               offset: Int,
                                               message: String) extends ReportItem {
    override def toString: String = messageFirstLine
  }

  private def processReportId(project: Project,
                              file: VirtualFile,
                              r: org.sireum.server.protocol.ResponseId): Option[(Int, ReportItem)] = {
    import org.sireum.server.protocol._
    r match {
      case r: ReportId =>
        r.message.posOpt match {
          case org.sireum.Some(pos) =>
            val line = pos.beginLine.toInt
            val msg = r.message
            msg.level match {
              case Level.InternalError =>
                return Some((line, ConsoleReportItem(project, file, Level.Error, pos.beginLine.toInt, pos.beginColumn.toInt, pos.offset.toInt, pos.length.toInt, msg.text.value)))
              case Level.Error =>
                return Some((line, ConsoleReportItem(project, file, Level.Error, pos.beginLine.toInt, pos.beginColumn.toInt, pos.offset.toInt, pos.length.toInt, msg.text.value)))
              case Level.Warning =>
                return Some((line, ConsoleReportItem(project, file, Level.Warning, pos.beginLine.toInt, pos.beginColumn.toInt, pos.offset.toInt, pos.length.toInt, msg.text.value)))
              case Level.Info =>
                return Some((line, ConsoleReportItem(project, file, Level.Info, pos.beginLine.toInt, pos.beginColumn.toInt, pos.offset.toInt, pos.length.toInt, msg.text.value)))
            }
          case _ =>
        }
      case r: Logika.Verify.Smt2Query =>
        val text = r.result.query.value
        val line = r.pos.beginLine.toInt
        val offset = r.pos.offset.toInt
        val firstLine = text.substring(text.indexOf(';') + 1, text.indexOf('\n')).trim
        return Some((line, SummoningReportItem(project, file, firstLine, offset, text)))
      case r: Logika.Verify.State =>
        import org.sireum._
        val sts = org.sireum.logika.State.Claim.claimsSTs(r.state.claims, org.sireum.logika.ClaimDefs.empty)
        val text =
          st"""${(sts, ",\n")}
              |""".render.value
        val line = r.posOpt.get.beginLine.toInt
        return scala.Some((line, HintReportItem(text)))
      // TODO
      //case Level.Warning if msg.kind === "checksat" =>
      //  return Some((line, CheckSatReportItem(msg.text.value)))
      case _ =>
    }
    None
  }

  def gutterIconRenderer(tooltipText: String, icon: Icon, action: AnAction): GutterIconRenderer =
    new GutterIconRenderer {
      override val getTooltipText: String = tooltipText

      override def getIcon: Icon = icon

      override def equals(other: Any): Boolean = false

      override def hashCode: Int = System.identityHashCode(this)

      override def getClickAction: AnAction = action
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

      def appendWs(c: Char) = {
        if (!isPrevWhitespace) sb.append(' ')
        sb.append(c)
        if (!isNextWhitespace) sb.append(' ')
      }

      c match {
        case '⊢' => sb.append("|-")
        case '∧' => sb.append('^')
        case '∨' => appendWs('V')
        case '¬' => sb.append('~')
        case '→' => sb.append("->")
        case '∀' => appendWs('A')
        case '∃' => appendWs('E')
        case '⊤' => appendWs('T')
        case '⊥' => sb.append("_|_")
        case '≤' => sb.append("<=")
        case '≥' => sb.append(">=")
        case '≠' => sb.append("!=")
        case _ => sb.append(c)
      }
      i += 1
    }
    sb.toString
  }

  def resetSireumView(project: Project): Unit = {
    sireumToolWindowFactory(project, f => {
      val list = f.logika.logikaList
      for (lsl <- list.getListSelectionListeners) {
        list.removeListSelectionListener(lsl)
      }
      list.setModel(new DefaultListModel[Object]())
      f.logika.logikaTextArea.setText("")
      saveSetDividerLocation(f.logika.logikaToolSplitPane, 1.0)
    })
  }

  def normalizeChars(text: String): String = {
    if (!LogikaConfigurable.hintUnicode) toASCII(text)
    else text
  }

  def processResult(r: org.sireum.server.protocol.ResponseId): Unit =
    ApplicationManager.getApplication.invokeLater(() => analysisDataKey.synchronized {
      import org.sireum._
      val (project, file, editor, input) = editorMap.synchronized {
        editorMap.get(r.id) match {
          case scala.Some(pe) =>
            r match {
              case r: org.sireum.server.protocol.Logika.Verify.End => editorMap -= r.id
              case _: server.protocol.Logika.Verify.Start => resetSireumView(pe._1)
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
        var (rhs, listModel) = editor.getUserData(analysisDataKey)
        r match {
          case _: server.protocol.Logika.Verify.Start =>
            sireumToolWindowFactory(project, f => {
              f.logika.logikaTextArea.setFont(
                editor.getColorsScheme.getFont(EditorFontType.PLAIN))
              f.logika.logikaTextArea.setText("")
            })
            editor.getContentComponent.setToolTipText(null)
            if (rhs != null) for ((_, rh) <- rhs) mm.removeHighlighter(rh)
            editor.putUserData(analysisDataKey, null)
            rhs = scala.collection.immutable.Map[Int, RangeHighlighter]()
            listModel = new DefaultListModel[Object]()
          case r: server.protocol.ReportId =>
            notifyHelper(scala.Some(project), scala.Some(editor), r)
            return
          case _ =>
        }
        if (input != editor.getDocument.getText) return
        val cs = editor.getColorsScheme
        val layer = 1000000
        val tooltipSep = "<hr>"

        def consoleReportItems(ci: ConsoleReportItem, line: Int): Unit = {
          val (icon, color) = ci.level match {
            case Level.InternalError => (gutterErrorIcon, cs.getAttributes(TextAttributesKey.find("ERRORS_ATTRIBUTES")).getErrorStripeColor)
            case Level.Error => (gutterErrorIcon, cs.getAttributes(TextAttributesKey.find("ERRORS_ATTRIBUTES")).getErrorStripeColor)
            case Level.Warning => (gutterWarningIcon, cs.getAttributes(TextAttributesKey.find("WARNING_ATTRIBUTES")).getErrorStripeColor)
            case Level.Info => (gutterInfoIcon, cs.getAttributes(TextAttributesKey.find("TYPO")).getEffectColor)
          }
          val attr = new TextAttributes(null, null, color, EffectType.WAVE_UNDERSCORE, Font.PLAIN)
          scala.util.Try {
            val message: Predef.String = rhs.get(line) match {
              case scala.Some(rh) =>
                mm.removeHighlighter(rh)
                rh.getGutterIconRenderer.getTooltipText + tooltipSep + ci.message
              case _ => ci.message
            }
            val rhLine = mm.addLineHighlighter(line - 1, layer, null)
            rhLine.setThinErrorStripeMark(false)
            rhLine.setErrorStripeMarkColor(color)
            rhLine.setGutterIconRenderer(gutterIconRenderer(message,
              icon, _ => sireumToolWindowFactory(project, f => {
                val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
                tw.activate(() => {
                  saveSetDividerLocation(f.logika.logikaToolSplitPane, 1.0)
                  val list = f.logika.logikaList
                  list.synchronized(list.setModel(listModel))
                })
              })))
            rhs = rhs + ((line, rhLine))
            val end = scala.math.min(ci.offset + ci.length, editor.getDocument.getTextLength)
            val rh = mm.addRangeHighlighter(ci.offset, end, layer, attr, HighlighterTargetArea.EXACT_RANGE)
            rh.setErrorStripeTooltip(ci.message)
            rh.setThinErrorStripeMark(false)
            rh.setErrorStripeMarkColor(color)
            rhs = rhs + ((line, rh))
            listModel.addElement(ci)
          }
        }

        for ((line, ri) <- processReportId(project, file, r)) {
          /* TODO
          if (ris.checksat.nonEmpty) scala.util.Try {
            val rhLine = mm.addLineHighlighter(line - 1, layer, null)
            rhLine.setThinErrorStripeMark(false)
            rhLine.setGutterIconRenderer(gutterIconRenderer(ris.checksat.map(_.message).mkString(tooltipSep),
              warningIcon, null))
            rhs :+= rhLine
          }
          */
          ri match {
            case ri: ConsoleReportItem => consoleReportItems(ri, line)
            case ri: HintReportItem =>
              scala.util.Try {
                val rhLine = mm.addLineHighlighter(line - 1, layer, null)
                rhLine.setThinErrorStripeMark(false)
                rhLine.setGutterIconRenderer(gutterIconRenderer("Click to show some hints",
                  gutterHintIcon, _ => sireumToolWindowFactory(project, f => {
                    val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
                    val font = editor.getColorsScheme.getFont(EditorFontType.PLAIN)
                    f.logika.logikaTextArea.setFont(font)
                    tw.activate(() => {
                      saveSetDividerLocation(f.logika.logikaToolSplitPane, 0.0)
                      f.logika.logikaTextArea.setText(normalizeChars(ri.message))
                      f.logika.logikaTextArea.setCaretPosition(0)
                    })
                  })
                ))
                rhs = rhs + ((line, rhLine))
              }
            case ri: SummoningReportItem =>
              scala.util.Try {
                val summoningListModel = rhs.get(line) match {
                  case scala.Some(rh) =>
                    mm.removeHighlighter(rh)
                    mm.getUserData(summoningKey)
                  case _ =>
                    new DefaultListModel[SummoningReportItem]()
                }
                summoningListModel.addElement(ri)
                val rhLine = mm.addLineHighlighter(line - 1, layer, null)
                rhLine.putUserData(summoningKey, summoningListModel)
                rhLine.setThinErrorStripeMark(false)
                rhLine.setGutterIconRenderer(gutterIconRenderer("Click to show scribed incantations",
                  gutterSummoningIcon, _ => sireumToolWindowFactory(project, f => {
                    val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
                    val font = editor.getColorsScheme.getFont(EditorFontType.PLAIN)
                    f.logika.logikaTextArea.setFont(font)
                    tw.activate(() => {
                      val list = f.logika.logikaList
                      list.synchronized {
                        list.setModel(summoningListModel.asInstanceOf[DefaultListModel[Object]])
                        var selection = 0
                        var i = 0
                        while (i < summoningListModel.size && selection == 0) {
                          if (summoningListModel.elementAt(i).messageFirstLine.contains("Invalid")) {
                            selection = i
                          }
                          i += 1
                        }
                        i = 0
                        while (i < summoningListModel.size && selection == 0) {
                          if (summoningListModel.elementAt(i).messageFirstLine.contains("Don't Know")) {
                            selection = i
                          }
                          i += 1
                        }
                        list.setSelectedIndex(selection)
                      }
                    })
                  })))
                rhs = rhs + ((line, rhLine))
              }
          }
        }
        sireumToolWindowFactory(project, f => {
          f.logika.logikaTextArea.setFont(editor.getColorsScheme.getFont(EditorFontType.PLAIN))
          val list = f.logika.logikaList
          list.synchronized {
            for (lsl <- list.getListSelectionListeners) {
              list.removeListSelectionListener(lsl)
            }
            list.setModel(listModel)
            list.addListSelectionListener(_ => list.synchronized {
              val i = list.getSelectedIndex
              if (0 <= i && i < list.getModel.getSize)
                list.getModel.getElementAt(i) match {
                  case sri: SummoningReportItem =>
                    f.logika.logikaToolSplitPane.setDividerLocation(dividerWeight)
                    f.logika.logikaTextArea.setText(normalizeChars(sri.message))
                    f.logika.logikaTextArea.setCaretPosition(0)
                    if (!editor.isDisposed)
                      TransactionGuard.submitTransaction(project, (() =>
                        FileEditorManager.getInstance(project).openTextEditor(
                          new OpenFileDescriptor(sri.project, sri.file, sri.offset), true)): Runnable)
                  case cri: ConsoleReportItem =>
                    if (!editor.isDisposed)
                      TransactionGuard.submitTransaction(project, (() =>
                        FileEditorManager.getInstance(project).openTextEditor(
                          new OpenFileDescriptor(cri.project, cri.file, cri.offset), true)): Runnable)
                }
            })
          }
          f.logika.logikaTextArea.setText("")
          saveSetDividerLocation(f.logika.logikaToolSplitPane, 1.0)
        })
        editor.putUserData(analysisDataKey, (rhs, listModel))
      }
    })
}

import LogikaCheckAction._

private class LogikaCheckAction extends LogikaOnlyAction {

  // init
  {
    getTemplatePresentation.setIcon(icon)

    val am = ActionManager.getInstance

    val runGroup = am.getAction("SireumLogikaGroup").
      asInstanceOf[DefaultActionGroup]
    runGroup.addAction(this, Constraints.FIRST)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(false)
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
    if (editor == null) return
    enableEditor(project, file, editor)
    analyze(project, file, editor, isBackground = false)
    e.getPresentation.setEnabled(true)
  }
}