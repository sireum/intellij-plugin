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
import com.intellij.openapi.application.ApplicationManager

import java.awt.Color
import javax.swing.{JComponent, SpinnerNumberModel}
import javax.swing.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor

final class SireumConfigurable extends SireumForm with Configurable {

  private val logo = IconLoader.getIcon("/icon/sireum-logo.png")

  import SireumApplicationComponent._

  private var validSireumHome = false
  private var validVmArgs = false
  private var validEnvVars = false
  private var validIdle = false
  private var fgColor: Color = _

  override def getDisplayName: String = "Sireum"

  override def getHelpTopic: String = null

  override def isModified: Boolean = {
    validSireumHome && validVmArgs && validEnvVars && validIdle &&
      (sireumHomeString != sireumHomeTextField.getText ||
        startup != startupCheckBox.isSelected ||
        logging != loggingCheckBox.isSelected ||
        verbose != verboseCheckBox.isSelected ||
        vmArgsString != vmArgsTextField.getText ||
        envVarsString != envVarsTextArea.getText ||
        cacheInput != cacheInputCheckBox.isSelected ||
        cacheType != cacheTypeCheckBox.isSelected ||
        backgroundAnalysis != bgValue ||
        idle.toString != idleTextField.getText ||
        bgCores != parSpinner.getValue.asInstanceOf[Int] ||
        sireumFont != sireumFontCheckBox.isSelected)
  }

  def parseGe200(text: String): Option[Int] =
    try {
      val n = text.toInt
      if (n < 200) None else Some(n)
    } catch {
      case _: Throwable => None
    }

  override def createComponent(): JComponent = {
    def updateSireumHome(path: org.sireum.Os.Path): Unit = {
      validSireumHome = checkSireumDir(path,
        parseVmArgs(vmArgsTextField.getText).getOrElse(Vector()),
        parseEnvVars(envVarsTextArea.getText).getOrElse(scala.collection.mutable.LinkedHashMap())).nonEmpty
      sireumHomeLabel.setForeground(if (validSireumHome) fgColor else JBColor.red)
      sireumHomeTextField.setToolTipText(if (validSireumHome) "OK" else sireumInvalid(path))
    }

    def updateVmArgs(text: String): Unit = {
      validVmArgs = text == "" || parseVmArgs(text).nonEmpty
      vmArgsLabel.setForeground(if (validVmArgs) fgColor else JBColor.red)
      vmArgsLabel.setToolTipText(if (validVmArgs) "OK" else "Ill-formed (format: space-separated text; each text starts with a dash '-').")
    }

    def updateEnvVars(text: String): Unit = {
      validEnvVars = text == "" || parseEnvVars(text).nonEmpty
      envVarsLabel.setForeground(if (validEnvVars) fgColor else JBColor.red)
      envVarsLabel.setToolTipText(if (validEnvVars) "OK" else "Ill-formed (format: key of [a-zA-Z_][a-zA-Z0-9_]* = value, per line).")
    }

    def updateBg(text: String): Unit = {
      validIdle = parseGe200(text).nonEmpty
      bgIdleRadioButton.setForeground(if (validIdle) fgColor else JBColor.red)
      idleTextField.setToolTipText(if (validIdle) "OK" else "Must be at least 200.")
    }

    logoLabel.setIcon(logo)

    reset()

    fgColor = sireumHomeLabel.getForeground

    sireumHomeTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = update()

      override def changedUpdate(e: DocumentEvent): Unit = update()

      override def removeUpdate(e: DocumentEvent): Unit = update()

      def update(): Unit = updateSireumHome(org.sireum.Os.path(sireumHomeTextField.getText))
    })

    vmArgsTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = update()

      override def changedUpdate(e: DocumentEvent): Unit = update()

      override def removeUpdate(e: DocumentEvent): Unit = update()

      def update(): Unit = updateVmArgs(vmArgsTextField.getText)
    })

    sireumHomeButton.addActionListener(e => browseSireumHome(null) match {
      case Some(p) =>
        updateSireumHome(p)
        if (validSireumHome) sireumHomeTextField.setText(p.string.value)
        else updateSireumHome(org.sireum.Os.path(sireumHomeTextField.getText))
      case _ =>
    })

    updateSireumHome(org.sireum.Os.path(sireumHomeString))

    envVarsTextArea.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = update()

      override def changedUpdate(e: DocumentEvent): Unit = update()

      override def removeUpdate(e: DocumentEvent): Unit = update()

      def update(): Unit = updateEnvVars(envVarsTextArea.getText.trim)
    })

    def updateBackground(): Unit = {
      val enabled = bgValue != 0
      idleTextField.setEnabled(enabled)
      parLabel.setEnabled(enabled)
      parSpinner.setEnabled(enabled)
    }

    def updateVerbose(): Unit = {
      verboseCheckBox.setEnabled(loggingCheckBox.isSelected)
    }

    loggingCheckBox.addActionListener(_ => updateVerbose())
    bgDisabledRadioButton.addActionListener(_ => updateBackground())
    bgSaveRadioButton.addActionListener(_ => updateBackground())
    bgIdleRadioButton.addActionListener(_ => updateBackground())

    idleTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = update()

      override def changedUpdate(e: DocumentEvent): Unit = update()

      override def removeUpdate(e: DocumentEvent): Unit = update()

      def update(): Unit = updateBg(idleTextField.getText)
    })

    parLabel.setText(s"CPU cores (max: $maxCores)")
    parSpinner.setModel(new SpinnerNumberModel(bgCores, 1, maxCores, 1))

    updateEnvVars(envVarsString)
    updateVmArgs(vmArgsString)
    updateBg(idle.toString)
    updateBackground()
    updateVerbose()

    sireumPanel
  }

  override def disposeUIResources(): Unit = {
    fgColor = null
  }

  override def apply(): Unit = {
    val path = org.sireum.Os.path(sireumHomeTextField.getText)
    val homeOpt = checkSireumDir(path, vmArgs, envVars)
    if (homeOpt.nonEmpty) {
      restartServer { () =>
        sireumHomeOpt = homeOpt
        startup = startupCheckBox.isSelected
        logging = loggingCheckBox.isSelected
        verbose = verboseCheckBox.isSelected
        vmArgs = parseVmArgs(vmArgsTextField.getText).getOrElse(Vector())
        envVars = parseEnvVars(envVarsTextArea.getText).getOrElse(scala.collection.mutable.LinkedHashMap())
        cacheInput = cacheInputCheckBox.isSelected
        cacheType = cacheTypeCheckBox.isSelected
        backgroundAnalysis = bgValue
        idle = parseGe200(idleTextField.getText).getOrElse(idle)
        bgCores = parSpinner.getValue.asInstanceOf[Int]
        sireumFont = sireumFontCheckBox.isSelected
        saveConfiguration()
      }
    } else {
      Messages.showMessageDialog(null: Project, sireumInvalid(path),
        "Invalid Sireum Configuration", null)
      SireumApplicationComponent.loadConfiguration()
    }
  }

  def bgValue: Int = if (bgDisabledRadioButton.isSelected) 0 else if (bgSaveRadioButton.isSelected) 1 else 2

  override def reset(): Unit = {
    restartServer { () =>
      sireumHomeTextField.setText(sireumHomeString)
      startupCheckBox.setSelected(startup)
      loggingCheckBox.setSelected(logging)
      verboseCheckBox.setSelected(verbose)
      vmArgsTextField.setText(vmArgsString)
      envVarsTextArea.setText(envVarsString)
      cacheInputCheckBox.setSelected(cacheInput)
      cacheTypeCheckBox.setSelected(cacheType)
      backgroundAnalysis match {
        case 0 => bgDisabledRadioButton.setSelected(true)
        case 1 => bgSaveRadioButton.setSelected(true)
        case _ => bgIdleRadioButton.setSelected(true)
      }
      idleTextField.setText(idle.toString)
      parSpinner.setValue(bgCores)
      sireumFontCheckBox.setSelected(sireumFont)
    }
  }

  def restartServer(f: () => Unit): Unit = {
    val oldVmArgs = vmArgsString
    val oldEnvVars = envVarsString
    val oldCacheInput = cacheInput
    val oldCacheType = cacheType
    val oldLogging = logging
    val oldVerbose = verbose

    f()

    if (SireumClient.processInit.nonEmpty && (oldCacheInput != cacheInput || oldVmArgs != vmArgsString ||
      oldCacheType != cacheType || oldLogging != logging || oldVerbose != verbose || oldEnvVars != envVarsString)) {
      ApplicationManager.getApplication.invokeLater { () =>
        SireumClient.shutdownServer()
        var found = false
        var project: Project = null
        for (p <- ProjectManager.getInstance.getOpenProjects if !found && p.isInitialized && p.isOpen) {
          found = true
          project = p
          SireumClient.init(p)
        }
        SireumClient.processInit match {
          case Some(_) if project != null =>
            Util.notify(new Notification(
              SireumClient.groupId, "Sireum server restarted",
              """<p>The Sireum server has been restarted successfully</p>""",
              NotificationType.INFORMATION), project, shouldExpire = true)
          case _ =>
            Util.notify(new Notification(
              SireumClient.groupId, "Failed to restart the Sireum server",
              """<p>Could not restart the Sireum server</p>""",
              NotificationType.ERROR), project, shouldExpire = true)
        }
      }
    }
  }
}
