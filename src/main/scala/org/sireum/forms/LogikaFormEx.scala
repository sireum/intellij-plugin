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
package org.sireum.forms

import java.awt.{Color, Cursor}
import java.awt.event.{MouseEvent, MouseListener}
import javax.swing.{JComponent, SpinnerNumberModel}
import javax.swing.event.{DocumentEvent, DocumentListener}

object LogikaFormEx {

  trait Parameter[ParseConfigsResult] {
    def defaultTimeout: Int
    def defaultRLimit: Long
    def defaultSmt2ValidOpts: String
    def defaultSmt2SatOpts: String
    def parseConfigs(nameExePathMap: Map[String, String],
                     isSat: Boolean,
                     options: String): Either[ParseConfigsResult, String]
    def hasSolver(solver: String): Boolean
  }

  var backgroundAnalysis: Boolean = true
  var timeout: Int = 2000
  var rlimit: Long = 2000000
  var checkSat: Boolean = false
  var hint: Boolean = true
  var coverage: Boolean = true
  var coverageIntensity: Int = 20
  var hintMaxColumn: Int = 60
  var hintUnicode: Boolean = scala.util.Properties.isMac
  var hintAtRewrite: Boolean = true
  var hintLinesFresh: Boolean = false
  var inscribeSummonings: Boolean = true
  var bitWidth: Int = 0
  var loopBound: Int = 3
  var callBound: Int = 3
  var methodContract: Boolean = true
  var useReal: Boolean = false
  var fpRoundingMode: String = "RNE"
  var smt2ValidOpts: String = "cvc4,--full-saturate-quant; z3; cvc5,--full-saturate-quant"
  var smt2SatOpts: String = "z3"
  var smt2Cache: Boolean = true
  var smt2Seq: Boolean = false
  var smt2Simplify: Boolean = false
  var branchPar: String = "All"
  var branchParCores: Int = Runtime.getRuntime.availableProcessors
  var splitConds: Boolean = false
  var splitMatchCases: Boolean = false
  var splitContractCases: Boolean = false
  var interpContracts: Boolean = false
  var strictPureMode: String = "Default"
  var infoFlow: Boolean = false
  var rawInscription: Boolean = false
  var elideEncoding: Boolean = false
  var transitionCache: Boolean = true
  var patternExhaustive: Boolean = true
  var pureFun: Boolean = false
  var detailedInfo: Boolean = true
  var satTimeout: Boolean = false
  var auto: Boolean = true
  var searchPc: Boolean = false
  var rwTrace: Boolean = true
  var rwMax: Int = 100
  var rwPar: Boolean = true
  var rwEvalTrace: Boolean = true
  var validRLimit: Boolean = true
  var validTimeout: Boolean = true
  var validHintMaxColumn: Boolean = true
  var validLoopBound: Boolean = true
  var validRecursionBound: Boolean = true
  var validSmt2ValidOpts: Boolean = true
  var validSmt2SatOpts: Boolean = true
  var validRwMax: Boolean = true
  var fgColor: Color = _

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

}

import LogikaFormEx._

abstract class LogikaFormEx[T] extends LogikaForm {

  def parameter: Parameter[T]

  def isUIModified: Boolean =
    validTimeout && validRLimit && validHintMaxColumn && validSmt2ValidOpts && validSmt2SatOpts &&
      validLoopBound && validRecursionBound && validRwMax &&
      (backgroundCheckBox.isSelected != backgroundAnalysis ||
        rlimitTextField.getText != rlimit.toString ||
        timeoutTextField.getText != timeout.toString ||
        checkSatCheckBox.isSelected != checkSat ||
        hintCheckBox.isSelected != hint ||
        coverageCheckBox.isSelected != coverage ||
        coverageIntensitySpinner.getValue.asInstanceOf[Int] != coverageIntensity ||
        hintMaxColumnTextField.getText != hintMaxColumn.toString ||
        hintUnicodeCheckBox.isSelected != hintUnicode ||
        hintAtRewriteCheckBox.isSelected != hintAtRewrite ||
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
        satTimeoutCheckBox.isSelected != satTimeout ||
        modeAutoRadioButton.isSelected != auto ||
        searchPcCheckBox.isSelected != searchPc ||
        rwTraceCheckBox.isSelected != rwTrace ||
        rwMaxTextField.getText != rwMax.toString ||
        rwParCheckBox.isSelected != rwPar ||
        rwEvalTraceCheckBox.isSelected != rwEvalTrace)

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

  private def selectedBranchPar: String = {
    if (branchParDisabledRadioButton.isSelected) "Disabled"
    else if (branchParReturnsRadioButton.isSelected) "OnlyAllReturns"
    else if (branchParAllRadioButton.isSelected) "All"
    else sys.error("Unexpected branch par")
  }

  private def selectedStrictPureMode: String = {
    if (spModeDefaultRadioButton.isSelected) "Default"
    else if (spModeFlipRadioButton.isSelected) "Flip"
    else if (spModeUninterpretedRadioButton.isSelected) "Uninterpreted"
    else sys.error("Unexpected strictpure mode")
  }

  def init(): JComponent = {
    def updateRLimit(): Unit = {
      val text = rlimitTextField.getText
      validRLimit = parseGe(text, 0).nonEmpty
      rlimitLabel.setForeground(if (validRLimit) fgColor else Color.red)
      rlimitTextField.setToolTipText(if (validRLimit) "OK" else "Must be at least 0.")
    }

    def updateTimeout(): Unit = {
      val text = timeoutTextField.getText
      validTimeout = parseGe(text, 200).nonEmpty
      timeoutLabel.setForeground(if (validTimeout) fgColor else Color.red)
      timeoutTextField.setToolTipText(if (validTimeout) "OK" else "Must be at least 200.")
    }

    def updateHintMaxColumn(): Unit = {
      val text = hintMaxColumnTextField.getText
      validHintMaxColumn = parseGe(text, 0).nonEmpty
      hintMaxColumnLabel.setForeground(if (validHintMaxColumn) fgColor else Color.red)
      hintMaxColumnTextField.setToolTipText(if (validHintMaxColumn) "OK" else "Must be at least 0.")
    }

    def updateLoopBound(): Unit = {
      val text = loopBoundTextField.getText
      validLoopBound = parsePosInteger(text).nonEmpty
      loopBoundLabel.setForeground(if (validLoopBound) fgColor else Color.red)
      loopBoundTextField.setToolTipText(if (validLoopBound) "OK" else "Must be at least 1.")
    }
    def updateRecursionBound(): Unit = {
      val text = callBoundTextField.getText
      validRecursionBound = parsePosInteger(text).nonEmpty
      callBoundLabel.setForeground(if (validRecursionBound) fgColor else Color.red)
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

    val nameExePathMap = Map(
      "alt-ergo" -> "alt-ergo",
      "cvc4" -> "cvc4",
      "cvc5" -> "cvc5",
      "z3" ->   "z3"
    )

    def updateSmt2ValidOpts(): Unit = {
      val text = smt2ValidConfigsTextArea.getText
      validSmt2ValidOpts = parameter.parseConfigs(nameExePathMap, false, text).isLeft
      smt2ValidConfigsLabel.setForeground(if (validSmt2ValidOpts) fgColor else Color.red)
      smt2ValidConfigsTextArea.setToolTipText(if (validSmt2ValidOpts) "OK" else "Invalid configurations")
    }

    def updateSmt2SatOpts(): Unit = {
      val text = smt2SatConfigsTextArea.getText
      validSmt2SatOpts = parameter.parseConfigs(nameExePathMap, true, text).isLeft
      smt2SatConfigsLabel.setForeground(if (validSmt2SatOpts) fgColor else Color.red)
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

    def updateRwMax(): Unit = {
      val text = rwMaxTextField.getText
      validRwMax = parseGe(text, 1).nonEmpty
      rwMaxLabel.setForeground(if (validRwMax) fgColor else Color.red)
      rwMaxTextField.setToolTipText(if (validRwMax) "OK" else "Must be at least 1.")
    }

    updateUI()

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
        smt2ValidConfigsTextArea.setText(parameter.defaultSmt2ValidOpts)
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
        smt2SatConfigsTextArea.setText(parameter.defaultSmt2SatOpts)
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

    branchParCoresLabel.setText(s"CPU cores (max: ${Runtime.getRuntime.availableProcessors})")
    branchParCoresSpinner.setModel(new SpinnerNumberModel(branchParCores, 1, Runtime.getRuntime.availableProcessors, 1))
    coverageIntensitySpinner.setModel(new SpinnerNumberModel(coverageIntensity, 0, 255, 1))

    def updateCoverage(): Unit = {
      coverageIntensitySpinner.setEnabled(coverageCheckBox.isSelected)
    }
    coverageCheckBox.addChangeListener(_ => updateCoverage())

    rwMaxTextField.getDocument.addDocumentListener(new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit = updateRwMax()

      override def changedUpdate(e: DocumentEvent): Unit = updateRwMax()

      override def removeUpdate(e: DocumentEvent): Unit = updateRwMax()
    })

    updateSymExe()
    updateHints()
    updateHintMaxColumn()
    updateFPRoundingMode()
    updateBranchPar()
    updateSummoning()
    updateInfoFlow()
    updateCoverage()
    updateRwMax()

    logikaPanel
  }

  def updateState(): Unit = {
    backgroundAnalysis = backgroundCheckBox.isSelected
    rlimit = parseGe(rlimitTextField.getText, 0).getOrElse(rlimit)
    timeout = parseGe(timeoutTextField.getText, 200).getOrElse(timeout.toLong).toInt
    checkSat = checkSatCheckBox.isSelected
    hint = hintCheckBox.isSelected
    coverage = coverageCheckBox.isSelected
    coverageIntensity = coverageIntensitySpinner.getValue.asInstanceOf[Int]
    hintMaxColumn = parseGe(hintMaxColumnTextField.getText, 0).getOrElse(hintMaxColumn.toLong).intValue
    hintUnicode = hintUnicodeCheckBox.isSelected
    hintAtRewrite = hintAtRewriteCheckBox.isSelected
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
    auto = modeAutoRadioButton.isSelected
    searchPc = searchPcCheckBox.isSelected
    rwTrace = rwTraceCheckBox.isSelected
    rwMax = parsePosInteger(rwMaxTextField.getText).getOrElse(rwMax)
    rwPar = rwParCheckBox.isSelected
    rwEvalTrace = rwEvalTraceCheckBox.isSelected
  }

  def updateUI(): Unit = {
    backgroundCheckBox.setSelected(backgroundAnalysis)
    rlimitTextField.setText(rlimit.toString)
    timeoutTextField.setText(timeout.toString)
    checkSatCheckBox.setSelected(checkSat)
    hintCheckBox.setSelected(hint)
    coverageCheckBox.setSelected(coverage)
    coverageIntensitySpinner.setValue(coverageIntensity)
    hintMaxColumnTextField.setText(hintMaxColumn.toString)
    hintUnicodeCheckBox.setSelected(hintUnicode)
    hintAtRewriteCheckBox.setSelected(hintAtRewrite)
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
      case "Disabled" => branchParDisabledRadioButton.setSelected(true)
      case "OnlyAllReturns" => branchParReturnsRadioButton.setSelected(true)
      case "All" => branchParAllRadioButton.setSelected(true)
    }
    branchParCoresSpinner.setValue(branchParCores)
    splitConditionalsCheckBox.setSelected(splitConds)
    splitMatchCasesCheckBox.setSelected(splitMatchCases)
    splitContractCasesCheckBox.setSelected(splitContractCases)
    interpContractCheckBox.setSelected(interpContracts)
    strictPureMode match {
      case "Default" => spModeDefaultRadioButton.setSelected(true)
      case "Flip" => spModeFlipRadioButton.setSelected(true)
      case "Uninterpreted" => spModeUninterpretedRadioButton.setSelected(true)
    }
    infoFlowCheckBox.setSelected(infoFlow)
    rawInscriptionCheckBox.setSelected(rawInscription)
    elideEncodingCheckBox.setSelected(elideEncoding)
    transitionCacheCheckBox.setSelected(transitionCache)
    patternExhaustiveCheckBox.setSelected(patternExhaustive)
    pureFunCheckBox.setSelected(pureFun)
    detailedInfoCheckBox.setSelected(detailedInfo)
    satTimeoutCheckBox.setSelected(satTimeout)
    if (auto) {
      modeAutoRadioButton.setSelected(true)
    } else {
      modeManualRadioButton.setSelected(true)
    }
    searchPcCheckBox.setSelected(searchPc)
    rwTraceCheckBox.setSelected(rwTrace)
    rwMaxTextField.setText(rwMax.toString)
    rwParCheckBox.setSelected(rwPar)
    rwEvalTraceCheckBox.setSelected(rwEvalTrace)
  }

  def parseSmt2Opts(text: String): Option[String] = {
    if ("" != text.trim) {
      for (e <- text.split(';').map(_.trim)) {
        e.split(',').map(_.trim) match {
          case Array(solver, _*) if parameter.hasSolver(solver) =>
          case _ => return None
        }
      }
    }
    Some(text)
  }

}

