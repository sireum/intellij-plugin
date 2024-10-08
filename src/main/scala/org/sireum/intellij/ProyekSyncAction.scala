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

import com.intellij.configurationStore.StoreReloadManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.{KillableColoredProcessHandler, ProcessEvent, ProcessListener}
import com.intellij.notification.{Notification, NotificationType}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.{ProjectManager, Project => IProject}
import com.intellij.openapi.util.Key

import java.nio.charset.Charset
import javax.swing.event.HyperlinkEvent

object ProyekSyncAction {
  def sync(ip: IProject): Unit = {
    var iproject = ip
    val basePath = ip.getBasePath
    StoreReloadManager.Companion.getInstance(ip).reloadProject()
    iproject = null
    ApplicationManager.getApplication.invokeLater(() => {
      while (iproject == null) {
        for (p <- ProjectManager.getInstance().getOpenProjects if p.getBasePath == basePath) {
          iproject = p
        }
        try {
          Thread.sleep(200)
        } catch {
          case _: Throwable =>
        }
      }
      import org.sireum._
      SireumApplicationComponent.getSireumHome(ip) match {
        case scala.Some(home) =>
          ProgressManager.getInstance().run(new Task.Backgroundable(iproject, "Proyek") {
            override def run(indicator: ProgressIndicator): Unit = {
              val srm: StoreReloadManager = StoreReloadManager.Companion.getInstance(iproject)
              val baseDir = iproject.getBaseDir
              try {
                srm.blockReloadingProjectOnExternalChanges()
                srm.scheduleProcessingChangedFiles()
                indicator.setIndeterminate(true)
                indicator.setText("Importing project ...")
                val cmds = new java.util.ArrayList[Predef.String]
                if (Os.isWin) {
                  cmds.add("cmd")
                  cmds.add("/c")
                  cmds.add("sireum.bat")
                } else {
                  cmds.add("./sireum")
                }
                cmds.add("proyek")
                cmds.add("ive")
                cmds.add("--force")
                if (SireumApplicationComponent.proxyHost.nonEmpty) {
                  cmds.add("--proxy-host")
                  cmds.add(SireumApplicationComponent.proxyHost)
                }
                if (SireumApplicationComponent.proxyPort.nonEmpty) {
                  cmds.add("--proxy-port")
                  cmds.add(SireumApplicationComponent.proxyPort)
                }
                if (SireumApplicationComponent.proxyUserEnvVar.nonEmpty) {
                  cmds.add("--proxy-user-env")
                  cmds.add(SireumApplicationComponent.proxyUserEnvVar)
                }
                if (SireumApplicationComponent.proxyPasswdEnvVar.nonEmpty) {
                  cmds.add("--proxy-passwd-env")
                  cmds.add(SireumApplicationComponent.proxyPasswdEnvVar)
                }
                if (SireumApplicationComponent.proxyNonHosts.nonEmpty) {
                  cmds.add("--proxy-non-hosts")
                  cmds.add(SireumApplicationComponent.proxyNonHosts)
                }
                cmds.add(iproject.getBasePath)
                val generalCommandLine = new GeneralCommandLine(cmds)
                generalCommandLine.setWorkDirectory((home / "bin").string.value)
                generalCommandLine.setCharset(Charset.forName("UTF-8"))
                val env = new java.util.HashMap[java.lang.String, java.lang.String]()
                env.put("SIREUM_HOME", home.string.value)
                org.sireum.Os.javaHomeOpt(org.sireum.Os.kind, org.sireum.Some(home)) match {
                  case org.sireum.Some(p) =>
                    env.put("JAVA_HOME", p.string.value)
                  case _ =>
                }
                org.sireum.Os.scalaHomeOpt(org.sireum.Some(home)) match {
                  case org.sireum.Some(p) =>
                    env.put("SCALA_HOME", p.string.value)
                  case _ =>
                }
                generalCommandLine.withEnvironment(env)
                val processHandler = new KillableColoredProcessHandler(generalCommandLine)
                SireumClient.sireumToolWindowFactory(iproject, forms => {
                  forms.consoleView.clear()
                  forms.consoleView.attachToProcess(processHandler)
                  ApplicationManager.getApplication.invokeLater(() => {
                    forms.toolWindow.activate(() => {
                      forms.toolWindow.getContentManager.setSelectedContent(forms.toolWindow.getContentManager.findContent("Console"))
                    })
                  })
                })
                processHandler.addProcessListener(new ProcessListener {
                  override def processTerminated(event: ProcessEvent): Unit = {
                    if (event.getExitCode == 0) {
                      baseDir.refresh(false, true)
                      srm.reloadProject()
                      Util.notify(new Notification(
                        SireumClient.groupId, "Proyek synchronized",
                        """<p>Proyek synchronization was successful. <a href="">Restart</a>?</p>""",
                        NotificationType.INFORMATION, (_: Notification, _: HyperlinkEvent) => {
                          ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.restart())
                        }), null, scala.Some(8000))
                    } else {
                      Util.notify(new Notification(
                        SireumClient.groupId, "Proyek failed to synchronize",
                        "<p>Could not synchronize Proyek</p>",
                        NotificationType.ERROR), null, shouldExpire = true)
                    }
                  }

                  override def startNotified(event: ProcessEvent): Unit = {}

                  override def onTextAvailable(event: ProcessEvent, outputType: Key[_]): Unit = {}
                })
                processHandler.startNotify()
                processHandler.waitFor()
              } finally {
                if (srm.isReloadBlocked) {
                  srm.unblockReloadingProjectOnExternalChanges()
                }
              }
            }
          })
        case _ =>
          Util.notify(new Notification(
            SireumClient.groupId, "Sireum home not set",
            "Please set Sireum home directory first",
            NotificationType.INFORMATION), iproject, shouldExpire = true)
      }

    })
  }
}
class ProyekSyncAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    ProyekSyncAction.sync(e.getProject)
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    e.getPresentation.setVisible(true)
    e.getPresentation.setEnabled(project != null && Util.isProyek(project))
  }
}
