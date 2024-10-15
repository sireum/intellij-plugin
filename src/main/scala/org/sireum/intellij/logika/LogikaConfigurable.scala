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

import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.{Notification, NotificationListener, NotificationType}
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.IconLoader
import org.sireum.forms.LogikaFormEx
import org.sireum.intellij.{SireumClient, Util}
import org.sireum.logika.{Smt2, Smt2Config}
import org.sireum.forms.LogikaFormEx._

object LogikaConfigurable {
  object Parameter extends LogikaFormEx.Parameter[org.sireum.ISZ[org.sireum.logika.Smt2Config]] {
    lazy val defaultSmt2ValidOpts: String = Smt2.defaultValidOpts.value.split(';').map(_.trim).mkString(";\n")
    lazy val defaultSmt2SatOpts: String = Smt2.defaultSatOpts.value.split(';').map(_.trim).mkString(";\n")
    def defaultTimeout: Int = Smt2.validTimeoutInMs.toInt
    def defaultRLimit: Long = Smt2.rlimit.toInt
    def parseConfigs(nameExePathMap: Map[String, String],
                     isSat: Boolean,
                     options: String): Either[org.sireum.ISZ[org.sireum.logika.Smt2Config], String] = {
      val map = org.sireum.HashMap.empty[org.sireum.String, org.sireum.String] ++ org.sireum.ISZ(
        (for ((k, v) <- nameExePathMap.toSeq) yield (org.sireum.String(k), org.sireum.String(v))): _*
      )
      org.sireum.logika.Smt2.parseConfigs(map, isSat, options) match {
        case org.sireum.Either.Left(l) => Left(l)
        case org.sireum.Either.Right(r) => Right(r.value)
      }
    }
    def hasSolver(solver: String): Boolean = org.sireum.logika.Smt2.solverArgsMap.contains(solver)
  }

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
  private val hintAtRewriteKey = logikaKey + "hintAtRewrite"
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
  private val autoKey = logikaKey + "auto"
  private val searchPcKey = logikaKey + "searchPc"
  private val rwTraceKey = logikaKey + "rw.trace"
  private val rwMaxKey = logikaKey + "rw.max"
  private val rwParKey = logikaKey + "rw.par"
  private val rwEvalTraceKey = logikaKey + "rw.evalTrace"

  def loadConfiguration[T](parameter: LogikaFormEx.Parameter[T]): Unit = {
    val pc = PropertiesComponent.getInstance
    backgroundAnalysis = pc.getBoolean(backgroundAnalysisKey, backgroundAnalysis)
    timeout = pc.getInt(timeoutKey, timeout)
    rlimit = pc.getLong(rlimitKey, rlimit)
    checkSat = pc.getBoolean(checkSatKey, checkSat)
    hint = pc.getBoolean(hintKey, hint)
    coverage = pc.getBoolean(coverageKey, coverage)
    coverageIntensity = pc.getInt(coverageIntensityKey, coverageIntensity)
    hintMaxColumn = pc.getInt(hintMaxColumnKey, hintMaxColumn)
    hintUnicode = pc.getBoolean(hintUnicodeKey, hintUnicode)
    hintAtRewrite = pc.getBoolean(hintAtRewriteKey, hintAtRewrite)
    hintLinesFresh = pc.getBoolean(hintLinesFreshKey, hintLinesFresh)
    inscribeSummonings = pc.getBoolean(inscribeSummoningsKey, inscribeSummonings)
    smt2Cache = pc.getBoolean(smt2CacheOptsKey, smt2Cache)
    smt2Seq = pc.getBoolean(smt2SeqOptsKey, smt2Seq)
    smt2Simplify = pc.getBoolean(smt2SimplifyKey, smt2Simplify)
    val defaultOpts = parameter.defaultSmt2ValidOpts + ";" + parameter.defaultSmt2SatOpts + ";" + Smt2.validTimeoutInMs + ";" + Smt2.rlimit
    if (defaultOpts != pc.getValue(smt2DefaultConfigsKey, defaultOpts)) {
      Util.notify(new Notification(SireumClient.groupId, "Update Logika SMT2 default configurations?",
        """<p>Logika SMT2 default configurations have changed. <a href="">Update</a>?</p>""",
        NotificationType.INFORMATION, new NotificationListener.Adapter {
          override def hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent): Unit = {
            smt2ValidOpts = parameter.defaultSmt2ValidOpts
            smt2SatOpts = parameter.defaultSmt2SatOpts
            timeout = Smt2.validTimeoutInMs.toInt
            rlimit = Smt2.rlimit.toLong
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
    branchPar = org.sireum.logika.Config.BranchPar.byOrdinal(pc.getInt(branchParKey, org.sireum.logika.Config.BranchPar.byName(branchPar).get.ordinal.toInt)).get.string.value
    branchParCores = pc.getInt(branchParCoresKey, branchParCores)
    splitConds = pc.getBoolean(splitCondsKey, splitConds)
    splitMatchCases = pc.getBoolean(splitMatchCasesKey, splitMatchCases)
    splitContractCases = pc.getBoolean(splitContractCasesKey, splitContractCases)
    interpContracts = pc.getBoolean(interpContractsKey, interpContracts)
    strictPureMode = org.sireum.logika.Config.StrictPureMode.byOrdinal(pc.getInt(strictPureModeKey, org.sireum.logika.Config.StrictPureMode.byName(strictPureMode).get.ordinal.toInt)).get.string.value
    infoFlow = pc.getBoolean(infoFlowKey, infoFlow)
    rawInscription = pc.getBoolean(rawInscriptionKey, rawInscription)
    elideEncoding = pc.getBoolean(elideEncodingKey, elideEncoding)
    transitionCache = pc.getBoolean(transitionCacheKey, transitionCache)
    patternExhaustive = pc.getBoolean(patternExhaustiveKey, patternExhaustive)
    pureFun = pc.getBoolean(pureFunKey, pureFun)
    detailedInfo = pc.getBoolean(detailedInfoKey, detailedInfo)
    satTimeout = pc.getBoolean(satTimeoutKey, satTimeout)
    auto = pc.getBoolean(autoKey, auto)
    searchPc = pc.getBoolean(searchPcKey, searchPc)
    rwTrace = pc.getBoolean(rwTraceKey, rwTrace)
    rwMax = pc.getInt(rwMaxKey, rwMax)
    rwPar = pc.getBoolean(rwParKey, rwPar)
    rwEvalTrace = pc.getBoolean(rwEvalTraceKey, rwEvalTrace)
    SireumClient.coverageTextAttributes.setBackgroundColor(SireumClient.createCoverageColor(coverageIntensity))
  }

  def saveConfiguration(): Unit = {
    val pc = PropertiesComponent.getInstance
    pc.setValue(backgroundAnalysisKey, backgroundAnalysis.toString)
    pc.setValue(rlimitKey, rlimit.toString)
    pc.setValue(timeoutKey, timeout.toString)
    pc.setValue(checkSatKey, checkSat.toString)
    pc.setValue(hintKey, hint.toString)
    pc.setValue(coverageKey, coverage.toString)
    pc.setValue(coverageIntensityKey, coverageIntensityKey)
    pc.setValue(hintMaxColumnKey, hintMaxColumn.toString)
    pc.setValue(hintUnicodeKey, hintUnicode.toString)
    pc.setValue(hintAtRewriteKey, hintAtRewrite.toString)
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
    pc.setValue(branchParKey, org.sireum.logika.Config.BranchPar.byName(branchPar).get.ordinal.toString)
    pc.setValue(branchParCoresKey, branchParCores.toString)
    pc.setValue(splitCondsKey, splitConds.toString)
    pc.setValue(splitMatchCasesKey, splitMatchCases.toString)
    pc.setValue(splitContractCasesKey, splitContractCases.toString)
    pc.setValue(interpContractsKey, interpContracts.toString)
    pc.setValue(strictPureModeKey, org.sireum.logika.Config.StrictPureMode.byName(strictPureMode).get.ordinal.toString)
    pc.setValue(infoFlowKey, infoFlow.toString)
    pc.setValue(rawInscriptionKey, rawInscription.toString)
    pc.setValue(elideEncodingKey, elideEncoding.toString)
    pc.setValue(transitionCacheKey, transitionCache.toString)
    pc.setValue(patternExhaustiveKey, patternExhaustive.toString)
    pc.setValue(pureFunKey, pureFun.toString)
    pc.setValue(detailedInfoKey, detailedInfo.toString)
    pc.setValue(satTimeoutKey, satTimeout.toString)
    pc.setValue(autoKey, auto.toString)
    pc.setValue(searchPcKey, searchPc.toString)
    pc.setValue(rwTraceKey, rwTrace.toString)
    pc.setValue(rwMaxKey, rwMax.toString)
    pc.setValue(rwParKey, rwPar.toString)
    pc.setValue(rwEvalTraceKey, rwEvalTrace.toString)
    SireumClient.coverageTextAttributes.setBackgroundColor(SireumClient.createCoverageColor(coverageIntensity))
  }
}

import LogikaConfigurable._

final class LogikaConfigurable extends LogikaFormEx[org.sireum.ISZ[org.sireum.logika.Smt2Config]] with Configurable {

  override def getDisplayName: String = "Logika"

  override def getHelpTopic: String = null

  override def disposeUIResources(): Unit = {}

  override def createComponent(): JComponent = {
    val r = init()
    logoLabel.setIcon(logo)
    r
  }

  override def apply(): Unit = {
    updateState()
    saveConfiguration()
  }

  override def reset(): Unit = {
    updateUI()
  }

  override def parameter: Parameter[_root_.org.sireum.ISZ[Smt2Config]] = LogikaConfigurable.Parameter

  override def isModified: Boolean = isUIModified
}
