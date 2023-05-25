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

import java.awt.{Color, Cursor}
import javax.swing.{JComponent, SpinnerNumberModel}
import javax.swing.event.{DocumentEvent, DocumentListener, HyperlinkEvent}
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.{Notification, NotificationListener, NotificationType}
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.{IconLoader, SystemInfo}
import com.intellij.ui.JBColor
import org.sireum.intellij.{SireumApplicationComponent, SireumClient, Util}
import org.sireum.logika.Smt2

import java.awt.event.{MouseEvent, MouseListener}

object LogikaConfigurable {
  private val logo = IconLoader.getIcon("/icon/logika-logo.png")

  private val logikaKey = "org.sireum.logika."
  private val backgroundAnalysisKey = logikaKey + "background"
  private val rlimitKey = logikaKey + "rlimit"
  private val timeoutKey = logikaKey + "timeout"
  private val autoEnabledKey = logikaKey + "auto"
  private val checkSatKey = logikaKey + "checkSat"
  private val hintKey = logikaKey + "hint"
  private val coverageKey = logikaKey + "coverage"
  private val coverageIntensityKey = logikaKey + "coverage.intensity"
  private val hintMaxColumnKey = logikaKey + "hintMaxColumn"
  private val hintUnicodeKey = logikaKey + "hintUnicode"
  private val hintLinesFreshKey = logikaKey + "hintLinesFresh"
  private val inscribeSummoningsKey = logikaKey + "inscribeSummonings"
  private val bitWidthKey = logikaKey + "bitWidth"
  private val loopBoundKey = logikaKey + "loopBound"
  private val callBoundKey = logikaKey + "callBound"
  private val methodContractKey = logikaKey + "methodContract"
  private val useRealKey = logikaKey + "useReal"
  private val fpRoundingModeKey = logikaKey + "fpRounding"
  private val smt2ValidOptsKey = logikaKey + "smt2.vopts"
  private val smt2SatOptsKey = logikaKey + "smt2.sopts"
  private val smt2CacheOptsKey = logikaKey + "smt2.caching"
  private val smt2SeqOptsKey = logikaKey + "smt2.seq"
  private val smt2SimplifyKey = logikaKey + "smt2.simplify"
  private val smt2DefaultConfigsKey = logikaKey + "smt2.default"
  private val branchParKey = logikaKey + "branchPar"
  private val branchParCoresKey = logikaKey + "branchParCores"
  private val splitCondsKey = logikaKey + "split.conditionals"
  private val splitMatchCasesKey = logikaKey + "split.matchCases"
  private val splitContractCasesKey = logikaKey + "split.contractCases"
  private val interpContractsKey = logikaKey + "interp.contracts"
  private val strictPureModeKey = logikaKey + "strictPureMode"
  private val infoFlowKey = logikaKey + "infoflow"
  private val rawInscriptionKey = logikaKey + "smt2.raw"
  private val elideEncodingKey = logikaKey + "smt2.elide"
  private val transitionCacheKey = logikaKey + "transitions.cache"
  private val patternExhaustiveKey = logikaKey + "pattern.exhaustiveness"
  private val pureFunKey = logikaKey + "pureFun"
  private val detailedInfoKey = logikaKey + "detailedInfo"
  private val satTimeoutKey = logikaKey + "timeout.sat"

  private lazy val defaultSmt2ValidOpts: String = org.sireum.logika.Smt2.defaultValidOpts.value.split(';').map(_.trim).mkString(";\n")
  private lazy val defaultSmt2SatOpts: String = org.sireum.logika.Smt2.defaultSatOpts.value.split(';').map(_.trim).mkString(";\n")

  private[intellij] var backgroundAnalysis: Boolean = true
  private[intellij] var timeout: Int = org.sireum.logika.Smt2.validTimeoutInMs.toInt
  private[intellij] var rlimit: Long = org.sireum.logika.Smt2.rlimit.toInt
  private[intellij] var autoEnabled: Boolean = true
  private[intellij] var checkSat: Boolean = false
  private[intellij] var hint: Boolean = true
  private[intellij] var coverage: Boolean = true
  private[intellij] var coverageIntensity: Int = 20
  private[intellij] var hintMaxColumn: Int = 60
  private[intellij] var hintUnicode: Boolean = SystemInfo.isMac
  private[intellij] var hintLinesFresh: Boolean = false
  private[intellij] var inscribeSummonings: Boolean = true
  private[intellij] var bitWidth: Int = 0
  private[intellij] var loopBound: Int = 3
  private[intellij] var callBound: Int = 3
  private[intellij] var methodContract: Boolean = true
  private[intellij] var useReal: Boolean = false
  private[intellij] var fpRoundingMode: String = "RNE"
  private[intellij] var smt2ValidOpts: String = defaultSmt2ValidOpts
  private[intellij] var smt2SatOpts: String = defaultSmt2SatOpts
  private[intellij] var smt2Cache: Boolean = true
  private[intellij] var smt2Seq: Boolean = false
  private[intellij] var smt2Simplify: Boolean = false
  private[intellij] var branchPar: org.sireum.logika.Config.BranchPar.Type = org.sireum.logika.Config.BranchPar.All
  private[intellij] var branchParCores: Int = Runtime.getRuntime.availableProcessors
  private[intellij] var splitConds: Boolean = false
  private[intellij] var splitMatchCases: Boolean = false
  private[intellij] var splitContractCases: Boolean = false
  private[intellij] var interpContracts: Boolean = false
  private[intellij] var strictPureMode: org.sireum.logika.Config.StrictPureMode.Type = org.sireum.logika.Config.StrictPureMode.Default
  private[intellij] var infoFlow: Boolean = false
  private[intellij] var rawInscription: Boolean = false
  private[intellij] var elideEncoding: Boolean = false
  private[intellij] var transitionCache: Boolean = true
  private[intellij] var patternExhaustive: Boolean = true
  private[intellij] var pureFun: Boolean = false
  private[intellij] var detailedInfo: Boolean = true
  private[intellij] var satTimeout: Boolean = false

  def loadConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    backgroundAnalysis = pc.getBoolean(backgroundAnalysisKey, backgroundAnalysis)
    timeout = pc.getInt(timeoutKey, timeout)
    rlimit = pc.getLong(rlimitKey, rlimit)
    autoEnabled = pc.getBoolean(autoEnabledKey, autoEnabled)
    checkSat = pc.getBoolean(checkSatKey, checkSat)
    hint = pc.getBoolean(hintKey, hint)
    coverage = pc.getBoolean(coverageKey, coverage)
    coverageIntensity = pc.getInt(coverageIntensityKey, coverageIntensity)
    hintMaxColumn = pc.getInt(hintMaxColumnKey, hintMaxColumn)
    hintUnicode = pc.getBoolean(hintUnicodeKey, hintUnicode)
    hintLinesFresh = pc.getBoolean(hintLinesFreshKey, hintLinesFresh)
    inscribeSummonings = pc.getBoolean(inscribeSummoningsKey, inscribeSummonings)
    smt2Cache = pc.getBoolean(smt2CacheOptsKey, smt2Cache)
    smt2Seq = pc.getBoolean(smt2SeqOptsKey, smt2Seq)
    smt2Simplify = pc.getBoolean(smt2SimplifyKey, smt2Simplify)
    val defaultOpts = defaultSmt2ValidOpts + ";" + defaultSmt2SatOpts + ";" + Smt2.validTimeoutInMs + ";" + Smt2.rlimit
    if (defaultOpts != pc.getValue(smt2DefaultConfigsKey)) {
      Util.notify(new Notification(SireumClient.groupId, "Update Logika SMT2 default configurations?",
        """<p>Logika SMT2 default configurations have changed. <a href="">Update</a>?</p>""",
        NotificationType.INFORMATION, new NotificationListener.Adapter {
          override def hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent): Unit = {
            LogikaConfigurable.smt2ValidOpts = defaultSmt2ValidOpts
            LogikaConfigurable.smt2SatOpts = defaultSmt2SatOpts
            LogikaConfigurable.timeout = Smt2.validTimeoutInMs.toInt
            LogikaConfigurable.rlimit = Smt2.rlimit.toLong
            notification.hideBalloon()
            Util.notify(new Notification(SireumClient.groupId, "Logika SMT2 Configurations",
              "The configurations have been successfully updated",
              NotificationType.INFORMATION, null), null, shouldExpire = true)
          }
        }), null, scala.None)
      pc.setValue(smt2DefaultConfigsKey, defaultOpts)
    }
    bitWidth = pc.getInt(bitWidthKey, bitWidth)
    loopBound = pc.getInt(loopBoundKey, loopBound)
    callBound = pc.getInt(callBoundKey, callBound)
    methodContract = pc.getBoolean(methodContractKey, methodContract)
    useReal = pc.getBoolean(useRealKey, useReal)
    fpRoundingMode = pc.getValue(fpRoundingModeKey, fpRoundingMode)
    smt2ValidOpts = pc.getValue(smt2ValidOptsKey, smt2ValidOpts)
    smt2SatOpts = pc.getValue(smt2SatOptsKey, smt2SatOpts)
    branchPar = org.sireum.logika.Config.BranchPar.byOrdinal(pc.getInt(branchParKey, branchPar.ordinal.toInt)).get
    branchParCores = pc.getInt(branchParCoresKey, branchParCores)
    splitConds = pc.getBoolean(splitCondsKey, splitConds)
    splitMatchCases = pc.getBoolean(splitMatchCasesKey, splitMatchCases)
    splitContractCases = pc.getBoolean(splitContractCasesKey, splitContractCases)
    interpContracts = pc.getBoolean(interpContractsKey, interpContracts)
    strictPureMode = org.sireum.logika.Config.StrictPureMode.byOrdinal(pc.getInt(strictPureModeKey, strictPureMode.ordinal.toInt)).get
    infoFlow = pc.getBoolean(infoFlowKey, infoFlow)
    rawInscription = pc.getBoolean(rawInscriptionKey, rawInscription)
    elideEncoding = pc.getBoolean(elideEncodingKey, elideEncoding)
    transitionCache = pc.getBoolean(transitionCacheKey, transitionCache)
    patternExhaustive = pc.getBoolean(patternExhaustiveKey, patternExhaustive)
    pureFun = pc.getBoolean(pureFunKey, pureFun)
    detailedInfo = pc.getBoolean(detailedInfoKey, detailedInfo)
    satTimeout = pc.getBoolean(satTimeoutKey, satTimeout)
    SireumClient.coverageTextAttributes.setBackgroundColor(SireumClient.createCoverageColor(coverageIntensity))
  }

  def saveConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    pc.setValue(backgroundAnalysisKey, backgroundAnalysis.toString)
    pc.setValue(rlimitKey, rlimit.toString)
    pc.setValue(timeoutKey, timeout.toString)
    pc.setValue(autoEnabledKey, autoEnabled.toString)
    pc.setValue(checkSatKey, checkSat.toString)
    pc.setValue(hintKey, hint.toString)
    pc.setValue(coverageKey, coverage.toString)
    pc.setValue(coverageIntensityKey, coverageIntensityKey.toString)
    pc.setValue(hintMaxColumnKey, hintMaxColumn.toString)
    pc.setValue(hintUnicodeKey, hintUnicode.toString)
    pc.setValue(hintLinesFreshKey, hintLinesFresh.toString)
    pc.setValue(inscribeSummoningsKey, inscribeSummonings.toString)
    pc.setValue(smt2CacheOptsKey, smt2Cache.toString)
    pc.setValue(smt2SeqOptsKey, smt2Seq.toString)
    pc.setValue(smt2SimplifyKey, smt2Simplify.toString)
    pc.setValue(bitWidthKey, bitWidth.toString)
    pc.setValue(loopBoundKey, loopBound.toString)
    pc.setValue(callBoundKey, callBound.toString)
    pc.setValue(methodContractKey, methodContract.toString)
    pc.setValue(useRealKey, useReal.toString)
    pc.setValue(fpRoundingModeKey, fpRoundingMode)
    pc.setValue(smt2ValidOptsKey, smt2ValidOpts)
    pc.setValue(smt2SatOptsKey, smt2SatOpts)
    pc.setValue(branchParKey, branchPar.ordinal.toString)
    pc.setValue(branchParCoresKey, branchParCores.toString)
    pc.setValue(splitCondsKey, splitConds.toString)
    pc.setValue(splitMatchCasesKey, splitMatchCases.toString)
    pc.setValue(splitContractCasesKey, splitContractCases.toString)
    pc.setValue(interpContractsKey, interpContracts.toString)
    pc.setValue(strictPureModeKey, strictPureMode.ordinal.toString)
    pc.setValue(infoFlowKey, infoFlow.toString)
    pc.setValue(rawInscriptionKey, rawInscription.toString)
    pc.setValue(elideEncodingKey, elideEncoding.toString)
    pc.setValue(transitionCacheKey, transitionCache.toString)
    pc.setValue(patternExhaustiveKey, patternExhaustive.toString)
    pc.setValue(pureFunKey, pureFun.toString)
    pc.setValue(detailedInfoKey, detailedInfo.toString)
    pc.setValue(satTimeoutKey, satTimeout.toString)
    SireumClient.coverageTextAttributes.setBackgroundColor(SireumClient.createCoverageColor(coverageIntensity))
  }

  def parseGe(text: String, min: Long): Option[Long] =
    try {
      val n = text.toLong
      if (n < min) None else Some(n)
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
    if ("" != text.trim) {
      for (e <- text.split(';').map(_.trim)) {
        e.split(',').map(_.trim) match {
          case Array(solver, _*) if Smt2.solverArgsMap.contains(solver) =>
          case _ => return None
        }
      }
    }
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

  private var validRLimit: Boolean = true
  private var validTimeout: Boolean = true
  private var validHintMaxColumn: Boolean = true
  private var validLoopBound: Boolean = true
  private var validRecursionBound: Boolean = true
  private var validSmt2ValidOpts: Boolean = true
  private var validSmt2SatOpts: Boolean = true
  private var fgColor: Color = _

  override def getDisplayName: String = "Logika"

  override def getHelpTopic: String = null

  override def isModified: Boolean =
    validTimeout && validRLimit && validHintMaxColumn && validSmt2ValidOpts && validSmt2SatOpts &&
      validLoopBound && validRecursionBound &&
      (backgroundCheckBox.isSelected != backgroundAnalysis ||
        rlimitTextField.getText != rlimit.toString ||
        timeoutTextField.getText != timeout.toString ||
        checkSatCheckBox.isSelected != checkSat ||
        hintCheckBox.isSelected != hint ||
        coverageCheckBox.isSelected != coverage ||
        coverageIntensitySpinner.getValue.asInstanceOf[Int] != coverageIntensity ||
        hintMaxColumnTextField.getText != hintMaxColumn.toString ||
        hintUnicodeCheckBox.isSelected != hintUnicode ||
        hintLinesFreshCheckBox.isSelected != hintLinesFresh ||
        inscribeSummoningsCheckBox.isSelected != inscribeSummonings ||
        smt2CacheCheckBox.isSelected != smt2Cache ||
        smt2SeqCheckBox.isSelected != smt2Seq ||
        smt2SimplifyCheckBox.isSelected != smt2Simplify ||
        selectedBitWidth != bitWidth ||
        loopBoundTextField.getText != loopBound.toString ||
        callBoundTextField.getText != callBound.toString ||
        useRealCheckBox.isSelected != useReal ||
        selectedFPRoundingMode != fpRoundingMode ||
        smt2ValidConfigsTextArea.getText != smt2ValidOpts ||
        smt2SatConfigsTextArea.getText != smt2SatOpts ||
        selectedBranchPar != branchPar ||
        branchParCoresSpinner.getValue.asInstanceOf[Int] != branchParCores ||
        splitConditionalsCheckBox.isSelected != splitConds ||
        splitMatchCasesCheckBox.isSelected != splitMatchCases ||
        splitContractCasesCheckBox.isSelected != splitContractCases ||
        interpContractCheckBox.isSelected != interpContracts ||
        selectedStrictPureMode != strictPureMode ||
        infoFlowCheckBox.isSelected != infoFlow ||
        rawInscriptionCheckBox.isSelected != rawInscription ||
        elideEncodingCheckBox.isSelected != elideEncoding ||
        transitionCacheCheckBox.isSelected != transitionCache ||
        patternExhaustiveCheckBox.isSelected != patternExhaustive ||
        pureFunCheckBox.isSelected != pureFun ||
        detailedInfoCheckBox.isSelected != detailedInfo ||
        satTimeoutCheckBox.isSelected != satTimeout)

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

  private def selectedBranchPar: org.sireum.logika.Config.BranchPar.Type = {
    if (branchParDisabledRadioButton.isSelected) org.sireum.logika.Config.BranchPar.Disabled
    else if (branchParReturnsRadioButton.isSelected) org.sireum.logika.Config.BranchPar.OnlyAllReturns
    else if (branchParAllRadioButton.isSelected) org.sireum.logika.Config.BranchPar.All
    else sys.error("Unexpected branch par")
  }

  private def selectedStrictPureMode: org.sireum.logika.Config.StrictPureMode.Type = {
    if (spModeDefaultRadioButton.isSelected) org.sireum.logika.Config.StrictPureMode.Default
    else if (spModeFlipRadioButton.isSelected) org.sireum.logika.Config.StrictPureMode.Flip
    else if (spModeUninterpretedRadioButton.isSelected) org.sireum.logika.Config.StrictPureMode.Uninterpreted
    else sys.error("Unexpected strictpure mode")
  }

  override def createComponent(): JComponent = {
    def updateRLimit(): Unit = {
      val text = rlimitTextField.getText
      validRLimit = parseGe(text, 0).nonEmpty
      rlimitLabel.setForeground(if (validRLimit) fgColor else JBColor.red)
      rlimitTextField.setToolTipText(if (validRLimit) "OK" else "Must be at least 0.")
    }

    def updateTimeout(): Unit = {
      val text = timeoutTextField.getText
      validTimeout = parseGe(text, 200).nonEmpty
      timeoutLabel.setForeground(if (validTimeout) fgColor else JBColor.red)
      timeoutTextField.setToolTipText(if (validTimeout) "OK" else "Must be at least 200.")
    }

    def updateHintMaxColumn(): Unit = {
      val text = hintMaxColumnTextField.getText
      validHintMaxColumn = parseGe(text, 0).nonEmpty
      hintMaxColumnLabel.setForeground(if (validHintMaxColumn) fgColor else JBColor.red)
      hintMaxColumnTextField.setToolTipText(if (validHintMaxColumn) "OK" else "Must be at least 0.")
    }

    def updateLoopBound(): Unit = {
      val text = loopBoundTextField.getText
      validLoopBound = parsePosInteger(text).nonEmpty
      loopBoundLabel.setForeground(if (validLoopBound) fgColor else JBColor.red)
      loopBoundTextField.setToolTipText(if (validLoopBound) "OK" else "Must be at least 1.")
    }
    def updateRecursionBound(): Unit = {
      val text = callBoundTextField.getText
      validRecursionBound = parsePosInteger(text).nonEmpty
      callBoundLabel.setForeground(if (validRecursionBound) fgColor else JBColor.red)
      callBoundTextField.setToolTipText(if (validRecursionBound) "OK" else "Must be at least 1.")
    }
    def updateSymExe(): Unit = {
//      val isUnrolling = unrollingSymExeRadioButton.isSelected
      val isSymExe = true // symExeRadioButton.isSelected || isUnrolling
      bitsLabel.setEnabled(isSymExe)
      bitsUnboundedRadioButton.setEnabled(isSymExe)
      // TODO
      //bits8RadioButton.setEnabled(isSymExe)
      //bits16RadioButton.setEnabled(isSymExe)
      //bits32RadioButton.setEnabled(isSymExe)
      //bits64RadioButton.setEnabled(isSymExe)
    }
    def updateHints(): Unit = {
      hintMaxColumnTextField.setEnabled(hintCheckBox.isSelected)
      hintUnicodeCheckBox.setEnabled(hintCheckBox.isSelected)
      hintLinesFreshCheckBox.setEnabled(hintCheckBox.isSelected)
    }

    def updateFPRoundingMode(): Unit = {
      val enabled = !useRealCheckBox.isSelected
      fpRNERadioButton.setEnabled(enabled)
      fpRNARadioButton.setEnabled(enabled)
      fpRTPRadioButton.setEnabled(enabled)
      fpRTNRadioButton.setEnabled(enabled)
      fpRTZRadioButton.setEnabled(enabled)
    }

    val nameExePathMap = org.sireum.HashMap.empty[org.sireum.String, org.sireum.String] ++ org.sireum.ISZ(
      org.sireum.String("alt-ergo-open") -> org.sireum.String("alt-ergo"),
      org.sireum.String("alt-ergo") -> org.sireum.String("alt-ergo"),
      org.sireum.String("cvc4") -> org.sireum.String("cvc4"),
      org.sireum.String("cvc5") -> org.sireum.String("cvc5"),
      org.sireum.String("z3") -> org.sireum.String("z3")
    )

    def updateSmt2ValidOpts(): Unit = {
      val text = smt2ValidConfigsTextArea.getText
      validSmt2ValidOpts = Smt2.parseConfigs(nameExePathMap, false, text).isLeft
      smt2ValidConfigsLabel.setForeground(if (validSmt2ValidOpts) fgColor else JBColor.red)
      smt2ValidConfigsTextArea.setToolTipText(if (validSmt2ValidOpts) "OK" else "Invalid configurations")
    }

    def updateSmt2SatOpts(): Unit = {
      val text = smt2SatConfigsTextArea.getText
      validSmt2SatOpts = Smt2.parseConfigs(nameExePathMap, true, text).isLeft
      smt2SatConfigsLabel.setForeground(if (validSmt2SatOpts) fgColor else JBColor.red)
      smt2SatConfigsTextArea.setToolTipText(if (validSmt2SatOpts) "OK" else "Invalid configurations")
    }

    def updateBranchPar(): Unit = {
      val enabled = !branchParDisabledRadioButton.isSelected
      branchParCoresLabel.setEnabled(enabled)
      branchParCoresSpinner.setEnabled(enabled)
    }

    def updateInfoFlow(): Unit = {
      val enabled = !infoFlowCheckBox.isSelected
      splitMatchCasesCheckBox.setEnabled(enabled)
      splitConditionalsCheckBox.setEnabled(enabled)
      splitContractCasesCheckBox.setEnabled(enabled)
    }

    def updateSummoning(): Unit = {
      val enabled = inscribeSummoningsCheckBox.isSelected
      rawInscriptionCheckBox.setEnabled(enabled)
      elideEncodingCheckBox.setEnabled(enabled)
    }

    logoLabel.setIcon(logo)

    reset()

    fgColor = logoLabel.getForeground

    rlimitTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateRLimit()

      override def changedUpdate(e: DocumentEvent): Unit = updateRLimit()

      override def removeUpdate(e: DocumentEvent): Unit = updateRLimit()
    })

    timeoutTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateTimeout()

      override def changedUpdate(e: DocumentEvent): Unit = updateTimeout()

      override def removeUpdate(e: DocumentEvent): Unit = updateTimeout()
    })

    hintMaxColumnTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateHintMaxColumn()

      override def changedUpdate(e: DocumentEvent): Unit = updateHintMaxColumn()

      override def removeUpdate(e: DocumentEvent): Unit = updateHintMaxColumn()
    })

    infoFlowCheckBox.addChangeListener(_ => updateInfoFlow())

    smt2ValidConfigsTextArea.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateSmt2ValidOpts()

      override def changedUpdate(e: DocumentEvent): Unit = updateSmt2ValidOpts()

      override def removeUpdate(e: DocumentEvent): Unit = updateSmt2ValidOpts()
    })

    smt2SatConfigsTextArea.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateSmt2SatOpts()

      override def changedUpdate(e: DocumentEvent): Unit = updateSmt2SatOpts()

      override def removeUpdate(e: DocumentEvent): Unit = updateSmt2SatOpts()
    })

    loopBoundTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateLoopBound()

      override def changedUpdate(e: DocumentEvent): Unit = updateLoopBound()

      override def removeUpdate(e: DocumentEvent): Unit = updateLoopBound()
    })

    callBoundTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateRecursionBound()

      override def changedUpdate(e: DocumentEvent): Unit = updateRecursionBound()

      override def removeUpdate(e: DocumentEvent): Unit = updateRecursionBound()
    })

//    symExeRadioButton.addChangeListener(_ => updateSymExe())
//    unrollingSymExeRadioButton.addChangeListener(_ => updateSymExe())
    hintCheckBox.addChangeListener(_ => updateHints())

    useRealCheckBox.addChangeListener(_ => updateFPRoundingMode())

    defaultSmt2ValidConfigsLabel.addMouseListener(new MouseListener {
      override def mouseClicked(mouseEvent: MouseEvent): Unit = {
        smt2ValidConfigsTextArea.setText(defaultSmt2ValidOpts)
      }
      override def mousePressed(mouseEvent: MouseEvent): Unit = {}
      override def mouseReleased(mouseEvent: MouseEvent): Unit = {}
      override def mouseEntered(mouseEvent: MouseEvent): Unit = {
        defaultSmt2ValidConfigsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
      }
      override def mouseExited(mouseEvent: MouseEvent): Unit = {
        defaultSmt2ValidConfigsLabel.setCursor(Cursor.getDefaultCursor)
      }
    })

    defaultSmt2SatConfigsLabel.addMouseListener(new MouseListener {
      override def mouseClicked(mouseEvent: MouseEvent): Unit = {
        smt2SatConfigsTextArea.setText(defaultSmt2SatOpts)
      }
      override def mousePressed(mouseEvent: MouseEvent): Unit = {}
      override def mouseReleased(mouseEvent: MouseEvent): Unit = {}
      override def mouseEntered(mouseEvent: MouseEvent): Unit = {
        defaultSmt2SatConfigsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
      }
      override def mouseExited(mouseEvent: MouseEvent): Unit = {
        defaultSmt2SatConfigsLabel.setCursor(Cursor.getDefaultCursor)
      }
    })

    inscribeSummoningsCheckBox.addChangeListener(_ => updateSummoning())

    branchParDisabledRadioButton.addChangeListener(_ => updateBranchPar())
    branchParReturnsRadioButton.addChangeListener(_ => updateBranchPar())
    branchParAllRadioButton.addChangeListener(_ => updateBranchPar())

    branchParCoresLabel.setText(s"CPU cores (max: ${SireumApplicationComponent.maxCores})")
    branchParCoresSpinner.setModel(new SpinnerNumberModel(branchParCores, 1, SireumApplicationComponent.maxCores, 1))
    coverageIntensitySpinner.setModel(new SpinnerNumberModel(coverageIntensity, 0, 255, 1))

    def updateCoverage(): Unit = {
      coverageIntensitySpinner.setEnabled(coverageCheckBox.isSelected)
    }
    coverageCheckBox.addChangeListener(_ => updateCoverage())

    updateSymExe()
    updateHints()
    updateHintMaxColumn()
    updateFPRoundingMode()
    updateBranchPar()
    updateSummoning()
    updateInfoFlow()
    updateCoverage()

    logikaPanel
  }

  override def disposeUIResources(): Unit = {}

  override def apply(): Unit = {
    backgroundAnalysis = backgroundCheckBox.isSelected
    rlimit = parseGe(rlimitTextField.getText, 0).getOrElse(rlimit)
    timeout = parseGe(timeoutTextField.getText, 200).getOrElse(timeout.toLong).toInt
    checkSat = checkSatCheckBox.isSelected
    hint = hintCheckBox.isSelected
    coverage = coverageCheckBox.isSelected
    coverageIntensity = coverageIntensitySpinner.getValue.asInstanceOf[Int]
    hintMaxColumn = parseGe(hintMaxColumnTextField.getText, 0).getOrElse(hintMaxColumn.toLong).intValue
    hintUnicode = hintUnicodeCheckBox.isSelected
    hintLinesFresh = hintLinesFreshCheckBox.isSelected
    inscribeSummonings = inscribeSummoningsCheckBox.isSelected
    smt2Cache = smt2CacheCheckBox.isSelected
    smt2Seq = smt2SeqCheckBox.isSelected
    smt2Simplify = smt2SimplifyCheckBox.isSelected
    bitWidth = selectedBitWidth
    loopBound = parsePosInteger(loopBoundTextField.getText).getOrElse(loopBound)
    callBound = parsePosInteger(callBoundTextField.getText).getOrElse(callBound)
    useReal = useRealCheckBox.isSelected
    fpRoundingMode = selectedFPRoundingMode
    smt2ValidOpts = parseSmt2Opts(smt2ValidConfigsTextArea.getText).getOrElse(smt2ValidOpts)
    smt2SatOpts = parseSmt2Opts(smt2SatConfigsTextArea.getText).getOrElse(smt2SatOpts)
    branchPar = selectedBranchPar
    branchParCores = branchParCoresSpinner.getValue.asInstanceOf[Int]
    splitConds = splitConditionalsCheckBox.isSelected
    splitMatchCases = splitMatchCasesCheckBox.isSelected
    splitContractCases = splitContractCasesCheckBox.isSelected
    interpContracts = interpContractCheckBox.isSelected
    strictPureMode = selectedStrictPureMode
    infoFlow = infoFlowCheckBox.isSelected
    rawInscription = rawInscriptionCheckBox.isSelected
    elideEncoding = elideEncodingCheckBox.isSelected
    transitionCache = transitionCacheCheckBox.isSelected
    patternExhaustive = patternExhaustiveCheckBox.isSelected
    pureFun = pureFunCheckBox.isSelected
    detailedInfo = detailedInfoCheckBox.isSelected
    satTimeout = satTimeoutCheckBox.isSelected
    saveConfiguration()
  }

  override def reset(): Unit = {
    backgroundCheckBox.setSelected(backgroundAnalysis)
    rlimitTextField.setText(rlimit.toString)
    timeoutTextField.setText(timeout.toString)
    checkSatCheckBox.setSelected(checkSat)
    hintCheckBox.setSelected(hint)
    coverageCheckBox.setSelected(coverage)
    coverageIntensitySpinner.setValue(coverageIntensity)
    hintMaxColumnTextField.setText(hintMaxColumn.toString)
    hintUnicodeCheckBox.setSelected(hintUnicode)
    hintLinesFreshCheckBox.setSelected(hintLinesFresh)
    inscribeSummoningsCheckBox.setSelected(inscribeSummonings)
    smt2CacheCheckBox.setSelected(smt2Cache)
    smt2SeqCheckBox.setSelected(smt2Seq)
    smt2SimplifyCheckBox.setSelected(smt2Simplify)
    bitWidth match {
      case 0 => bitsUnboundedRadioButton.setSelected(true)
      case 8 => bits8RadioButton.setSelected(true)
      case 16 => bits16RadioButton.setSelected(true)
      case 32 => bits32RadioButton.setSelected(true)
      case 64 => bits64RadioButton.setSelected(true)
    }
    loopBoundTextField.setText(loopBound.toString)
    callBoundTextField.setText(callBound.toString)
    useRealCheckBox.setSelected(useReal)
    fpRoundingMode match {
      case "RNE" => fpRNERadioButton.setSelected(true)
      case "RNA" => fpRNARadioButton.setSelected(true)
      case "RTP" => fpRTPRadioButton.setSelected(true)
      case "RTN" => fpRTNRadioButton.setSelected(true)
      case "RTZ" => fpRTZRadioButton.setSelected(true)
    }
    smt2ValidConfigsTextArea.setText(smt2ValidOpts)
    smt2SatConfigsTextArea.setText(smt2SatOpts)
    branchPar match {
      case org.sireum.logika.Config.BranchPar.Disabled => branchParDisabledRadioButton.setSelected(true)
      case org.sireum.logika.Config.BranchPar.OnlyAllReturns => branchParReturnsRadioButton.setSelected(true)
      case org.sireum.logika.Config.BranchPar.All => branchParAllRadioButton.setSelected(true)
    }
    branchParCoresSpinner.setValue(branchParCores)
    splitConditionalsCheckBox.setSelected(splitConds)
    splitMatchCasesCheckBox.setSelected(splitMatchCases)
    splitContractCasesCheckBox.setSelected(splitContractCases)
    interpContractCheckBox.setSelected(interpContracts)
    strictPureMode match {
      case org.sireum.logika.Config.StrictPureMode.Default => spModeDefaultRadioButton.setSelected(true)
      case org.sireum.logika.Config.StrictPureMode.Flip => spModeFlipRadioButton.setSelected(true)
      case org.sireum.logika.Config.StrictPureMode.Uninterpreted => spModeUninterpretedRadioButton.setSelected(true)
    }
    infoFlowCheckBox.setSelected(infoFlow)
    rawInscriptionCheckBox.setSelected(rawInscription)
    elideEncodingCheckBox.setSelected(elideEncoding)
    transitionCacheCheckBox.setSelected(transitionCache)
    patternExhaustiveCheckBox.setSelected(patternExhaustive)
    pureFunCheckBox.setSelected(pureFun)
    detailedInfoCheckBox.setSelected(detailedInfo)
    satTimeoutCheckBox.setSelected(satTimeout)
  }
}
