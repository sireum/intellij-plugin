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

import java.awt.Color
import javax.swing.{JComponent, SpinnerNumberModel}
import javax.swing.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
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
        vmArgsString != vmArgsTextField.getText ||
        envVarsString != envVarsTextArea.getText ||
        backgroundAnalysis != backgroundCheckBox.isSelected ||
        idle.toString != idleTextField.getText ||
        bgCores != parSpinner.getValue.asInstanceOf[Int])
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

    def updateIdle(text: String) = {
      validIdle = parseGe200(text).nonEmpty
      idleLabel.setForeground(if (validIdle) fgColor else JBColor.red)
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

    backgroundCheckBox.addActionListener(_ => {
      idleLabel.setEnabled(backgroundCheckBox.isSelected)
      idleTextField.setEnabled(backgroundCheckBox.isSelected)
    })

    idleTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = update()

      override def changedUpdate(e: DocumentEvent): Unit = update()

      override def removeUpdate(e: DocumentEvent): Unit = update()

      def update(): Unit = updateIdle(idleTextField.getText)
    })

    parLabel.setText(s"CPU cores (max: $maxCores)")
    parSpinner.setModel(new SpinnerNumberModel(bgCores, 1, maxCores, 1))

    updateEnvVars(envVarsString)
    updateVmArgs(vmArgsString)
    updateIdle(idle.toString)

    sireumPanel
  }

  override def disposeUIResources(): Unit = {
    fgColor = null
  }

  override def apply(): Unit = {
    envVars = parseEnvVars(envVarsTextArea.getText).getOrElse(scala.collection.mutable.LinkedHashMap())
    vmArgs = parseVmArgs(vmArgsTextField.getText).getOrElse(Vector())
    val path = org.sireum.Os.path(sireumHomeTextField.getText)
    sireumHomeOpt = checkSireumDir(path, vmArgs, envVars)
    backgroundAnalysis = backgroundCheckBox.isSelected
    idle = parseGe200(idleTextField.getText).getOrElse(idle)
    bgCores = parSpinner.getValue.asInstanceOf[Int]
    if (sireumHomeOpt.nonEmpty) saveConfiguration()
    else {
      Messages.showMessageDialog(null: Project, sireumInvalid(path),
        "Invalid Sireum Configuration", null)
      SireumApplicationComponent.loadConfiguration()
    }
  }

  override def reset(): Unit = {
    sireumHomeTextField.setText(sireumHomeString)
    vmArgsTextField.setText(vmArgsString)
    envVarsTextArea.setText(envVarsString)
    backgroundCheckBox.setSelected(backgroundAnalysis)
    idleTextField.setText(idle.toString)
    parSpinner.setValue(bgCores)
  }
}
