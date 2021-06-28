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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.{KillableColoredProcessHandler, ProcessEvent, ProcessListener}
import com.intellij.notification.{Notification, NotificationListener, NotificationType}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.{Project => IProject}
import com.intellij.openapi.util.Key

import java.nio.charset.Charset
import javax.swing.event.HyperlinkEvent

object ProyekSyncAction {
  def sync(iproject: IProject, restart: Boolean): Unit = {
    import org.sireum._
    SireumApplicationComponent.getSireumHome(iproject) match {
      case scala.Some(home) =>
        ProgressManager.getInstance().run(new Task.Backgroundable(iproject, "Proyek") {
          override def run(indicator: ProgressIndicator): Unit = {
            indicator.setIndeterminate(true)
            indicator.setText("Importing project ...")
            val sireum = if (Os.isWin) "sireum.bat" else "./sireum"
            val cmds = new java.util.ArrayList[Predef.String]
            cmds.add(sireum)
            cmds.add("proyek")
            cmds.add("ive")
            cmds.add(iproject.getBasePath)
            val generalCommandLine = new GeneralCommandLine(cmds)
            generalCommandLine.setWorkDirectory((home / "bin").string.value)
            generalCommandLine.setCharset(Charset.forName("UTF-8"))
            val processHandler = new KillableColoredProcessHandler(generalCommandLine)
            SireumClient.sireumToolWindowFactory(iproject, forms => {
              forms.consoleView.clear()
              forms.consoleView.attachToProcess(processHandler)
              ApplicationManager.getApplication.invokeLater(() => {
                forms.toolWindow.getContentManager.setSelectedContent(forms.toolWindow.getContentManager.findContent("Console"))
              })
            })
            processHandler.addProcessListener(new ProcessListener {
              override def processTerminated(event: ProcessEvent): Unit = {
                if (event.getExitCode == 0) {
                  iproject.getBaseDir.refresh(false, true)
                  if (restart) {
                    Util.notify(new Notification(
                      SireumClient.groupId, "Proyek synchronized",
                      """<p>Proyek synchronization was successful</p>""",
                      NotificationType.INFORMATION), iproject, shouldExpire = true)
                    ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.restart())
                  } else {
                    Util.notify(new Notification(
                      SireumClient.groupId, "Proyek synchronized",
                      """<p>Proyek synchronization was successful. <a href="">Restart</a>?</p>""",
                      NotificationType.INFORMATION, new NotificationListener.Adapter {
                        override def hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent): Unit = {
                          ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.restart())
                        }
                      }), iproject, scala.Some(8000))
                  }
                } else {
                  Util.notify(new Notification(
                    SireumClient.groupId, "Proyek failed to synchronize",
                    "<p>Could not synchronize Proyek</p>",
                    NotificationType.ERROR), iproject, shouldExpire = true)
                }
              }
              override def startNotified(event: ProcessEvent): Unit = {}
              override def onTextAvailable(event: ProcessEvent, outputType: Key[_]): Unit = {}
            })
            processHandler.startNotify()
            processHandler.waitFor()
          }
        })
      case _ =>
        Util.notify(new Notification(
          SireumClient.groupId, "Sireum home not set",
          "Please set Sireum home directory first",
          NotificationType.INFORMATION), iproject, shouldExpire = true)
    }
  }
}
class ProyekSyncAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    ProyekSyncAction.sync(e.getProject, false)
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    e.getPresentation.setVisible(true)
    e.getPresentation.setEnabled(project != null && Util.isProyek(project))
  }
}
