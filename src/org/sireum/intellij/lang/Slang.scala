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

package org.sireum.intellij.lang

import java.awt.Font
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import javax.swing.Icon
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.markup.{EffectType, HighlighterTargetArea, RangeHighlighter, TextAttributes}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.{Balloon, JBPopupFactory}
import com.intellij.openapi.util.{Condition, Key}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import org.sireum.{Os, Some => SSome}
import org.sireum.intellij.Util
import org.sireum.intellij.logika.action.LogikaCheckAction
import org.sireum.intellij.logika.action.LogikaCheckAction._
import org.sireum.lang.ast.TopUnit
import org.sireum.lang.logika.TruthTableVerifier
import org.sireum.lang.parser.SlangParser
import org.sireum.message._

object Slang {

  object EditorEnabled

  val changedKey = new Key[Option[Long]]("Slang Last Changed")
  val statusKey = new Key[Boolean]("Logika Last Status")
  val analysisDataKey = new Key[Vector[RangeHighlighter]]("Slang Analysis Data")

  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  val changeThreshold = 1500

  val layer = 1000000
  val tooltipSep = "<hr>"
  val emptyAction: AnAction = _ => {}

  def editorOpened(project: Project, file: VirtualFile, editor: Editor): Unit =
    ApplicationManager.getApplication.invokeLater(() => {
      Util.isSireumOrLogikaFile(project) match {
        case (true, true) =>
          val filePath = Os.path(file.getCanonicalPath)
          val r = check(editor, filePath)
          processResult(editor, r)
          editor.putUserData(changedKey, Some(System.currentTimeMillis()))
          scheduler.scheduleAtFixedRate((() => {
            try analyze(editor, filePath) catch {
              case t: Throwable =>
                val sw = new java.io.StringWriter()
                t.printStackTrace(new java.io.PrintWriter(sw))
                Util.notify(new Notification("Slang", "Slang Internal Error", s"Unexpected error: ${sw.toString}",
                  NotificationType.ERROR), editor.getProject, shouldExpire = true)
            }
          }): Runnable, 0, changeThreshold, TimeUnit.MILLISECONDS)
          editor.getDocument.addDocumentListener(new DocumentListener {
            override def documentChanged(event: DocumentEvent): Unit = {
              if (editor.isDisposed) return
              editor.synchronized {
                editor.putUserData(changedKey, Some(System.currentTimeMillis()))
              }
            }

            override def beforeDocumentChange(event: DocumentEvent): Unit = {
            }
          })
          editor.addEditorMouseMotionListener(new EditorMouseMotionListener {
            override def mouseMoved(e: EditorMouseEvent): Unit = {
              if (!EditorMouseEventArea.EDITING_AREA.equals(e.getArea))
                return
              val rhs = editor.getUserData(analysisDataKey)
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
              for (rh <- rhs if rh.getErrorStripeTooltip != null)
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

        case _ =>
      }

    })

  def check(editor: Editor, filePath: Os.Path): Seq[Message] = {
    val text = editor.getDocument.getText
    val reporter = Reporter.create
    val (noPrev, prevStatus) = Option(editor.getUserData(statusKey)) match {
      case Some(b) => (false, b)
      case _ => (true, false)
    }

    val r = SlangParser(allowSireumPackage = "true" == System.getProperty("org.sireum.ive.dev"),
      isWorksheet = false, isDiet = false, fileUriOpt = SSome(new java.io.File(filePath.string.value).toURI.toASCIIString),
      txt = text, reporter = reporter)
    var status = !reporter.hasIssue.value
    r.unitOpt match {
      case SSome(tt: TopUnit.TruthTableUnit) =>
        TruthTableVerifier.verify(tt, reporter)
        status = !reporter.hasIssue.value
        if (noPrev || (status != prevStatus)) {
          if (status) {
            Util.notify(new Notification("Sireum Logika", "Logika Verified", "Truth Table is accepted",
              NotificationType.INFORMATION) {
              override def getIcon: Icon = verifiedInfoIcon
            }, editor.getProject, shouldExpire = true)
          } else {
            Util.notify(new Notification("Sireum Logika", "Logika Error", "Truth Table is rejected",
              NotificationType.ERROR), editor.getProject, shouldExpire = true)
          }
        }
      case _ =>
    }
    for (m <- reporter.internalErrors) {
      Util.notify(new Notification(
        "Sireum Logika", "Logika Internal Error", m.text.value,
        NotificationType.ERROR), editor.getProject, shouldExpire = true)
    }
    editor.putUserData(statusKey, status)
    reporter.messages.elements
  }

  def analyze(editor: Editor, filePath: Os.Path): Unit = {
    editor.synchronized {
      editor.getUserData(changedKey) match {
        case Some(lastChanged) =>
          val d = System.currentTimeMillis() - lastChanged
          if (d > changeThreshold) {
            val r = check(editor, filePath)
            processResult(editor, r)
            editor.putUserData(changedKey, null)
          }
        case _ =>
      }
    }
  }

  def processResult(editor: Editor, messages: Seq[Message]): Unit =
    ApplicationManager.getApplication.invokeLater(() => editor.synchronized {
      val mm = editor.getMarkupModel

      def addRangeHighlighter(offset: org.sireum.Z, length: org.sireum.Z, attr: TextAttributes): RangeHighlighter = {
        val end = scala.math.min((offset + length).toInt, editor.getDocument.getTextLength)
        mm.addRangeHighlighter(offset.toInt, end.toInt, layer, attr, HighlighterTargetArea.EXACT_RANGE)
      }

      var rhs = editor.getUserData(analysisDataKey)
      if (rhs != null)
        for (rh <- rhs)
          mm.removeHighlighter(rh)
      rhs = Vector[RangeHighlighter]()
      val cs = editor.getColorsScheme
      val errorColor = cs.getAttributes(
        TextAttributesKey.find("ERRORS_ATTRIBUTES")).getErrorStripeColor
      val warningColor = cs.getAttributes(
        TextAttributesKey.find("WARNING_ATTRIBUTES")).getErrorStripeColor
      val infoColor = cs.getAttributes(
        TextAttributesKey.find("TYPO")).getEffectColor
      val (errorIcon, errorAttr) =
        (gutterErrorIcon, new TextAttributes(null, null, errorColor, EffectType.WAVE_UNDERSCORE, Font.PLAIN))
      val (warningIcon, warningAttr) =
        (gutterWarningIcon, new TextAttributes(null, null, warningColor, EffectType.WAVE_UNDERSCORE, Font.PLAIN))
      val (infoIcon, infoAttr) =
        (gutterInfoIcon, new TextAttributes(null, null, infoColor, EffectType.WAVE_UNDERSCORE, Font.PLAIN))
      errorAttr.setErrorStripeColor(errorColor)
      warningAttr.setErrorStripeColor(warningColor)
      infoAttr.setErrorStripeColor(infoColor)
      var lineMap = Map[org.sireum.Z, Vector[Message]]()
      for (msg <- messages) msg.posOpt match {
        case org.sireum.Some(pos) if msg.level != Level.InternalError =>
          msg.level match {
            case Level.Error =>
              lineMap += pos.beginLine -> (lineMap.getOrElse(pos.beginLine, Vector()) :+ msg)
              val rh = addRangeHighlighter(pos.offset, pos.length, errorAttr)
              rh.setErrorStripeTooltip(msg.text.value)
              rh.setThinErrorStripeMark(false)
              rh.setErrorStripeMarkColor(errorColor)
              rhs :+= rh
            case Level.Warning =>
              lineMap += pos.beginLine -> (lineMap.getOrElse(pos.beginLine, Vector()) :+ msg)
              val rh = addRangeHighlighter(pos.offset, pos.length, errorAttr)
              rh.setErrorStripeTooltip(msg.text.value)
              rh.setThinErrorStripeMark(false)
              rh.setErrorStripeMarkColor(warningColor)
              rhs :+= rh
            case Level.Info =>
              lineMap += pos.beginLine -> (lineMap.getOrElse(pos.beginLine, Vector()) :+ msg)
              val rh = addRangeHighlighter(pos.offset, pos.length, errorAttr)
              rh.setErrorStripeTooltip(msg.text.value)
              rh.setThinErrorStripeMark(false)
              rh.setErrorStripeMarkColor(infoColor)
              rhs :+= rh
            case _ =>
          }
        case _ => lineMap = lineMap + ((org.sireum.Z(1), lineMap.getOrElse(1, Vector()) :+ msg))
      }

      for ((line, messages) <- lineMap) {
        val rhLine = mm.addLineHighlighter((line - 1).toInt, layer, null)
        val (color, icon) = {
          var p = 0
          for (msg <- messages) {
            msg.level match {
              case Level.Error if p <= 1 => p = 2
              case Level.Warning if p <= 0 => p = 1
              case _ =>
            }
          }
          p match {
            case 0 => (infoColor, infoIcon)
            case 1 => (warningColor, warningIcon)
            case 2 => (errorColor, errorIcon)
          }
        }
        rhLine.setThinErrorStripeMark(false)
        rhLine.setErrorStripeMarkColor(color)
        rhLine.setGutterIconRenderer(
          LogikaCheckAction.gutterIconRenderer(messages.map(_.text.value).mkString(tooltipSep),
            icon, emptyAction))
        rhs :+= rhLine
      }
      editor.putUserData(analysisDataKey, rhs)
    })

  def editorClosed(project: Project, file: VirtualFile): Unit = {}
}
