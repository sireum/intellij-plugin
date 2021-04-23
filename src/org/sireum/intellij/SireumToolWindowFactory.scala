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

import java.util.concurrent.ConcurrentHashMap
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import org.sireum.intellij.logika.LogikaToolWindowForm

object SireumToolWindowFactory {

  final case class Forms(toolWindow: ToolWindow, logika: LogikaToolWindowForm, consoleView: ConsoleView)

  val windows = new ConcurrentHashMap[Project, Forms]()

  def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    toolWindow.setAutoHide(false)
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
    windows.put(project, Forms(toolWindow, logikaForm, console))
  }

  def removeToolWindow(project: Project): Unit = {
    windows.remove(project)
  }
}
