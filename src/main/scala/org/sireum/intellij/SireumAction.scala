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
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.sireum.intellij.hamr.HAMRCodeGenForm

import java.awt.Color
import java.awt.event.{ActionEvent, ActionListener, KeyEvent, KeyListener}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.Document
import javax.swing.{JComponent, JDialog, JFrame, JLabel, JTextField, KeyStroke}

object SireumAction {
  val infoTitle: String = "Sireum Info"
  val warningTitle: String = "Sireum Warning"
  val errorTitle: String = "Sireum Error"
}

trait SireumAction extends AnAction

final class SlangCheckActionFile extends SireumAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null &&
      Util.isSireumOrLogikaFile(project)(org.sireum.String(editor.getDocument.getText))._1)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(false)
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
    if (editor == null) return
    SireumClient.enableEditor(project, file, editor)
    SireumClient.analyze(isSlang = true, project, file, editor, 0, isBackground = false, isInterprocedural = false, typeCheckOnly = true)
    e.getPresentation.setEnabled(true)
  }
}

final class SysMLv2CheckActionFile extends SireumAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null &&
      Util.isSysMLv2File(project))
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(false)
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
    if (editor == null) return
    SireumClient.enableEditor(project, file, editor)
    SireumClient.analyze(isSlang = false, project, file, editor, 0, isBackground = false, isInterprocedural = false, typeCheckOnly = true)
    e.getPresentation.setEnabled(true)
  }
}

final class ConfigureHAMRCodeGenAction extends SireumAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null &&
      Util.isSysMLv2File(project))
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(false)
    val title = "Configure HAMR CodeGen"
    val dialog = new JDialog(new JFrame(title), title, true)
    val f = new HAMRCodeGenForm()
    val labelColor = f.outputLabel.getForeground
    def isValid: Boolean = {
      f.platformComboBox.getSelectedItem.toString match {
        case "JVM" =>
          f.outputLabel.getForeground != Color.RED &&
            f.stringSizeLabel.getForeground != Color.RED &&
            f.seqSizeLabel.getForeground != Color.RED
        case "macOS" | "Linux" | "Cygwin" =>
          f.outputLabel.getForeground != Color.RED &&
            f.stringSizeLabel.getForeground != Color.RED &&
            f.seqSizeLabel.getForeground != Color.RED &&
            f.cOutputLabel.getForeground != Color.RED &&
            f.auxLabel.getForeground != Color.RED &&
            f.seL4OutputLabel.getForeground != Color.RED &&
            f.auxSeL4Label.getForeground != Color.RED
        case "seL4_Only" | "seL4_TB" =>
            f.seL4OutputLabel.getForeground != Color.RED &&
            f.auxSeL4Label.getForeground != Color.RED
      }
    }
    def updateLabel(pred: () => Boolean, label: JLabel): Unit = {
      if (pred()) {
        label.setForeground(labelColor)
      } else {
        label.setForeground(Color.RED)
      }
      f.okButton.setEnabled(isValid)
    }
    def addChangeListener(d: Document, f: () => Unit): Unit = {
      d.addDocumentListener(new DocumentListener {
        override def insertUpdate(e: DocumentEvent): Unit = f()
        override def removeUpdate(e: DocumentEvent): Unit = f()
        override def changedUpdate(e: DocumentEvent): Unit = f()
      })
    }
    def isPosLongOpt(lOpt: Option[Long]): Boolean = {
      lOpt match {
        case Some(l) => l > 0
        case _ => false
      }
    }
    def updateOutput(): Unit = updateLabel(() => org.sireum.Os.path(f.outputTextField.getText).isWritable, f.outputLabel)
    def updatePackage(): Unit = updateLabel(() => {
      val text = f.packageTextField.getText
      text.nonEmpty &&
        text.headOption.forall(Character.isJavaIdentifierStart) &&
        text.tail.forall(Character.isJavaIdentifierPart)
    }, f.packageLabel)
    def updateSeqSize(): Unit = updateLabel(() => isPosLongOpt(f.seqSizeTextField.getText.toLongOption), f.seqSizeLabel)
    def updateStringSize(): Unit = updateLabel(() => isPosLongOpt(f.stringSizeTextField.getText.toLongOption), f.stringSizeLabel)
    def updateCOutput(): Unit = updateLabel(() => org.sireum.Os.path(f.cOutputTextField.getText).isWritable, f.cOutputLabel)
    def updateAux(): Unit = updateLabel(() => f.auxTextField.getText.isEmpty || org.sireum.Os.path(f.auxTextField.getText).exists, f.auxLabel)
    def updateSeL4Output(): Unit = updateLabel(() => org.sireum.Os.path(f.seL4OutputTextField.getText).isWritable, f.seL4OutputLabel)
    def updateAuxSeL4(): Unit = updateLabel(() => f.auxSeL4TextField.getText.isEmpty || org.sireum.Os.path(f.auxSeL4TextField.getText).exists, f.auxSeL4Label)
    updateOutput()
    updatePackage()
    updateSeqSize()
    updateStringSize()
    updateCOutput()
    updateAux()
    updateSeL4Output()
    updateAuxSeL4()
    addChangeListener(f.outputTextField.getDocument, updateOutput _)
    addChangeListener(f.packageTextField.getDocument, updatePackage _)
    addChangeListener(f.seqSizeTextField.getDocument, updateSeqSize _)
    addChangeListener(f.stringSizeTextField.getDocument, updateStringSize _)
    addChangeListener(f.cOutputTextField.getDocument, updateCOutput _)
    addChangeListener(f.auxTextField.getDocument, updateAux _)
    addChangeListener(f.seL4OutputTextField.getDocument, updateSeL4Output _)
    addChangeListener(f.auxSeL4TextField.getDocument, updateAuxSeL4 _)
    f.platformComboBox.addActionListener((_: ActionEvent) => {
      f.platformComboBox.getSelectedItem.toString match {
        case "JVM" =>
          f.outputLabel.setVisible(true)
          f.outputTextField.setVisible(true)
          f.browseOutputButton.setVisible(true)
          f.packageLabel.setVisible(true)
          f.packageTextField.setVisible(true)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(false)
          f.camkesPanel.setVisible(false)
        case "macOS" | "Linux" | "Cygwin" =>
          f.outputLabel.setVisible(true)
          f.outputTextField.setVisible(true)
          f.browseOutputButton.setVisible(true)
          f.packageLabel.setVisible(true)
          f.packageTextField.setVisible(true)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(true)
          f.camkesPanel.setVisible(false)
        case "seL4" =>
          f.outputLabel.setVisible(true)
          f.outputTextField.setVisible(true)
          f.browseOutputButton.setVisible(true)
          f.packageLabel.setVisible(true)
          f.packageTextField.setVisible(true)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(true)
          f.camkesPanel.setVisible(true)
        case "seL4_Only" | "seL4_TB" =>
          f.outputLabel.setVisible(false)
          f.outputTextField.setVisible(false)
          f.browseOutputButton.setVisible(false)
          f.packageLabel.setVisible(false)
          f.packageTextField.setVisible(false)
          f.runtimeMonitoringCheckBox.setVisible(true)
          f.transpilerPanel.setVisible(false)
          f.camkesPanel.setVisible(true)
      }
    })
    dialog.add(f.contentPanel)
    dialog.pack()
    f.platformComboBox.setSelectedIndex(0)
    f.bitWidthComboBox.setSelectedItem("32")
    dialog.setLocationRelativeTo(null)
    f.contentPanel.registerKeyboardAction((_: ActionEvent) => dialog.dispose(),
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW)
    f.okButton.addActionListener((_: ActionEvent) => {
      // TODO for Jason: generate and insert cli options
      dialog.dispose()
    })
    f.cancelButton.addActionListener((_: ActionEvent) => {
      dialog.dispose()
    })
    dialog.setVisible(true)
    e.getPresentation.setEnabled(true)
  }
}


trait SireumOnlyAction extends SireumAction {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor != null) e.getPresentation.setEnabledAndVisible(project != null &&
      Util.isSireumOrLogikaFile(project)(org.sireum.String(editor.getDocument.getText))._1)
  }
}

object SireumOnlyAction {
  def processSlangRewriteResponse(resp: org.sireum.server.protocol.Slang.Rewrite.Response,
                                  project: Project, editor: Editor): Unit = {
    WriteCommandAction.runWriteCommandAction(project,
      (() => {
        if (editor != null && !editor.isDisposed) {
          val (nt, title, msg) = resp.message.level match {
            case org.sireum.message.Level.Info =>
              resp.newTextOpt match {
                case org.sireum.Some(newText) if resp.numOfRewrites =!= 0 =>
                  editor.getDocument.setText(newText.value)
                case _ =>
              }
              (NotificationType.INFORMATION, SireumAction.infoTitle, resp.message.text.value)
            case org.sireum.message.Level.Error => (NotificationType.ERROR, SireumAction.errorTitle, resp.message.text.value)
            case org.sireum.message.Level.InternalError => (NotificationType.ERROR, SireumAction.errorTitle, resp.message.text.value)
            case org.sireum.message.Level.Warning => (NotificationType.WARNING, SireumAction.warningTitle, resp.message.text.value)
          }
          Util.notify(new Notification(SireumClient.groupId, title, msg, nt), project, shouldExpire = true)
        }
      }): Runnable)
  }
}

trait SlangRewriteAction extends SireumOnlyAction {

  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val fileUriOpt: org.sireum.Option[org.sireum.String] = Util.getFilePath(project) match {
      case Some(p) => org.sireum.Some(p.canon.string)
      case _ => org.sireum.None()
    }
    val document = editor.getDocument
    val text = document.getText
    val isWorksheet = fileUriOpt match {
      case org.sireum.Some(fileUri) => !fileUri.value.endsWith(".scala") && !fileUri.value.endsWith(".slang")
      case _ => true
    }
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
    SireumClient.addRequest(id =>
      Vector(org.sireum.server.protocol.Slang.Rewrite.Request(id, kind, isWorksheet, fileUriOpt, text)),
      project, file, editor, isBackground = false, text, isInterprocedural = false
    )
  }
}

final class SlangInsertConstructorValsAction extends SlangRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.InsertConstructorVals
}

trait SlangTypedRewriteAction extends SlangRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    val fileUriOpt: org.sireum.Option[org.sireum.String] = Util.getFilePath(project) match {
      case Some(p) => org.sireum.Some(p.canon.toUri)
      case _ => org.sireum.None()
    }
    val document = editor.getDocument
    val text = document.getText
    val isWorksheet = fileUriOpt match {
      case org.sireum.Some(fileUri) => !fileUri.value.endsWith(".scala") && !fileUri.value.endsWith(".slang")
      case _ => true
    }
    val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)

    SireumClient.addRequest(id => Vector(
      if (isWorksheet) org.sireum.server.protocol.Slang.Check.Script(
        isBackground = false, false, id, org.sireum.Some(org.sireum.Os.path(project.getBasePath).string), fileUriOpt, text, 0,
        rewriteKindOpt = org.sireum.Some(kind)
      ) else org.sireum.server.protocol.Slang.Check.Project(
        isBackground = false, id, org.sireum.Os.path(project.getBasePath).string,
        org.sireum.HashSMap.empty[org.sireum.String, org.sireum.String] + fileUriOpt.get ~> text,
        org.sireum.ISZ(), 0, kind, fileUriOpt
      )
    ), project, file, editor, isBackground = false, text, isInterprocedural = false)
  }
}

final class SlangRenumberProofStepsAction extends SlangTypedRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.RenumberProofSteps
}

final class SlangExpandInduct extends SlangTypedRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.ExpandInduct
}

class SlangReplaceEnumSymbolsAction extends SlangRewriteAction {
  def kind: org.sireum.server.protocol.Slang.Rewrite.Kind.Type =
    org.sireum.server.protocol.Slang.Rewrite.Kind.ReplaceEnumSymbols
}

final class SlangReformatProofsAction extends SireumAction {
  final override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor == null) return
    val fileUriOpt: org.sireum.Option[org.sireum.String] = Util.getFilePath(project) match {
      case Some(p) => org.sireum.Some(p.canon.toUri)
      case _ => org.sireum.None()
    }
    val document = editor.getDocument
    val text = document.getText
    val isWorksheet = fileUriOpt match {
      case org.sireum.Some(fileUri) => !fileUri.value.endsWith(".scala") && !fileUri.value.endsWith(".slang")
      case _ => true
    }
    import org.sireum._
    try {
      lang.FrontEnd.reformatProof(isWorksheet, fileUriOpt, text) match {
        case Some((r, n)) =>
          if (n > 0 && text != r.value) {
            WriteCommandAction.runWriteCommandAction(project,
              (() => document.setText(r.value)): Runnable)
            Util.notify(new Notification(
              SireumClient.groupId, "Proofs reformatted",
              s"Program proofs have been reformatted with $n number of edit(s)",
              NotificationType.INFORMATION), project, shouldExpire = true)
          } else {
            Util.notify(new Notification(
              SireumClient.groupId, "Proofs well-formatted",
              s"Program proofs are already well-formatted",
              NotificationType.INFORMATION), project, shouldExpire = true)
          }
        case _ =>
          Util.notify(new Notification(
            SireumClient.groupId, "Ill-formed program",
            s"Cannot reformat proofs in ill-formed programs",
            NotificationType.ERROR), project, shouldExpire = true)
      }
    } catch {
      case e: Throwable =>
        val sw = new java.io.StringWriter()
        val pw = new java.io.PrintWriter(sw)
        e.printStackTrace(pw)
        Util.notify(new Notification(
          SireumClient.groupId, "Internal error", sw.toString,
          NotificationType.ERROR), project, shouldExpire = true)
    }
  }
}

trait SireumInsertSymbol extends SireumAction {
  def symbol: String

  final override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor == null) return
    val document = editor.getDocument
    WriteCommandAction.runWriteCommandAction(project,
      (() => {
        val caretModel = editor.getCaretModel
        val caret = caretModel.getPrimaryCaret
        if (caret.hasSelection) {
          document.replaceString(caret.getSelectionStart, caret.getSelectionEnd, symbol)
          caret.moveToOffset(caret.getSelectionStart + 1)
        } else {
          val offset = caret.getOffset
          document.insertString(offset, symbol)
          caret.moveToOffset(offset + 1)
        }
      }): Runnable)
  }
}

final class SireumInsertImply extends SireumInsertSymbol {
  val symbol: String = "__>:"
}

final class SireumInsertSimply extends SireumInsertSymbol {
  val symbol: String = "___>:"
}

final class SireumInsertForAll extends SireumInsertSymbol {
  val symbol: String = "∀"
}

final class SireumInsertExists extends SireumInsertSymbol {
  val symbol: String = "∃"
}

final class SireumInsertSequent extends SireumInsertSymbol {
  val symbol: String = "⊢"
}

final class SireumInsertEquiv extends SireumInsertSymbol {
  val symbol: String = "≡"
}

final class SireumInsertInequiv extends SireumInsertSymbol {
  val symbol: String = "≢"
}

final class SireumInsertUniSpace extends SireumInsertSymbol {
  val symbol: String = "␣"
}

import org.sireum.lang.{ast => AST}

trait SireumInsertProofStep extends SireumOnlyAction {
  def proofStep: String

  final override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = FileEditorManager.
      getInstance(project).getSelectedTextEditor
    if (editor == null) return
    val fileUriOpt: org.sireum.Option[org.sireum.String] = Util.getFilePath(project) match {
      case Some(p) => org.sireum.Some(p.canon.toUri)
      case _ => org.sireum.None()
    }
    val document = editor.getDocument
    val text = document.getText
    val isWorksheet = fileUriOpt match {
      case org.sireum.Some(fileUri) => !fileUri.value.endsWith(".scala") && !fileUri.value.endsWith(".slang")
      case _ => true
    }
    val caretModel = editor.getCaretModel
    val caret = caretModel.getPrimaryCaret
    val line = document.getLineNumber(caret.getOffset) + 1
    import org.sireum._
    try {
      lang.FrontEnd.insertProofStep(isWorksheet, fileUriOpt, text, proofStep, line) match {
        case Some(r) => WriteCommandAction.runWriteCommandAction(project, (() => {
          caret.removeSelection()
          document.setText(r.value)
        }): Runnable)
        case _ =>
          Util.notify(new Notification(
            SireumClient.groupId, "Could not insert proof step",
            s"Please navigate caret to a suitable place for proof step insertion",
            NotificationType.ERROR), project, shouldExpire = true)
      }
    } catch {
      case e: Throwable =>
        val sw = new java.io.StringWriter()
        val pw = new java.io.PrintWriter(sw)
        e.printStackTrace(pw)
        Util.notify(new Notification(
          SireumClient.groupId, "Internal error", sw.toString,
          NotificationType.ERROR), project, shouldExpire = true)
    }
  }
}

final class SireumInsertProofStepRegular extends SireumInsertProofStep {
  val proofStep: String = AST.Util.ProofStepTemplate.regular.value
}

final class SireumInsertProofStepAssume extends SireumInsertProofStep {
  val proofStep: String = AST.Util.ProofStepTemplate.assum.value
}

final class SireumInsertProofStepAssert extends SireumInsertProofStep {
  val proofStep: String = AST.Util.ProofStepTemplate.asser.value
}

final class SireumInsertProofStepSubProof extends SireumInsertProofStep {
  val proofStep: String = AST.Util.ProofStepTemplate.subProof.value
}

final class SireumInsertProofStepLet extends SireumInsertProofStep {
  val proofStep: String = AST.Util.ProofStepTemplate.let.value
}

final class SireumInsertQuantForAll extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.all.value
}

final class SireumInsertQuantExists extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.exists.value
}

final class SireumInsertQuantForAllRange extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.allRange.value
}

final class SireumInsertQuantExistsRange extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.existsRange.value
}

final class SireumInsertQuantForAllEach extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.allEach.value
}

final class SireumInsertQuantExistsEach extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.existsEach.value
}

final class SireumInsertQuantForAllEachIndices extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.allEachIndex.value
}

final class SireumInsertQuantExistsEachIndices extends SireumOnlyAction with SireumInsertSymbol {
  val symbol: String = AST.Util.ProofStepTemplate.existsEachIndex.value
}


