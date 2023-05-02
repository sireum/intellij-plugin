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

import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.{ApplicationManager, TransactionGuard}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.{EditorFontType, TextAttributesKey}
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.markup._
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor, TextEditor}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util._
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.openapi.wm.StatusBarWidget.{IconPresentation, WidgetPresentation}
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.openapi.wm.{StatusBar, StatusBarWidget, ToolWindowManager, WindowManager}
import com.intellij.ui.JBColor
import org.jetbrains.plugins.terminal.{ShellTerminalWidget, TerminalToolWindowManager}
import org.sireum.intellij.logika.LogikaConfigurable
import org.sireum.logika.{Smt2, Smt2Config, Smt2Invoke}
import org.sireum.message.Level
import org.sireum.server.protocol.Analysis

import java.awt.{Color, Font}
import java.util.concurrent._
import javax.swing.{DefaultListModel, Icon, JComponent, JMenu, JMenuItem, JPopupMenu, JSplitPane}

object SireumClient {

  object EditorEnabled

  class SireumStatusWidget extends StatusBarWidget {

    var statusTooltip: String = statusIdle
    var frame: Int = defaultFrame
    var component: JComponent = _
    var menu: JPopupMenu = _

    override def ID(): String = "Sireum"

    def clearCache(kind: Analysis.Cache.Kind.Type): Unit = {
      kind match {
        case Analysis.Cache.Kind.All => prevAnalysisData.clear()
        case _ =>
      }
      SireumClient.queue.add(Vector((true, org.sireum.server.protocol.JSON.fromRequest(
        org.sireum.server.protocol.Analysis.Cache.Clear(kind), true).value)))
    }

    override def install(statusBar: StatusBar): Unit = {
      component = statusBar.getComponent
      val shutdownItem = new JMenuItem("Shutdown Sireum server")
      shutdownItem.addActionListener { _ => shutdownServer()}
      val cacheMenu = new JMenu("Clear a specific cache")
      val fileItem = new JMenuItem("Files")
      fileItem.addActionListener { _ => clearCache(Analysis.Cache.Kind.Files) }
      cacheMenu.add(fileItem)
      val smt2Item = new JMenuItem("SMT2 Queries")
      smt2Item.addActionListener { _ => clearCache(Analysis.Cache.Kind.SMT2) }
      cacheMenu.add(smt2Item)
      val transitionItem = new JMenuItem("Transitions")
      transitionItem.addActionListener { _ => clearCache(Analysis.Cache.Kind.Transitions) }
      cacheMenu.add(transitionItem)
      val persistentItem = new JMenuItem("Persistent")
      persistentItem.addActionListener { _ => clearCache(Analysis.Cache.Kind.Persistent) }
      cacheMenu.add(persistentItem)
      menu = new JPopupMenu("Sireum")
      menu.add(cacheMenu)
      val allItem = new JMenuItem("Clear all Sireum caches")
      allItem.addActionListener { _ => clearCache(Analysis.Cache.Kind.All) }
      menu.add(allItem)
      menu.add(shutdownItem)
      menu.pack()
    }

    override def getPresentation: WidgetPresentation =
      new IconPresentation {
        override def getClickConsumer = e => {
          var found = false
          val wm = WindowManager.getInstance
          for (frame <- wm.getAllProjectFrames if !found) {
            val f = wm.getFrame(frame.getProject)
            if (f.isActive) {
              found = true
              val fc = frame.getComponent
              menu.show(e.getComponent, e.getX - menu.getPreferredSize.height, e.getY - menu.getPreferredSize.height)
            }
          }
        }

        override def getTooltipText: String = statusTooltip

        override def getIcon: Icon = icons(frame)
      }

    override def dispose(): Unit = {}

    def reset(): Unit = {
      statusTooltip = statusIdle
      frame = defaultFrame
    }
  }

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
  val gutterVerifiedIcon: Icon = IconLoader.getIcon("/icon/gutter-verified.png")
  val verifiedInfoIcon: Icon = IconLoader.getIcon("/icon/logika-verified-info.png")
  val sireumGrayIcon: Icon = IconLoader.getIcon("/icon/sireum-gray.png")
  val editorMap: scala.collection.mutable.Map[org.sireum.ISZ[org.sireum.String], (Project, VirtualFile, Editor, String, Boolean)] = scala.collection.mutable.Map()
  val sireumKey = new Key[EditorEnabled.type]("Sireum")
  val analysisDataKey = new Key[(scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]], DefaultListModel[Object], scala.collection.mutable.HashMap[Int, DefaultListModel[SummoningReportItem]], scala.collection.mutable.HashMap[Int, DefaultListModel[HintReportItem]], scala.collection.mutable.HashSet[Int])]("Analysis Data")
  val statusKey = new Key[Boolean]("Sireum Analysis Status")
  val reportItemKey = new Key[ReportItem]("Sireum Report Item")
  val coverageTextAttributes = new TextAttributes(null,
    createCoverageColor(LogikaConfigurable.coverageIntensity), null, EffectType.BOXED, Font.PLAIN)

  val statusIdle = "Sireum is idle"
  val statusWaiting = "Sireum is waiting to work"
  val statusWorking = "Sireum is working"
  val layer = 1000000
  val smt2SolverPrefix = "; Solver: "
  val smt2SolverArgsPrefix = "; Arguments: "
  val smt2SolverAndArgsPrefix = "; Solvers and arguments:"
  val smt2TabName = "Local"

  var request: Option[Request] = None
  var processInit: Option[(scala.sys.process.Process, org.sireum.Os.Path)] = None
  var dividerWeight: Double = .2
  var tooltipMessageOpt: Option[String] = None
  var tooltipBalloonOpt: Option[Balloon] = None
  val tooltipDefaultBgColor: Color = new Color(0xff, 0xff, 0xcc)
  val tooltipDarculaBgColor: Color = new Color(0x5c, 0x5c, 0x42)
  val maxTooltipLength: Int = 1024
  val singleExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor
  val statusRequest: Vector[(Boolean, String)] = Vector(
    (false, org.sireum.server.protocol.JSON.fromRequest(org.sireum.server.protocol.Status.Request(), true).value)
  )
  val terminateRequest: Vector[(Boolean, String)] = Vector(
    (true, org.sireum.server.protocol.JSON.fromRequest(org.sireum.server.protocol.Terminate(), true).value)
  )
  lazy val defaultFrame: Int = icons.length / 2 + 1
  lazy val statusBarWidget: SireumStatusWidget = new SireumStatusWidget
  val prevAnalysisData: ConcurrentHashMap[String, (scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]], scala.collection.mutable.HashMap[Int, DefaultListModel[SummoningReportItem]], scala.collection.mutable.HashMap[Int, DefaultListModel[HintReportItem]])] = new ConcurrentHashMap
  var usedMemory: org.sireum.Z = 0
  var shutdown: Boolean = false
  var queue: LinkedBlockingQueue[Vector[(Boolean, String)]] = new LinkedBlockingQueue
  var lastStatusUpdate: Long = System.currentTimeMillis

  def createCoverageColor(intensity: Int): JBColor =
    new JBColor(new Color(129, 62, 200, intensity), new Color(129, 62, 200, intensity))

  final case class Request(time: Long, requestId: org.sireum.ISZ[org.sireum.String],
                           project: Project, file: VirtualFile, editor: Editor,
                           input: String, msgGen: () => Vector[String], isInterprocedural: Boolean)

  def runLater[T](delayInMs: Int)(f: Runnable): Unit = singleExecutor.schedule(f, delayInMs, TimeUnit.MILLISECONDS)

  def shutdownServer(): Unit = editorMap.synchronized {
    this.synchronized {
      request = None
      editorMap.clear()
      queue.add(terminateRequest)
      queue = new LinkedBlockingQueue
      processInit.foreach(p => runLater(5000)(() => if (p._1.isAlive()) p._1.destroy()))
      processInit = None
      shutdown = true
      for (frame <- WindowManager.getInstance.getAllProjectFrames) {
        frame.getStatusBar.removeWidget(statusBarWidget.ID())
      }
    }
  }

  def logStackTrace(t: Throwable): Unit = {
    if (!SireumApplicationComponent.logging) {
      return
    }
    val sw = new _root_.java.io.StringWriter
    t.printStackTrace(new _root_.java.io.PrintWriter(sw))
    writeLog(isRequest = false, sw.toString)
  }

  def writeLog(isRequest: Boolean, content: String, full: Boolean = false): Unit = {
    if (!SireumApplicationComponent.logging) {
      return
    }
    processInit match {
      case Some((_, logFile)) =>
        if (content.isEmpty) return
        val maxSize = org.sireum.server.Server.maxLogLineSize.toInt
        if (logFile.size > org.sireum.server.Server.maxLogFileSize) logFile.writeOver("")
        logFile.writeAppend(s"${org.sireum.server.Server.Ext.timeStamp(isRequest)}${if (!full && content.length > maxSize) content.substring(0, maxSize) else content}${org.sireum.Os.lineSep}")
      case _ =>
    }
  }

  def init(p: Project): Unit = editorMap.synchronized {
    if (processInit.isEmpty) {
      statusBarWidget.reset()
      var serverArgs = Vector[String]("server", "--message", "json")
      if (!SireumApplicationComponent.cacheInput) {
        serverArgs = serverArgs :+ "--no-input-cache"
      }
      if (!SireumApplicationComponent.cacheType) {
        serverArgs = serverArgs :+ "--no-type-cache"
      }
      if (SireumApplicationComponent.logging) {
        serverArgs = serverArgs :+ "--log"
      }
      if (SireumApplicationComponent.verbose) {
        serverArgs = serverArgs :+ "--verbose"
      }
      val sireumHome = SireumApplicationComponent.getSireumHome(p) match {
        case Some(home) => home
        case _ => return
      }
      val logFile = sireumHome / ".client.log"
      logFile.removeAll()
      val command = SireumApplicationComponent.getCommand(sireumHome, serverArgs)
      queue = new LinkedBlockingQueue
      processInit = Some((
        SireumApplicationComponent.getSireumProcess(sireumHome, command,
          queue, { s =>
            val trimmed = s.trim
            var shouldLog = false
            var hasError = false

            def err(): Unit = {
              shouldLog = true
              hasError = true
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
                  case org.sireum.Either.Left(r) =>
                    r match {
                      case _: org.sireum.server.protocol.Status.Response =>
                      case _ => shouldLog = true
                    }
                    processResult(r)
                  case org.sireum.Either.Right(_) => err()
                }
              } catch {
                case _: Throwable => err()
              }
            } else {
              if (trimmed.nonEmpty) shouldLog = true
            }
            if (shouldLog) writeLog(isRequest = false, if (hasError) s"Error occurred when processing response: $s" else s)
          }),
        logFile
      ))
      writeLog(isRequest = false, s"Client v${PluginManager.getPlugin(PluginId.getId("org.sireum.intellij")).getVersion}: Started Sireum server ...")
      writeLog(isRequest = false, command.mkString(" ").replace(sireumHome.string.value, if (org.sireum.Os.isWin) "%SIREUM_HOME%" else "$SIREUM_HOME"))
      if (processInit.isEmpty) return

      def memory: String =
        if (usedMemory > 1024 * 1024 * 1024) f"${usedMemory.toLong / 1024d / 1024d / 1024d}%.2f GB"
        else f"${usedMemory.toLong / 1024d / 1024d}%.2f MB"

      def statusText(status: String): String =
        s"$status${if (usedMemory =!= 0) s" ($memory)" else ""}<br>[click to shutdown or<br>clear caches]"

      shutdown = false
      val id = statusBarWidget.ID()
      for (frame <- WindowManager.getInstance.getAllProjectFrames) {
        val statusBar = frame.getStatusBar
        if (statusBar.getWidget(id) == null) {
          statusBar.addWidget(statusBarWidget)
        }
        statusBar.updateWidget(id)
      }

      def updateWidget(): Unit = {
        val id = statusBarWidget.ID()
        for (frame <- WindowManager.getInstance.getAllProjectFrames) {
          frame.getStatusBar.updateWidget(id)
        }
      }

      val t = new Thread {
        override def run(): Unit = {
          var idle = false
          while (!shutdown) {
            if (editorMap.nonEmpty || request.nonEmpty) {
              idle = false
              statusBarWidget.frame = (statusBarWidget.frame + 1) % icons.length
              statusBarWidget.statusTooltip = statusText(if (editorMap.nonEmpty) statusWorking else statusWaiting)
              updateWidget()
            } else {
              if (!idle) {
                idle = true
                val f = statusBarWidget.frame
                statusBarWidget.frame = defaultFrame
                statusBarWidget.statusTooltip = statusText(statusIdle)
                if (f != defaultFrame) updateWidget()
              }
              if (System.currentTimeMillis - lastStatusUpdate > 60000) {
                lastStatusUpdate = System.currentTimeMillis
                queue.add(statusRequest)
              }
            }
            this.synchronized {
              request match {
                case Some(r: Request) =>
                  if (System.currentTimeMillis - r.time > SireumApplicationComponent.idle) {
                    request = None
                    editorMap.synchronized {
                      editorMap(r.requestId) = (r.project, r.file, r.editor, r.input, r.isInterprocedural)
                    }
                    queue.add(for (m <- r.msgGen()) yield (true, m))
                  }
                case None =>
              }
            }
            Thread.sleep(175)
          }
        }
      }
      t.setDaemon(true)
      t.start()
    }
  }

  def isEnabled(editor: Editor): Boolean = EditorEnabled == editor.getUserData(sireumKey)

  def addRequest(reqsF: org.sireum.ISZ[org.sireum.String] => Vector[org.sireum.server.protocol.Request],
                 project: Project, file: VirtualFile, editor: Editor, isBackground: Boolean, input: String,
                 isInterprocedural: Boolean): Unit = {
    if (Util.getPath(file).isEmpty) {
      return
    }
    Util.async { () =>
      init(project)
      val (t, requestId) = {
        val t = System.currentTimeMillis
        val id = org.sireum.ISZ(org.sireum.String(t.toString))
        (t, id)
      }

      editorMap.synchronized {
        val cancels = for (rid <- editorMap.keys.toVector) yield {
          editorMap -= rid
          (true, org.sireum.server.protocol.JSON.fromRequest(org.sireum.server.protocol.Cancel(rid), true).value)
        }
        if (cancels.nonEmpty) queue.add(cancels)
      }

      def f(): Vector[String] = for (req <- reqsF(requestId)) yield org.sireum.server.protocol.JSON.fromRequest(req, true).value

      if (isBackground) {
        this.synchronized {
          request = Some(Request(t, requestId, project, file, editor, input, f, isInterprocedural))
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
          editorMap(requestId) = (project, file, editor, input, isInterprocedural)
        }
        queue.add(for (m <- f()) yield (true, m))
      }
    }
  }

  def getSmt2Configs(project: Project): org.sireum.ISZ[Smt2Config] = SireumApplicationComponent.getSireumHome(project) match {
    case Some(sireumHome) if Util.isLogikaSupportedPlatform =>
      val nameExePathMap = Smt2Invoke.nameExePathMap(sireumHome)
      Smt2.parseConfigs(nameExePathMap, false, LogikaConfigurable.smt2ValidOpts, LogikaConfigurable.timeout, LogikaConfigurable.rlimit).left ++
        Smt2.parseConfigs(nameExePathMap, true, LogikaConfigurable.smt2SatOpts, LogikaConfigurable.timeout, LogikaConfigurable.rlimit).left
    case _ => org.sireum.ISZ()
  }

  def analyze(project: Project, file: VirtualFile, editor: Editor, line: Int,
              ofiles: org.sireum.HashSMap[org.sireum.String, org.sireum.String],
              isBackground: Boolean, isInterprocedural: Boolean, disallowTransitionCaching: Boolean = false,
              typeCheckOnly: Boolean = false): Unit = {
    if (editor.isDisposed || !isEnabled(editor)) return
    val input = editor.getDocument.getText

    def f(requestId: org.sireum.ISZ[org.sireum.String]): Vector[org.sireum.server.protocol.Request] = {
      import org.sireum.server.protocol._
      val p = Util.getPath(file) match {
        case Some(path) => path
        case _ => return Vector()
      }
      Vector(
        Logika.Verify.Config(LogikaConfigurable.hint, LogikaConfigurable.inscribeSummonings, LogikaConfigurable.infoFlow,
          org.sireum.server.service.AnalysisService.defaultConfig(
            parCores = if (isBackground) SireumApplicationComponent.bgCores else SireumApplicationComponent.maxCores,
            sat = LogikaConfigurable.checkSat,
            timeoutInMs = LogikaConfigurable.timeout,
            useReal = LogikaConfigurable.useReal,
            fpRoundingMode = LogikaConfigurable.fpRoundingMode,
            caching = LogikaConfigurable.smt2Cache,
            simplifiedQuery = LogikaConfigurable.smt2Simplify,
            smt2Configs = getSmt2Configs(project),
            branchPar = LogikaConfigurable.branchPar,
            branchParCores = LogikaConfigurable.branchParCores,
            splitIf = !LogikaConfigurable.infoFlow && LogikaConfigurable.splitConds,
            splitMatch = !LogikaConfigurable.infoFlow && LogikaConfigurable.splitMatchCases,
            splitContract = !LogikaConfigurable.infoFlow && LogikaConfigurable.splitContractCases,
            atLinesFresh = LogikaConfigurable.hintLinesFresh,
            interp = isInterprocedural,
            loopBound = LogikaConfigurable.loopBound,
            callBound = LogikaConfigurable.callBound,
            interpContracts = LogikaConfigurable.interpContracts,
            rawInscription = LogikaConfigurable.rawInscription,
            elideEncoding = LogikaConfigurable.elideEncoding,
            flipStrictPure = LogikaConfigurable.flipStrictPure,
            transitionCache = LogikaConfigurable.transitionCache && !disallowTransitionCaching
          )),
        if (!(org.sireum.Os.path(project.getBasePath) / "bin" / "project.cmd").exists || p.ext.value == "sc" || p.ext.value == "cmd") {
          Slang.Check.Script(
            isBackground = isBackground,
            logikaEnabled = !typeCheckOnly && Util.isLogikaSupportedPlatform && (!isBackground ||
              (SireumApplicationComponent.backgroundAnalysis != 0 && LogikaConfigurable.backgroundAnalysis)),
            id = requestId,
            uriOpt = org.sireum.Some(org.sireum.String(file.toNioPath.toUri.toASCIIString)),
            content = input,
            line = line
          )
        } else {
          var files = ofiles
          var vfiles = org.sireum.ISZ[org.sireum.String]()
          try {
            if (!typeCheckOnly) {
              val content = editor.getDocument.getText
              val (hasSireum, compactFirstLine, _) = org.sireum.lang.parser.SlangParser.detectSlang(org.sireum.Some(p.toUri), content)
              if (hasSireum) {
                files = files + p.string ~> content
                if (Util.isLogikaSupportedPlatform && (!isBackground ||
                  (SireumApplicationComponent.backgroundAnalysis != 0 && LogikaConfigurable.backgroundAnalysis)) &&
                  (compactFirstLine.contains("#Logika") || isInterprocedural)) {
                  vfiles = vfiles :+ p.string
                }
              }
            }
          } catch {
            case t: Throwable =>
              logStackTrace(t)
              return Vector()
          }
          Slang.Check.Project(
            isBackground = isBackground,
            id = requestId,
            proyek = org.sireum.Os.path(project.getBasePath).string,
            files = files,
            vfiles = vfiles,
            line = line
          )
        }
      )
    }

    addRequest(f, project, file, editor, isBackground, input, isInterprocedural)
  }

  def analyzeOpt(project: Project, file: VirtualFile, editor: Editor, line: Int,
                 ofiles: org.sireum.HashSMap[org.sireum.String, org.sireum.String], isBackground: Boolean): Unit = {
    val pOpt = Util.getPath(file)
    if (pOpt.isEmpty) {
      return
    }
    val (isSireum, isLogika) = Util.isSireumOrLogikaFile(pOpt.get)(org.sireum.String(editor.getDocument.getText))
    if (isLogika) {
      enableEditor(project, file, editor)
      if (SireumApplicationComponent.backgroundAnalysis != 0)
        analyze(project, file, editor, line, ofiles, isBackground = isBackground, isInterprocedural = false)
    } else if (isSireum) {
      enableEditor(project, file, editor)
      if (SireumApplicationComponent.backgroundAnalysis != 0)
        analyze(project, file, editor, line, ofiles, isBackground = isBackground, isInterprocedural = false)
    }
  }

  def getModifiedFiles(project: Project, file: VirtualFile): org.sireum.HashSMap[org.sireum.String, org.sireum.String] = {
    var r = org.sireum.HashSMap.empty[org.sireum.String, org.sireum.String]
    val pOpt = Util.getPath(file)
    if (pOpt.isEmpty) return r
    if (pOpt.get.ext === ".sc") return r
    for (fileEditor <- FileEditorManager.getInstance(project).getAllEditors if fileEditor.isModified) scala.util.Try {
      val pathOpt = Util.getPath(fileEditor.getFile)
      fileEditor match {
        case fileEditor: TextEditor if pathOpt.nonEmpty =>
          val path = pathOpt.get
          val e = fileEditor.getEditor
          if (path.ext.value == "scala") {
            val content = e.getDocument.getText
            if (org.sireum.lang.parser.SlangParser.detectSlang(org.sireum.Some(path.toUri), content)._1) {
              r = r + path.string ~> content
            }
          }
        case _ =>
      }
    }
    return r
  }

  def getCurrentLine(editor: Editor): Int = {
    val offset = editor.getCaretModel.getPrimaryCaret.getOffset
    return editor.getDocument.getLineNumber(offset) + 1
  }

  def enableEditor(project: Project, file: VirtualFile, editor: Editor): Unit = {
    if (editor.getUserData(sireumKey) != null) return
    editor.putUserData(sireumKey, EditorEnabled)
    editor.getDocument.addDocumentListener(new DocumentListener {
      override def documentChanged(event: DocumentEvent): Unit =
        if (SireumApplicationComponent.backgroundAnalysis == 2 && !project.isDisposed && !editor.isDisposed) {
          analyzeOpt(project, file, editor, getCurrentLine(editor), getModifiedFiles(project, file), isBackground = true)
        }

      override def beforeDocumentChange(event: DocumentEvent): Unit = {}
    })
  }

  def editorClosed(project: Project): Unit = {
    resetSireumView(project, None)
  }

  def editorOpened(project: Project, file: VirtualFile, editor: Editor): Unit = {
    if (processInit.nonEmpty) {
      ApplicationManager.getApplication.invokeLater { () =>
        val statusBar = WindowManager.getInstance.getStatusBar(project)
        val id = statusBarWidget.ID()
        if (statusBar.getWidget(id) == null) {
          statusBar.addWidget(statusBarWidget)
        }
        statusBar.updateWidget(id)
      }
    }
    analyzeOpt(project, file, editor, 0, getModifiedFiles(project, file), isBackground =
      !(SireumApplicationComponent.backgroundAnalysis != 0 && LogikaConfigurable.backgroundAnalysis))
  }

  val sireumServerTitle = "Sireum Server"

  val slangErrorTitle = "Slang Error"

  val logikaErrorTitle = "Logika Error"

  val logikaWarningTitle = "Logika Warning"

  val logikaVerifiedTitle = "Logika Verified"

  val internalErrorTitle = "Internal Error"

  def notifyHelper(projectOpt: Option[Project], editorOpt: Option[Editor],
                   r: org.sireum.server.protocol.Response): Unit = {
    import org.sireum.message.Level
    import org.sireum.server.protocol._
    val project = projectOpt.orNull
    val statusOpt = editorOpt.map(_.getUserData(statusKey))
    r match {
      case r: Analysis.End =>
        if (r.numOfErrors > 0 || r.numOfInternalErrors > 0) {
          if (!r.isBackground || statusOpt.getOrElse(true)) {
            if (r.hasLogika) {
              if (r.isIllFormed) {
                Util.notify(new Notification(
                  groupId, slangErrorTitle,
                  s"Ill-formed program with ${r.numOfErrors} error(s)",
                  NotificationType.ERROR), project, shouldExpire = true)
              }
              else {
                Util.notify(new Notification(
                  groupId, logikaErrorTitle,
                  s"Programming logic proof is rejected with ${r.numOfErrors} error(s)",
                  NotificationType.ERROR), project, shouldExpire = true)
              }
            }
          }
          editorOpt.foreach(_.putUserData(statusKey, false))
        } else if (r.hasLogika && r.numOfWarnings > 0) {
          Util.notify(new Notification(
            groupId, logikaWarningTitle,
            s"Programming logic proof is accepted with ${r.numOfWarnings} warning(s)",
            NotificationType.WARNING, null), project, shouldExpire = true)
          editorOpt.foreach(_.putUserData(statusKey, true))
        } else if (!r.wasCancelled) {
          val icon = verifiedInfoIcon
          if (r.hasLogika && (!r.isBackground || !(statusOpt.getOrElse(false)))) {
            Util.notify(new Notification(groupId, logikaVerifiedTitle, "Programming logic proof is accepted",
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
              groupId, internalErrorTitle,
              r.message.text.value,
              NotificationType.ERROR), project, shouldExpire = false)
            editorOpt.foreach(_.putUserData(statusKey, false))
          case _ =>
        }
      case _ =>
    }
  }

  private[intellij] sealed trait ReportItem

  private[intellij] object CoverageReportItem extends ReportItem

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

  private[intellij] final case class HintReportItem(kindOpt: Option[org.sireum.server.protocol.Logika.Verify.Info.Kind.Type],
                                                    project: Project,
                                                    file: VirtualFile,
                                                    messageHeader: String,
                                                    offset: Int,
                                                    val message: String,
                                                    terminated: Boolean) extends ReportItem {
    override def toString: String = messageHeader
  }

  private[intellij] final case class SummoningReportItem(project: Project,
                                                         file: VirtualFile,
                                                         messageHeader: String,
                                                         info: String,
                                                         offset: Int,
                                                         val message: String) extends ReportItem {
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
            writeLog(isRequest = false, msg.text.value, full = true)
            return Some((line, ConsoleReportItem(iproject, file, Level.Error, line, column, offset, length, msg.text.value)))
          case Level.Error =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Error, line, column, offset, length, msg.text.value)))
          case Level.Warning =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Warning, line, column, offset, length, msg.text.value)))
          case Level.Info =>
            return Some((line, ConsoleReportItem(iproject, file, Level.Info, line, column, offset, length, msg.text.value)))
        }
      case r: Logika.Verify.Smt2Query =>
        val text: Predef.String = r.query.value
        val line = r.pos.beginLine.toInt
        val offset = r.pos.offset.toInt
        val header = r.info.value.lines().limit(2).map(line => line.replace(';', ' ').
          replace("Result:", "").trim).toArray.mkString(": ")
        return Some((line, SummoningReportItem(iproject, file, header, r.info.value, offset, text)))
      case r: Logika.Verify.State =>
        import org.sireum._
        val text = normalizeChars(r.claims.value)
        val pos = r.posOpt.get
        val line = pos.beginLine.toInt
        val offset = pos.offset.toInt
        val header = {
          val labels = org.sireum.ISZ[String](
            if (r.terminated) s"Post-state at line $line" else s"Pre-state at line $line"
          ) ++ r.labels
          labels.elements.reverse.mkString(" / ")
        }
        return scala.Some((line, HintReportItem(scala.None, iproject, file, header, offset, text, r.terminated)))
      case r: Logika.Verify.Info =>
        val pos = r.pos
        val line = pos.beginLine.toInt
        val offset = pos.offset.toInt
        val text = r.message.value
        val header = {
          val i = text.indexOf('\n')
          var firstLine = if (i >= 0) text.substring(0, i) else text
          if (firstLine.length >= 100) {
            firstLine = firstLine.substring(0, 100) + " ..."
          }
          s"Info: $firstLine"
        }
        return scala.Some((line, HintReportItem(Some(r.kind), iproject, file, header, offset, text, false)))

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
        case '≡' => sb.append("===")
        case '≢' => sb.append("=!=")
        case _ => sb.append(c)
      }
      i += 1
    }
    sb.toString
  }

  def resetSireumView(project: Project, editorOpt: Option[Editor]): Unit = {
    import javax.swing.event.{DocumentEvent, DocumentListener}
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
                val content: Predef.String = if (sri.message.startsWith(";")) sri.message else {
                  val p = org.sireum.Os.path(sri.message)
                  try p.read.value catch {
                    case _: Throwable => sri.info
                  }
                }
                f.logika.logikaToolSplitPane.setDividerLocation(dividerWeight)
                f.logika.logikaTextArea.setText(normalizeChars(content))
                f.logika.logikaTextArea.setCaretPosition(0)
                f.logika.logikaToolTextField.getDocument.putProperty("Logika", f.logika.logikaTextArea.getText)
                f.logika.logikaToolTextField.setPlaceholder("Search ...")
                f.logika.logikaToolTextField.setText("")
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
                f.logika.logikaToolSplitPane.setDividerLocation(if (list.getModel.getSize <= 1) 0 else dividerWeight)
                var content = normalizeChars(hri.message)
                if (LogikaConfigurable.hintMaxColumn > 0) {
                  org.sireum.Scalafmt.format(
                    s"${org.sireum.Scalafmt.minimalConfig}\nmaxColumn = ${LogikaConfigurable.hintMaxColumn}", true,
                    content
                  ) match {
                    case org.sireum.Some(r) => content = r.value
                    case _ =>
                  }
                }
                f.logika.logikaTextArea.setText(content)
                f.logika.logikaToolTextField.getDocument.putProperty("Logika", f.logika.logikaTextArea.getText)
                f.logika.logikaToolTextField.setPlaceholder("Filter claims ...")
                f.logika.logikaToolTextField.setText("")
                f.logika.logikaTextArea.setCaretPosition(f.logika.logikaTextArea.getDocument.getLength)
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

  def addLineHighlighter(mm: MarkupModel, line: Int, layer: Int, ta: TextAttributes = null): RangeHighlighter = {
    val max = mm.getDocument.getLineCount
    val l = if (line < 0) 0 else if (line >= max) max - 1 else line
    mm.addLineHighlighter(l, layer, ta)
  }

  def processResult(r: org.sireum.server.protocol.Response): Unit = {
    def getProjectFileEditorInput(pe: (Project, VirtualFile, Editor, String, Boolean)): Option[(Project, VirtualFile, Editor, String)] = {
      r.posOpt match {
        case org.sireum.Some(pos) if pos.uriOpt.nonEmpty && !pe._1.isDisposed =>
          val uri = pos.uriOpt.get
          val pOpt = Util.getPath(pe._2)
          pOpt match {
            case Some(p) =>
              if (uri == p.toUri) return Some((pe._1, pe._2, pe._3, pe._4))
              val project = pe._1
              for (fileEditor <- FileEditorManager.getInstance(project).getAllEditors) {
                val pOpt2 = Util.getPath(fileEditor.getFile)
                pOpt2 match {
                  case Some(p2) =>
                    fileEditor match {
                      case fileEditor: TextEditor if p2.toUri == uri =>
                        val editor = fileEditor.getEditor
                        return Some((project, fileEditor.getFile, editor, editor.getDocument.getText))
                      case _ =>
                    }
                  case _ =>
                }
              }
              if (pe._5) {
                val file = LocalFileSystem.getInstance.findFileByPath(org.sireum.Os.uriToPath(uri).string.value)
                val offset = pos.offset.toInt
                val editor = FileEditorManager.getInstance(project).openTextEditor(
                  if (offset >= 1) new OpenFileDescriptor(project, file, offset)
                  else new OpenFileDescriptor(project, file),
                  true)
                return if (editor != null) Some((project, file, editor, editor.getDocument.getText)) else None
              } else {
                return None
              }
            case _ =>
              return None
          }
        case _ => return Some((pe._1, pe._2, pe._3, pe._4))
      }
    }

    def consoleReportItems(listModel: DefaultListModel[Object],
                           rhs: scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]],
                           editor: Editor,
                           ci: ConsoleReportItem,
                           line: Int): Unit = {
      val tooltipSep = "<hr>"
      val cs = editor.getColorsScheme
      var level = ci.level
      val mm = editor.getMarkupModel
      val project = editor.getProject
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
      val rhLine = addLineHighlighter(mm,line - 1, layer)
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
        rhs.put(line, rhl :+ rhLine :+ rh)
      } else {
        rhs.put(line, rhl :+ rhLine)
      }
    }

    def hintReportItem(listModelMap: scala.collection.mutable.HashMap[Int, DefaultListModel[HintReportItem]],
                       rhs: scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]],
                       editor: Editor,
                       ri: HintReportItem,
                       line: Int): Unit = {
      val mm = editor.getMarkupModel
      val project = editor.getProject
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
      val hintListModel = listModelMap.get(line) match {
        case scala.Some(l) => l
        case _ =>
          val l = new DefaultListModel[HintReportItem]()
          listModelMap.put(line, l)
          l
      }
      hintListModel.addElement(ri)
      val rhLine = addLineHighlighter(mm,line - 1, layer)
      rhLine.putUserData(reportItemKey, ri)
      rhLine.setThinErrorStripeMark(false)
      val (title, icon) = ri.kindOpt match {
        case scala.Some(org.sireum.server.protocol.Logika.Verify.Info.Kind.Verified) =>
          ("Click to show verification report", gutterVerifiedIcon)
        case _ => ("Click to show some hints", gutterHintIcon)
      }
      rhLine.setGutterIconRenderer(gutterIconRenderer(
        title, icon, _ => sireumToolWindowFactory(project, f => {
          val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
          tw.activate(() => {
            val list = f.logika.logikaList
            list.synchronized {
              list.setModel(hintListModel.asInstanceOf[DefaultListModel[Object]])
              list.setSelectedIndex(0)
            }
            tw.getContentManager.setSelectedContent(tw.getContentManager.findContent("Output"))
          })
        })
      ))
      rhs.put(line, rhl :+ rhLine)
    }

    def summoningReportItem(listModelMap: scala.collection.mutable.HashMap[Int, DefaultListModel[SummoningReportItem]],
                            rhs: scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]],
                            editor: Editor,
                            ri: SummoningReportItem,
                            line: Int): Unit = {
      val mm = editor.getMarkupModel
      val project = editor.getProject
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
      val summoningListModel = listModelMap.get(line) match {
        case scala.Some(l) => l
        case _ =>
          val l = new DefaultListModel[SummoningReportItem]()
          listModelMap.put(line, l)
          l
      }
      summoningListModel.addElement(ri)
      val rhLine = addLineHighlighter(mm, line - 1, layer)
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
      rhs.put(line, rhl :+ rhLine)
    }

    r match {
      case r: org.sireum.server.protocol.Status.Response =>
        lastStatusUpdate = System.currentTimeMillis
        usedMemory = r.totalMemory - r.freeMemory
      case _: org.sireum.server.protocol.Version.Response =>
      case r: org.sireum.server.protocol.Analysis.Cache.Cleared =>
        Util.notify(new Notification(groupId, sireumServerTitle,
          r.msg.value,NotificationType.INFORMATION), null, shouldExpire = true)
      case _ =>
        def processResultH(): Unit = {
          def clearEditorH(editor: Editor): Unit = {
            if (!editor.isDisposed) {
              val mm = editor.getMarkupModel
              analysisDataKey.synchronized {
                val q = editor.getUserData(analysisDataKey)
                if (q != null) {
                  for (rh <- mm.getAllHighlighters if rh.getUserData(reportItemKey) != null) {
                    mm.removeHighlighter(rh)
                  }
                  val t = prevAnalysisData.get(editor.getVirtualFile.getCanonicalPath)
                  if (t != null) {
                    t._1.addAll(q._1)
                    t._2.addAll(q._3)
                    t._3.addAll(q._4)
                  }
                  editor.putUserData(analysisDataKey, null)
                }
              }
            }
          }

          def clearProgramMarkings(): Unit = {
            r match {
              case _: org.sireum.server.protocol.Analysis.Start =>
                for (project <- ProjectManager.getInstance.getOpenProjects) {
                  for (fileEditor <- FileEditorManager.getInstance(project).getAllEditors) {
                    fileEditor match {
                      case fileEditor: TextEditor =>
                        val path = fileEditor.getFile.getPath
                        if (path.endsWith(".scala") || path.endsWith(".slang")) {
                          clearEditorH(fileEditor.getEditor)
                        }
                      case _ =>
                    }
                  }
                }
              case _ =>
            }
          }

          def clearScriptMarkings(editor: Editor): Unit = {
            r match {
              case _: org.sireum.server.protocol.Analysis.Start => clearEditorH(editor)
              case _ =>
            }
          }

          val (project, file, editor, input) = editorMap.synchronized {
            editorMap.get(r.id) match {
              case Some(pe) =>
                val path = pe._2.getPath
                if (path.endsWith(".scala") || path.endsWith(".slang")) {
                  clearProgramMarkings()
                } else {
                  clearScriptMarkings(pe._3)
                }
                r match {
                  case r: org.sireum.server.protocol.Analysis.End => editorMap -= r.id
                  case r: org.sireum.server.protocol.Slang.Rewrite.Response => editorMap -= r.id
                  case _: org.sireum.server.protocol.Analysis.Start =>
                    if (!pe._1.isDisposed && !pe._3.isDisposed) {
                      resetSireumView(pe._1, Some(pe._3))
                      sireumToolWindowFactory(pe._1, f => {
                        f.logika.logikaTextArea.setFont(
                          pe._3.getColorsScheme.getFont(EditorFontType.PLAIN))
                        f.logika.logikaTextArea.setText("")
                      })
                    }
                  case r: org.sireum.server.protocol.Report if r.message.level == Level.InternalError =>
                    notifyHelper(Some(pe._1), if (pe._3.isDisposed) None else Some(pe._3), r)
                  case _ =>
                }
                getProjectFileEditorInput(pe) match {
                  case Some(v) if !pe._3.isDisposed => v
                  case _ =>
                    writeLog(isRequest = false, s"There is no opened editor for response: $r")
                    return
                }
              case _ =>
                clearProgramMarkings()
                notifyHelper(None, None, r)
                return
            }
          }
          val (rhs, listModel, summoningListModelMap, hintListModelMap, coverageLines, prevRhs, prevSm, prevHm) =
            analysisDataKey.synchronized {
              r match {
                case r: org.sireum.server.protocol.Analysis.End =>
                  notifyHelper(Some(project), Some(editor), r)
                  return
                case r: org.sireum.server.protocol.Slang.Rewrite.Response =>
                  SireumOnlyAction.processSlangRewriteResponse(r, project, editor)
                  return
                case _ =>
                  var q = editor.getUserData(analysisDataKey)
                  if (q == null) {
                    q = (
                      scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]](),
                      new DefaultListModel[Object](),
                      scala.collection.mutable.HashMap[Int, DefaultListModel[SummoningReportItem]](),
                      scala.collection.mutable.HashMap[Int, DefaultListModel[HintReportItem]](),
                      scala.collection.mutable.HashSet[Int]()
                    )
                    editor.putUserData(analysisDataKey, q)
                  }
                  var t = prevAnalysisData.get(editor.getVirtualFile.getCanonicalPath)
                  if (t == null) {
                    t = (
                      scala.collection.mutable.HashMap[Int, Vector[RangeHighlighter]](),
                      scala.collection.mutable.HashMap[Int, DefaultListModel[SummoningReportItem]](),
                      scala.collection.mutable.HashMap[Int, DefaultListModel[HintReportItem]]()
                    )
                    prevAnalysisData.put(editor.getVirtualFile.getCanonicalPath, t)
                  }
                  (q._1, q._2, q._3, q._4, q._5, t._1, t._2, t._3)
              }
            }
          if (input != editor.getDocument.getText) {
            writeLog(isRequest = false, s"Stale response: $r")
            return
          }
          r match {
            case r: org.sireum.server.protocol.Analysis.Coverage => try {
              val mm = editor.getMarkupModel
              for (i <- r.pos.beginLine to r.pos.endLine if !coverageLines.contains(i.toInt)) {
                val line = i.toInt
                if (LogikaConfigurable.coverage) {
                  val rh = addLineHighlighter(mm, line - 1, -1, coverageTextAttributes)
                  rh.putUserData(reportItemKey, CoverageReportItem)
                  coverageLines.add(line)
                }
                if (r.cached) {
                  prevRhs.get(line) match {
                    case Some(rhs) =>
                      for (rh <- rhs) {
                        val ri = rh.getUserData(reportItemKey)
                        val skip: Boolean = ri match {
                          case ri: ConsoleReportItem => ri.level == org.sireum.message.Level.InternalError ||
                            ri.level == org.sireum.message.Level.Error
                          case _ => false
                        }
                        if (!skip) {
                          val newRh = mm.addLineHighlighter(line - 1, rh.getLayer, rh.getTextAttributes(null))
                          newRh.putUserData(reportItemKey, ri)
                        }
                      }
                    case _ =>
                  }
                  prevSm.get(line) match {
                    case Some(sm) =>
                      for (i <- 0 until sm.size()) {
                        val ri = sm.elementAt(i)
                        summoningReportItem(summoningListModelMap, rhs, editor,
                          ri.copy(message = ri.message.replace("Result:", "Result (Cached):")), line)
                      }
                    case _ =>
                  }
                  prevHm.get(line) match {
                    case Some(hm) =>
                      for (i <- 0 until hm.size) {
                        val ri = hm.elementAt(i)
                        if (!ri.terminated) {
                          hintReportItem(hintListModelMap, rhs, editor,
                            if (!ri.message.endsWith("Cached")) ri.copy(message = ri.message + "\n// Cached") else ri, line)
                        }
                      }
                    case _ =>
                  }
                }
              }
            } catch {
              case t: Throwable => logStackTrace(t)
            }
            case _ =>
              for ((line, ri) <- processReport(project, file, r)) try {
                ri match {
                  case ri: ConsoleReportItem => consoleReportItems(listModel, rhs, editor, ri, line)
                  case ri: HintReportItem => hintReportItem(hintListModelMap, rhs, editor, ri, line)
                  case ri: SummoningReportItem => summoningReportItem(summoningListModelMap, rhs, editor, ri, line)
                }
              } catch {
                case t: Throwable => logStackTrace(t)
              }
          }
        }

        ApplicationManager.getApplication.invokeLater(() => processResultH())
    }
  }

  def launchSMT2Solver(project: Project, editor: Editor): Unit = {
    def execute(command: String): Unit = ApplicationManager.getApplication.invokeLater(() => {
      val ttwm = TerminalToolWindowManager.getInstance(project)
      var window = ttwm.getToolWindow
      if (window == null) {
        window = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
      }
      window.activate(null)
      val content = window.getContentManager.findContent(smt2TabName)
      val widget = if (content == null) ttwm.createLocalShellWidget(project.getBasePath, smt2TabName)
      else TerminalToolWindowManager.getWidgetByContent(content).asInstanceOf[ShellTerminalWidget]
      widget.setAutoscrolls(true)
      widget.requestFocus()
      widget.executeCommand(command.replaceAll(" -in ", " "))
    })
    if (editor != null) Util.getPath(editor.getVirtualFile) match {
      case Some(path) =>
        val text = editor.getDocument.getText
        val solverIndex = text.indexOf(smt2SolverPrefix)
        if (solverIndex >= 0) {
          val solverArgumentsIndex = text.indexOf(smt2SolverArgsPrefix, solverIndex)
          if (solverArgumentsIndex >= 0) {
            val solverPath = text.substring(solverIndex + smt2SolverPrefix.length, text.indexOf('\n', solverIndex)).trim
            val solverArguments = text.substring(solverArgumentsIndex + smt2SolverArgsPrefix.length,
              text.indexOf('\n', solverArgumentsIndex)).trim
            execute(s""""$solverPath" $solverArguments "${path.string.value}"""")
          }
        } else {
          var solverArgumentsIndex = text.indexOf(smt2SolverAndArgsPrefix, solverIndex)
          while (solverArgumentsIndex >= 0) {
            solverArgumentsIndex = text.indexOf("; *", solverArgumentsIndex)
            solverArgumentsIndex = text.indexOf(": ", solverArgumentsIndex)
            val n = text.indexOf('\n', solverArgumentsIndex)
            val Array(solverPath, solverArguments) = text.substring(solverArgumentsIndex + 2, n).split(',')
            execute(s""""${solverPath.trim}" ${solverArguments.trim} "${path.string.value}"""")
            solverArgumentsIndex = text.indexOf("; *", n)
          }
        }
      case _ =>
    }
  }
}
