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

package org.sireum.intellij.logika

import java.awt.Color
import javax.swing.JComponent
import javax.swing.event.{DocumentEvent, DocumentListener}

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.{IconLoader, SystemInfo}
import com.intellij.ui.JBColor

object LogikaConfigurable {
  private val logo = IconLoader.getIcon("/icon/logika-logo.png")

  private val logikaKey = "org.sireum.logika."
  private val backgroundAnalysisKey = logikaKey + "background"
  private val timeoutKey = logikaKey + "timeout"
  private val autoEnabledKey = logikaKey + "auto"
  private val checkSatKey = logikaKey + "checkSat"
  private val hintKey = logikaKey + "hint"
  private val hintUnicodeKey = logikaKey + "hintUnicode"
  private val inscribeSummoningsKey = logikaKey + "inscribeSummonings"
  private val bitWidthKey = logikaKey + "bitWidth"
  private val loopBoundKey = logikaKey + "loopBound"
  private val recursionBoundKey = logikaKey + "recursionBound"
  private val methodContractKey = logikaKey + "methodContract"
  private val useRealKey = logikaKey + "useReal"
  private val fpRoundingModeKey = logikaKey + "fpRounding"
  private val cvcRLimitKey = logikaKey + "cvc.rlimit"
  private val cvcValidOptsKey = logikaKey + "cvc.vopts"
  private val cvcSatOptsKey = logikaKey + "cvc.sopts"
  private val z3ValidOptsKey = logikaKey + "z3.vopts"
  private val z3SatOptsKey = logikaKey + "z3.sopts"

  private[intellij] var backgroundAnalysis: Boolean = true
  private[intellij] var timeout: Int = 2000
  private[intellij] var autoEnabled: Boolean = true
  private[intellij] var checkSat: Boolean = false
  private[intellij] var hint: Boolean = true
  private[intellij] var hintUnicode: Boolean = SystemInfo.isMac
  private[intellij] var inscribeSummonings: Boolean = true
  // TODO
  //private[intellij] var checkerKind = CheckerKind.Forward
  private[intellij] var bitWidth: Int = 0
  private[intellij] var loopBound: Int = 3
  private[intellij] var recursionBound: Int = 1
  private[intellij] var methodContract: Boolean = true
  private[intellij] var useReal: Boolean = false
  private[intellij] var fpRoundingMode: String = "RNE"
  private[intellij] var cvcRLimit: Int = 1000000
  private[intellij] var cvcValidOpts: String = "--full-saturate-quant"
  private[intellij] var cvcSatOpts: String = ""
  private[intellij] var z3ValidOpts: String = ""
  private[intellij] var z3SatOpts: String = ""

  def loadConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    backgroundAnalysis = pc.getBoolean(backgroundAnalysisKey, backgroundAnalysis)
    timeout = pc.getInt(timeoutKey, timeout)
    autoEnabled = pc.getBoolean(autoEnabledKey, autoEnabled)
    checkSat = pc.getBoolean(checkSatKey, checkSat)
    hint = pc.getBoolean(hintKey, hint)
    hintUnicode = pc.getBoolean(hintUnicodeKey, hintUnicode)
    inscribeSummonings = pc.getBoolean(inscribeSummoningsKey, inscribeSummonings)
    // TODO
    //checkerKind = pc.getValue(checkerKindKey, checkerKind)
    bitWidth = pc.getInt(bitWidthKey, bitWidth)
    loopBound = pc.getInt(loopBoundKey, loopBound)
    recursionBound = pc.getInt(recursionBoundKey, recursionBound)
    methodContract = pc.getBoolean(methodContractKey, methodContract)
    useReal = pc.getBoolean(useRealKey, useReal)
    fpRoundingMode = pc.getValue(fpRoundingModeKey, fpRoundingMode)
    cvcRLimit = pc.getInt(cvcRLimitKey, cvcRLimit)
    cvcValidOpts = pc.getValue(cvcValidOptsKey, cvcValidOpts)
    cvcSatOpts = pc.getValue(cvcSatOptsKey, cvcSatOpts)
    z3ValidOpts = pc.getValue(z3ValidOptsKey, z3ValidOpts)
    z3SatOpts = pc.getValue(z3SatOptsKey, z3SatOpts)
  }

  def saveConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    pc.setValue(backgroundAnalysisKey, backgroundAnalysis.toString)
    pc.setValue(timeoutKey, timeout.toString)
    pc.setValue(autoEnabledKey, autoEnabled.toString)
    pc.setValue(checkSatKey, checkSat.toString)
    pc.setValue(hintKey, hint.toString)
    pc.setValue(hintUnicodeKey, hintUnicode.toString)
    pc.setValue(inscribeSummoningsKey, inscribeSummonings.toString)
    // TODO
    //pc.setValue(checkerKindKey, checkerKind)
    pc.setValue(bitWidthKey, bitWidth.toString)
    pc.setValue(loopBoundKey, loopBound.toString)
    pc.setValue(recursionBoundKey, recursionBound.toString)
    pc.setValue(methodContractKey, methodContract.toString)
    pc.setValue(useRealKey, useReal.toString)
    pc.setValue(fpRoundingModeKey, fpRoundingMode)
    pc.setValue(cvcRLimitKey, cvcRLimit.toString)
    pc.setValue(cvcValidOptsKey, cvcValidOpts)
    pc.setValue(cvcSatOptsKey, cvcSatOpts)
    pc.setValue(z3ValidOptsKey, z3ValidOpts)
    pc.setValue(z3SatOptsKey, z3SatOpts)
  }

  def parseGe200(text: String): Option[Int] =
    try {
      val n = text.toInt
      if (n < 200) None else Some(n)
    } catch {
      case _: Throwable => None
    }

  def parsePosInteger(text: String): Option[Int] =
    try {
      val n = text.toInt
      if (n <= 0) None else Some(n)
    } catch {
      case _: Throwable => None
    }

  def parseSmt2Opts(text: String): Option[String] = {
    if ("" != text) for (e <- text.split(' ') if !e.startsWith("-")) return None
    return Some(text)
  }

  def parseFileExts(text: String): Option[Seq[String]] = {
    var r = Vector[String]()
    for (e <- text.split(";")) {
      val ext = e.trim
      if (ext.nonEmpty && ext.forall(_.isLetterOrDigit))
        r :+= ext
      else return None
    }
    Some(r)
  }
}

import LogikaConfigurable._

final class LogikaConfigurable extends LogikaForm with Configurable {

  private var validTimeout: Boolean = true
  private var validLoopBound: Boolean = true
  private var validRecursionBound: Boolean = true
  private var validCvcRLimit: Boolean = true
  private var validCvcValidOpts: Boolean = true
  private var validCvcSatOpts: Boolean = true
  private var validZ3ValidOpts: Boolean = true
  private var validZ3SatOpts: Boolean = true
  private var fgColor: Color = _

  override def getDisplayName: String = "Logika"

  override def getHelpTopic: String = null

  override def isModified: Boolean =
    validTimeout && validLoopBound && validRecursionBound &&
      validCvcRLimit && validCvcValidOpts & validCvcSatOpts && validZ3ValidOpts && validZ3SatOpts &&
      (backgroundCheckBox.isSelected != backgroundAnalysis ||
        timeoutTextField.getText != timeout.toString ||
        autoCheckBox.isSelected != autoEnabled ||
        checkSatCheckBox.isSelected != checkSat ||
        hintCheckBox.isSelected != hint ||
        hintUnicodeCheckBox.isSelected != hintUnicode ||
        inscribeSummoningsCheckBox.isSelected != inscribeSummonings ||
        // TODO
        //selectedKind != checkerKind ||
        selectedBitWidth != bitWidth ||
        //loopBoundTextField.getText != loopBound.toString ||
        //recursionBoundTextField.getText != recursionBound.toString ||
        //methodContractCheckBox.isSelected != methodContract
        useRealCheckBox.isSelected != useReal ||
        selectedFPRoundingMode != fpRoundingMode ||
        cvcRLimitTextField.getText != cvcRLimit.toString ||
        cvcValidOptsTextField.getText != cvcValidOpts ||
        cvcSatOptsTextField.getText != cvcSatOpts ||
        z3ValidOptsTextField.getText != z3ValidOpts ||
        z3SatOptsTextField.getText != z3SatOpts)

  /* TODO
  private def selectedKind: CheckerKind.Value =
    if (forwardRadioButton.isSelected) CheckerKind.Forward
    else if (backwardRadioButton.isSelected) CheckerKind.Backward
    else if (symExeRadioButton.isSelected) CheckerKind.SummarizingSymExe
    else if (unrollingSymExeRadioButton.isSelected) CheckerKind.UnrollingSymExe
    else sys.error("Unexpected checker kind.")

   */

  def selectedFPRoundingMode: String = {
    if (fpRNERadioButton.isSelected) "RNE"
    else if (fpRNARadioButton.isSelected) "RNA"
    else if (fpRTPRadioButton.isSelected) "RTP"
    else if (fpRTNRadioButton.isSelected) "RTN"
    else if (fpRTZRadioButton.isSelected) "RTZ"
    else sys.error("Unexpected FP rounding mode")
  }

  private def selectedBitWidth: Int =
    if (bitsUnboundedRadioButton.isSelected) 0
    else if (bits8RadioButton.isSelected) 8
    else if (bits16RadioButton.isSelected) 16
    else if (bits32RadioButton.isSelected) 32
    else if (bits64RadioButton.isSelected) 64
    else sys.error("Unexpected bit width.")

  override def createComponent(): JComponent = {
    //devPanel.setVisible(false)
    //unrollingSymExeRadioButton.setEnabled(false)

    def updateTimeout() = {
      val text = timeoutTextField.getText
      validTimeout = parseGe200(text).nonEmpty
      timeoutLabel.setForeground(if (validTimeout) fgColor else JBColor.red)
      timeoutTextField.setToolTipText(if (validTimeout) "OK" else "Must be at least 200.")
    }
//    def updateLoopBound() = {
//      val text = loopBoundTextField.getText
//      validLoopBound = parsePosInteger(text).nonEmpty
//      loopBoundLabel.setForeground(if (validLoopBound) fgColor else JBColor.red)
//      loopBoundTextField.setToolTipText(if (validLoopBound) "OK" else "Must be at least 1.")
//    }
//    def updateRecursionBound() = {
//      val text = recursionBoundTextField.getText
//      validRecursionBound = parsePosInteger(text).nonEmpty
//      recursionBoundLabel.setForeground(if (validRecursionBound) fgColor else JBColor.red)
//      recursionBoundTextField.setToolTipText(if (validRecursionBound) "OK" else "Must be at least 1.")
//    }
    def updateSymExe() = {
//      val isUnrolling = unrollingSymExeRadioButton.isSelected
      val isSymExe = true // symExeRadioButton.isSelected || isUnrolling
      bitsLabel.setEnabled(isSymExe)
      bitsUnboundedRadioButton.setEnabled(isSymExe)
      // TODO
      //bits8RadioButton.setEnabled(isSymExe)
      //bits16RadioButton.setEnabled(isSymExe)
      //bits32RadioButton.setEnabled(isSymExe)
      //bits64RadioButton.setEnabled(isSymExe)
      autoCheckBox.setEnabled(!isSymExe)
//      loopBoundLabel.setEnabled(isUnrolling)
//      loopBoundTextField.setEnabled(isUnrolling)
//      recursionBoundLabel.setEnabled(isUnrolling)
//      recursionBoundTextField.setEnabled(isUnrolling)
      //methodContractCheckBox.setEnabled(isUnrolling)
    }
    def updateHintUnicode() = {
      hintUnicodeCheckBox.setEnabled(hintCheckBox.isSelected)
    }

    def updateFPRoundingMode() = {
      val enabled = !useRealCheckBox.isSelected
      fpRNERadioButton.setEnabled(enabled)
      fpRNARadioButton.setEnabled(enabled)
      fpRTPRadioButton.setEnabled(enabled)
      fpRTNRadioButton.setEnabled(enabled)
      fpRTZRadioButton.setEnabled(enabled)
    }

    def updateCvcRLimit() = {
      val text = cvcRLimitTextField.getText
      validCvcRLimit = parsePosInteger(text).nonEmpty
      cvcRLimitLabel.setForeground(if (validCvcRLimit) fgColor else JBColor.red)
      cvcRLimitTextField.setToolTipText(if (validCvcRLimit) "OK" else "Must be positive integer.")
    }

    def updateCvcValidOpts() = {
      val text = cvcValidOptsTextField.getText
      validCvcValidOpts = parseSmt2Opts(text).nonEmpty
      cvcValidOptsLabel.setForeground(if (validCvcValidOpts) fgColor else JBColor.red)
      cvcValidOptsTextField.setToolTipText(if (validCvcValidOpts) "OK" else "Each element starts with a dash (-).")
    }

    def updateCvcSatOpts() = {
      val text = cvcSatOptsTextField.getText
      validCvcSatOpts = parseSmt2Opts(text).nonEmpty
      cvcSatOptsLabel.setForeground(if (validCvcSatOpts) fgColor else JBColor.red)
      cvcSatOptsTextField.setToolTipText(if (validCvcSatOpts) "OK" else "Each element starts with a dash (-).")
    }

    def updateZ3ValidOpts() = {
      val text = z3ValidOptsTextField.getText
      validZ3ValidOpts = parseSmt2Opts(text).nonEmpty
      z3ValidOptsLabel.setForeground(if (validZ3ValidOpts) fgColor else JBColor.red)
      z3ValidOptsTextField.setToolTipText(if (validZ3ValidOpts) "OK" else "Each element starts with a dash (-).")
    }

    def updateZ3SatOpts() = {
      val text = z3SatOptsTextField.getText
      validZ3SatOpts = parseSmt2Opts(text).nonEmpty
      z3SatOptsLabel.setForeground(if (validZ3SatOpts) fgColor else JBColor.red)
      z3SatOptsTextField.setToolTipText(if (validZ3SatOpts) "OK" else "Each element starts with a dash (-).")
    }

    logoLabel.setIcon(logo)

    reset()

    fgColor = logoLabel.getForeground

    timeoutTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateTimeout()

      override def changedUpdate(e: DocumentEvent): Unit = updateTimeout()

      override def removeUpdate(e: DocumentEvent): Unit = updateTimeout()
    })

    cvcRLimitTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateCvcRLimit()

      override def changedUpdate(e: DocumentEvent): Unit = updateCvcRLimit()

      override def removeUpdate(e: DocumentEvent): Unit = updateCvcRLimit()
    })

    cvcValidOptsTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateCvcValidOpts()

      override def changedUpdate(e: DocumentEvent): Unit = updateCvcValidOpts()

      override def removeUpdate(e: DocumentEvent): Unit = updateCvcValidOpts()
    })

    cvcSatOptsTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateCvcSatOpts()

      override def changedUpdate(e: DocumentEvent): Unit = updateCvcSatOpts()

      override def removeUpdate(e: DocumentEvent): Unit = updateCvcSatOpts()
    })

    z3ValidOptsTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateZ3ValidOpts()

      override def changedUpdate(e: DocumentEvent): Unit = updateZ3ValidOpts()

      override def removeUpdate(e: DocumentEvent): Unit = updateZ3ValidOpts()
    })

    z3SatOptsTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateZ3SatOpts()

      override def changedUpdate(e: DocumentEvent): Unit = updateZ3SatOpts()

      override def removeUpdate(e: DocumentEvent): Unit = updateZ3SatOpts()
    })

    //    loopBoundTextField.getDocument.addDocumentListener(new DocumentListener {
//      override def insertUpdate(e: DocumentEvent): Unit = updateLoopBound()
//
//      override def changedUpdate(e: DocumentEvent): Unit = updateLoopBound()
//
//      override def removeUpdate(e: DocumentEvent): Unit = updateLoopBound()
//    })

//    recursionBoundTextField.getDocument.addDocumentListener(new DocumentListener {
//      override def insertUpdate(e: DocumentEvent): Unit = updateRecursionBound()
//
//      override def changedUpdate(e: DocumentEvent): Unit = updateRecursionBound()
//
//      override def removeUpdate(e: DocumentEvent): Unit = updateRecursionBound()
//    })

//    symExeRadioButton.addChangeListener(_ => updateSymExe())
//    unrollingSymExeRadioButton.addChangeListener(_ => updateSymExe())
    hintCheckBox.addChangeListener(_ => updateHintUnicode())

    useRealCheckBox.addChangeListener(_ => updateFPRoundingMode())

    updateSymExe()
    updateHintUnicode()
    updateFPRoundingMode()

    logikaPanel
  }

  override def disposeUIResources(): Unit = {}

  override def apply(): Unit = {
    backgroundAnalysis = backgroundCheckBox.isSelected
    timeout = parseGe200(timeoutTextField.getText).getOrElse(timeout)
    autoEnabled = autoCheckBox.isSelected
    checkSat = checkSatCheckBox.isSelected
    hint = hintCheckBox.isSelected
    hintUnicode = hintUnicodeCheckBox.isSelected
    inscribeSummonings = inscribeSummoningsCheckBox.isSelected
    // TODO
    //checkerKind = selectedKind
    bitWidth = selectedBitWidth
//    loopBound = parsePosInteger(loopBoundTextField.getText).getOrElse(loopBound)
//    recursionBound = parsePosInteger(recursionBoundTextField.getText).getOrElse(recursionBound)
//    methodContract = methodContractCheckBox.isSelected
    useReal = useRealCheckBox.isSelected
    fpRoundingMode = selectedFPRoundingMode
    cvcRLimit = parsePosInteger(cvcRLimitTextField.getText).getOrElse(cvcRLimit)
    cvcValidOpts = parseSmt2Opts(cvcValidOptsTextField.getText).getOrElse(cvcValidOpts)
    cvcSatOpts = parseSmt2Opts(cvcSatOptsTextField.getText).getOrElse(cvcSatOpts)
    z3ValidOpts = parseSmt2Opts(z3ValidOptsTextField.getText).getOrElse(z3ValidOpts)
    z3SatOpts = parseSmt2Opts(z3SatOptsTextField.getText).getOrElse(z3SatOpts)
    saveConfiguration()
  }

  override def reset(): Unit = {
    backgroundCheckBox.setSelected(backgroundAnalysis)
    timeoutTextField.setText(timeout.toString)
    autoCheckBox.setSelected(autoEnabled)
    checkSatCheckBox.setSelected(checkSat)
    hintCheckBox.setSelected(hint)
    hintUnicodeCheckBox.setSelected(hintUnicode)
    inscribeSummoningsCheckBox.setSelected(inscribeSummonings)
    /* TODO
    checkerKind match {
      case CheckerKind.Forward => forwardRadioButton.setSelected(true)
      case CheckerKind.Backward => backwardRadioButton.setSelected(true)
      case CheckerKind.SummarizingSymExe => symExeRadioButton.setSelected(true)
      case CheckerKind.UnrollingSymExe => unrollingSymExeRadioButton.setSelected(true)
    }
     */
    bitWidth match {
      case 0 => bitsUnboundedRadioButton.setSelected(true)
      case 8 => bits8RadioButton.setSelected(true)
      case 16 => bits16RadioButton.setSelected(true)
      case 32 => bits32RadioButton.setSelected(true)
      case 64 => bits64RadioButton.setSelected(true)
    }
//    loopBoundTextField.setText(loopBound.toString)
//    recursionBoundTextField.setText(recursionBound.toString)
//    methodContractCheckBox.setSelected(methodContract)
    useRealCheckBox.setSelected(useReal)
    fpRoundingMode match {
      case "RNE" => fpRNERadioButton.setSelected(true)
      case "RNA" => fpRNARadioButton.setSelected(true)
      case "RTP" => fpRTPRadioButton.setSelected(true)
      case "RTN" => fpRTNRadioButton.setSelected(true)
      case "RTZ" => fpRTZRadioButton.setSelected(true)
    }
    cvcRLimitTextField.setText(cvcRLimit.toString)
    cvcValidOptsTextField.setText(cvcValidOpts)
    cvcSatOptsTextField.setText(cvcSatOpts)
    z3ValidOptsTextField.setText(z3ValidOpts)
    z3SatOptsTextField.setText(z3SatOpts)
  }
}
