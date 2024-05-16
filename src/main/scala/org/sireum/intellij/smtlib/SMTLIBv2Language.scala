/*
 Copyright (c) 2023, Robby, Kansas State University
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
package org.sireum.intellij.smtlib

import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.lang._
import com.intellij.lang.annotation.{AnnotationHolder, ExternalAnnotator}
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.folding.{FoldingBuilderEx, FoldingDescriptor}
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Document, FoldingGroup}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes._
import com.intellij.openapi.options.colors.{AttributesDescriptor, ColorDescriptor, ColorSettingsPage}
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.util.{IconLoader, TextRange}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.tree.{CompositeElement, LeafElement}
import com.intellij.psi.tree.{IElementType, IFileElementType, TokenSet}
import com.intellij.psi.{FileViewProvider, PsiElement, PsiElementVisitor, PsiFile}
import org.antlr.intellij.adaptor.lexer.{ANTLRLexerAdaptor, PSIElementTypeFactory, TokenIElementType}
import org.antlr.intellij.adaptor.parser.{ANTLRParseTreeToPSIConverter, ANTLRParserAdaptor}
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.ParseTree
import org.sireum.smtlib._
import org.sireum.smtlib.parser._

import _root_.java.util
import javax.swing._


object Icons {
  val SMTLIBV2_ICON: Icon = IconLoader.getIcon("/icon/hamr-icon.png", getClass.getClassLoader)
}

object SMTLIBV2Language {
  val INSTANCE = new SMTLIBV2Language
}

class SMTLIBV2Language extends Language("SMTLIBv2") {
}

class SMTLIBV2SyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  override def getSyntaxHighlighter(project: Project, virtualFile: VirtualFile) = new SMTLIBV2SyntaxHighlighter
}

object SMTLIBV2SyntaxHighlighter {
  private val EMPTY_KEYS = new Array[TextAttributesKey](0)
  val ID: TextAttributesKey = createTextAttributesKey("SMTLIBV2_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
  val KEYWORD: TextAttributesKey = createTextAttributesKey("SMTLIBV2_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  val STRING: TextAttributesKey = createTextAttributesKey("SMTLIBV2_STRING", DefaultLanguageHighlighterColors.STRING)
  val LINE_COMMENT: TextAttributesKey = createTextAttributesKey("SMTLIBV2_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val BLOCK_COMMENT: TextAttributesKey = createTextAttributesKey("SMTLIBV2_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
  val DOC_COMMENT: TextAttributesKey = createTextAttributesKey("SMTLIBV2_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
  val CONSTANT: TextAttributesKey = createTextAttributesKey("SMTLIBV2_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT)

  PSIElementTypeFactory.defineLanguageIElementTypes(SMTLIBV2Language.INSTANCE, SMTLIBv2Parser.VOCABULARY, SMTLIBv2Parser.ruleNames)

}

class SMTLIBV2SyntaxHighlighter extends SyntaxHighlighterBase {
  override def getHighlightingLexer: Lexer = {
    val lexer = new SMTLIBv2Lexer(null)
    new ANTLRLexerAdaptor(SMTLIBV2Language.INSTANCE, lexer)
  }

  override def getTokenHighlights(tokenType: IElementType): Array[TextAttributesKey] = {
    if (!tokenType.isInstanceOf[TokenIElementType]) return SMTLIBV2SyntaxHighlighter.EMPTY_KEYS
    val myType = tokenType.asInstanceOf[TokenIElementType]
    val ttype = myType.getANTLRTokenType
    var attrKey: TextAttributesKey = null
    ttype match {
      case SMTLIBv2Lexer.QuotedSymbol | SMTLIBv2Lexer.UndefinedSymbol => attrKey = SMTLIBV2SyntaxHighlighter.ID
      case SMTLIBv2Lexer.String => attrKey = SMTLIBV2SyntaxHighlighter.STRING
      case SMTLIBv2Lexer.Comment => attrKey = SMTLIBV2SyntaxHighlighter.LINE_COMMENT
      case SMTLIBv2Lexer.Numeral | SMTLIBv2Lexer.Binary | SMTLIBv2Lexer.HexDecimal | SMTLIBv2Lexer.Decimal => attrKey = SMTLIBV2SyntaxHighlighter.CONSTANT
      case _ if isKeyword(ttype) => attrKey = SMTLIBV2SyntaxHighlighter.KEYWORD
      case _ => return SMTLIBV2SyntaxHighlighter.EMPTY_KEYS
    }
    Array[TextAttributesKey](attrKey)
  }

  def isKeyword(tokenType: Int): Boolean = {
    import SMTLIBv2Lexer._
    tokenType match {
      case PS_Bool |
           PS_ContinuedExecution |
           PS_Error |
           PS_False |
           PS_ImmediateExit |
           PS_Incomplete |
           PS_Logic |
           PS_Memout |
           PS_Not |
           PS_Sat |
           PS_Success |
           PS_Theory |
           PS_True |
           PS_Unknown |
           PS_Unsat |
           PS_Unsupported |
           CMD_Assert |
           CMD_CheckSat |
           CMD_CheckSatAssuming |
           CMD_DeclareConst |
           CMD_DeclareDatatype |
           CMD_DeclareDatatypes |
           CMD_DeclareFun |
           CMD_DeclareSort |
           CMD_DefineFun |
           CMD_DefineFunRec |
           CMD_DefineFunsRec |
           CMD_DefineSort |
           CMD_Echo |
           CMD_Exit |
           CMD_GetAssertions |
           CMD_GetAssignment |
           CMD_GetInfo |
           CMD_GetModel |
           CMD_GetOption |
           CMD_GetProof |
           CMD_GetUnsatAssumptions |
           CMD_GetUnsatCore |
           CMD_GetValue |
           CMD_Pop |
           CMD_Push |
           CMD_Reset |
           CMD_ResetAssertions |
           CMD_SetInfo |
           CMD_SetLogic |
           CMD_SetOption |
           PK_AllStatistics |
           PK_AssertionStackLevels |
           PK_Authors |
           PK_Category |
           PK_Chainable |
           PK_Definition |
           PK_DiagnosticOutputChannel |
           PK_ErrorBehaviour |
           PK_Extension |
           PK_Funs |
           PK_FunsDescription |
           PK_GlobalDeclarations |
           PK_InteractiveMode |
           PK_Language |
           PK_LeftAssoc |
           PK_License |
           PK_Name |
           PK_Named |
           PK_Notes |
           PK_Pattern |
           PK_PrintSuccess |
           PK_ProduceAssertions |
           PK_ProduceAssignments |
           PK_ProduceModels |
           PK_ProduceProofs |
           PK_ProduceUnsatAssumptions |
           PK_ProduceUnsatCores |
           PK_RandomSeed |
           PK_ReasonUnknown |
           PK_RegularOutputChannel |
           PK_ReproducibleResourceLimit |
           PK_RightAssoc |
           PK_SmtLibVersion |
           PK_Sorts |
           PK_SortsDescription |
           PK_Source |
           PK_Status |
           PK_Theories |
           PK_Values |
           PK_Verbosity |
           PK_Version |
           GRW_As |
           GRW_Binary |
           GRW_Decimal |
           GRW_Exclamation |
           GRW_Exists |
           GRW_Forall |
           GRW_Hexadecimal |
           GRW_Let |
           GRW_Match |
           GRW_Numeral |
           GRW_Par |
           GRW_String |
           GRW_Underscore => true
      case _ => false
    }
  }
}


object SMTLIBv2ParserDefinition {
  val FILE: IFileElementType = new IFileElementType(SMTLIBV2Language.INSTANCE)
  val ID: TokenIElementType = try {
    PSIElementTypeFactory.defineLanguageIElementTypes(SMTLIBV2Language.INSTANCE, SMTLIBv2Parser.VOCABULARY, SMTLIBv2Parser.ruleNames)
    val tokenIElementTypes: util.List[TokenIElementType] = PSIElementTypeFactory.getTokenIElementTypes(SMTLIBV2Language.INSTANCE)
    tokenIElementTypes.get(SMTLIBv2Lexer.UndefinedSymbol)
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  }
  val COMMENTS: TokenSet = PSIElementTypeFactory.createTokenSet(SMTLIBV2Language.INSTANCE, SMTLIBv2Lexer.Comment)
  val WHITESPACE: TokenSet = PSIElementTypeFactory.createTokenSet(SMTLIBV2Language.INSTANCE, SMTLIBv2Lexer.WS)
  val STRING: TokenSet = PSIElementTypeFactory.createTokenSet(SMTLIBV2Language.INSTANCE, SMTLIBv2Lexer.String)
}

class SMTLIBv2ParserDefinition extends ParserDefinition {
  override def createLexer(project: Project): Lexer = {
    val lexer = new SMTLIBv2Lexer(null)
    new ANTLRLexerAdaptor(SMTLIBV2Language.INSTANCE, lexer)
  }

  override def createParser(project: Project): PsiParser = {
    val parser = new SMTLIBv2Parser(null)
    val r = new ANTLRParserAdaptor(SMTLIBV2Language.INSTANCE, parser) {
      override protected def parse(parser: Parser, root: IElementType): ParseTree = {
        val r = if (root.isInstanceOf[IFileElementType]) parser.asInstanceOf[SMTLIBv2Parser].script
        else parser.asInstanceOf[SMTLIBv2Parser].command
        r
      }

      override protected def createListener(parser: Parser, root: IElementType, builder: PsiBuilder): ANTLRParseTreeToPSIConverter = new ANTLRParseTreeToPSIConverter(language, parser, builder)
    }
    r
  }

  override def getWhitespaceTokens: TokenSet = SMTLIBv2ParserDefinition.WHITESPACE

  override def getCommentTokens: TokenSet = SMTLIBv2ParserDefinition.COMMENTS

  override def getStringLiteralElements: TokenSet = SMTLIBv2ParserDefinition.STRING

  override def spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) = SpaceRequirements.MAY

  override def getFileNodeType: IFileElementType = SMTLIBv2ParserDefinition.FILE

  override def createFile(viewProvider: FileViewProvider) = new SMTLIBV2PSIFileRoot(viewProvider)

  override def createElement(node: ASTNode): PsiElement = new ANTLRPsiNode(node)
}


class SMTLIBV2FindUsagesProvider extends FindUsagesProvider {
  override def canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement.isInstanceOf[IdentifierPSINode]

  override def getWordsScanner: WordsScanner = null

  override def getHelpId(psiElement: PsiElement): String = null

  override def getType(element: PsiElement): String = ""

  override def getDescriptiveName(element: PsiElement): String = element.getText

  override def getNodeText(element: PsiElement, useFullName: Boolean): String = element.getText
}

class SMTLIBV2FileTypeFactory extends FileTypeFactory {
  override def createFileTypes(fileTypeConsumer: FileTypeConsumer): Unit = {
    fileTypeConsumer.consume(SMTLIBV2FileType.INSTANCE, SMTLIBV2FileType.FILE_EXTENSION)
  }
}

object SMTLIBV2FileType {
  val FILE_EXTENSION = "smt2"
  val INSTANCE = new SMTLIBV2FileType
}

class SMTLIBV2FileType protected extends LanguageFileType(SMTLIBV2Language.INSTANCE) {
  override def getName = "SMT-LIB"

  override def getDescription = "SMT-LIB v2 file"

  override def getDefaultExtension: String = SMTLIBV2FileType.FILE_EXTENSION

  override def getIcon: Icon = Icons.SMTLIBV2_ICON
}

object SMTLIBV2ExternalAnnotator {
  class Issue(private[smtlib] var msg: String, private[smtlib] var offendingNode: PsiElement) {
  }
}

class SMTLIBV2ExternalAnnotator extends ExternalAnnotator[PsiFile, util.List[SMTLIBV2ExternalAnnotator.Issue]] {
  override def collectInformation(file: PsiFile): PsiFile = file

  override def doAnnotate(file: PsiFile): util.List[SMTLIBV2ExternalAnnotator.Issue] = {
    val issues = new util.ArrayList[SMTLIBV2ExternalAnnotator.Issue]
    issues
  }

  override def apply(file: PsiFile, issues: util.List[SMTLIBV2ExternalAnnotator.Issue], holder: AnnotationHolder): Unit = {
    import scala.jdk.CollectionConverters._
    for (issue <- issues.asScala) {
      val range = issue.offendingNode.getTextRange
      holder.createErrorAnnotation(range, issue.msg)
    }
  }
}

object SMTLIBV2ColorSettingsPage {
  private val DESCRIPTORS = Array[AttributesDescriptor](new AttributesDescriptor("Identifier", SMTLIBV2SyntaxHighlighter.ID), new AttributesDescriptor("Keyword", SMTLIBV2SyntaxHighlighter.KEYWORD), new AttributesDescriptor("String", SMTLIBV2SyntaxHighlighter.STRING), new AttributesDescriptor("Line comment", SMTLIBV2SyntaxHighlighter.LINE_COMMENT), new AttributesDescriptor("Block comment", SMTLIBV2SyntaxHighlighter.BLOCK_COMMENT))
}

class SMTLIBV2ColorSettingsPage extends ColorSettingsPage {
  override def getAdditionalHighlightingTagToDescriptorMap: util.Map[String, TextAttributesKey] = null

  override def getIcon: Icon = Icons.SMTLIBV2_ICON

  override def getHighlighter = new SMTLIBV2SyntaxHighlighter

  override def getDemoText: String =
    """(set-logic ALL)
      |(exit)""".stripMargin

  override def getAttributeDescriptors: Array[AttributesDescriptor] = SMTLIBV2ColorSettingsPage.DESCRIPTORS

  override def getColorDescriptors: Array[ColorDescriptor] = ColorDescriptor.EMPTY_ARRAY

  override def getDisplayName = "SMTLIBV2"
}


class SMTLIBV2ASTFactory extends DefaultASTFactoryImpl {
  override def createComposite(t: IElementType): CompositeElement = super.createComposite(t)

  override def createLeaf(t: IElementType, text: CharSequence): LeafElement = {
    t match {
      case elementType: TokenIElementType if elementType.getANTLRTokenType == SMTLIBv2Lexer.QuotedSymbol || elementType.getANTLRTokenType == SMTLIBv2Lexer.UndefinedSymbol =>
        return new IdentifierPSINode(t, text)
      case _ =>
    }
    val leaf = super.createLeaf(t, text)
    leaf
  }
}

class SMTLIBV2FoldingBuilder extends FoldingBuilderEx with DumbAware {
  override def buildFoldRegions(psiElement: PsiElement, document: Document, b: Boolean): Array[FoldingDescriptor] = {
    var r = Vector[FoldingDescriptor]()
    for (e <- psiElement.getChildren if e.getNode.getElementType.toString == "script") {
      for (command <- e.getChildren) {
        val children = command.getChildren
        val left = children(0)
        val right = children(children.length - 1)
        r = r :+ new FoldingDescriptor(left, new TextRange(left.getTextRange.getStartOffset,
          right.getTextRange.getEndOffset))
      }
    }
    r.toArray
  }

  override def isCollapsedByDefault(astNode: ASTNode): Boolean = false

  override def getPlaceholderText(astNode: ASTNode): String = {
    val text = astNode.getTreeParent.getText
    var r = Vector[Char]()
    var i = 0
    val len = text.length
    while (i < len) {
      val c = text(i)
      if (c.isWhitespace) {
        i = len
      } else {
        r = r :+ c
        i = i + 1
      }
    }
    r = r ++ Vector(' ', '.', '.', '.')
    r = r :+ text(len - 1)
    r.mkString
  }

}