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
package org.sireum.intellij.hamr.sysml

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
import org.sireum.hamr.sysml._
import org.sireum.hamr.sysml.parser._

import _root_.java.util
import javax.swing._


object Icons {
  val SYSMLV2_ICON: Icon = IconLoader.getIcon("/icon/hamr-icon.png", getClass.getClassLoader)
}

object SysMLv2Language {
  val INSTANCE = new SysMLv2Language
}

class SysMLv2Language extends Language("SysMLv2") {
}

class SysMLv2SyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  override def getSyntaxHighlighter(project: Project, virtualFile: VirtualFile) = new SysMLv2SyntaxHighlighter
}

object SysMLv2SyntaxHighlighter {
  private val EMPTY_KEYS = new Array[TextAttributesKey](0)
  val ID: TextAttributesKey = createTextAttributesKey("SYSMLV2_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
  val KEYWORD: TextAttributesKey = createTextAttributesKey("SYSMLV2_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  val STRING: TextAttributesKey = createTextAttributesKey("SYSMLV2_STRING", DefaultLanguageHighlighterColors.STRING)
  val LINE_COMMENT: TextAttributesKey = createTextAttributesKey("SYSMLV2_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val BLOCK_COMMENT: TextAttributesKey = createTextAttributesKey("SYSMLV2_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
  val DOC_COMMENT: TextAttributesKey = createTextAttributesKey("SYSMLV2_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
  val CONSTANT: TextAttributesKey = createTextAttributesKey("SYSMLV2_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT)

  PSIElementTypeFactory.defineLanguageIElementTypes(SysMLv2Language.INSTANCE, SysMLv2Parser.VOCABULARY, SysMLv2Parser.ruleNames)

}

class SysMLv2SyntaxHighlighter extends SyntaxHighlighterBase {
  override def getHighlightingLexer: Lexer = {
    val lexer = new SysMLv2Lexer(null)
    new ANTLRLexerAdaptor(SysMLv2Language.INSTANCE, lexer)
  }

  override def getTokenHighlights(tokenType: IElementType): Array[TextAttributesKey] = {
    if (!tokenType.isInstanceOf[TokenIElementType]) return SysMLv2SyntaxHighlighter.EMPTY_KEYS
    val myType = tokenType.asInstanceOf[TokenIElementType]
    val ttype = myType.getANTLRTokenType
    var attrKey: TextAttributesKey = null
    ttype match {
      case SysMLv2Lexer.RULE_ID | SysMLv2Lexer.RULE_UNRESTRICTED_NAME => attrKey = SysMLv2SyntaxHighlighter.ID
      case SysMLv2Lexer.RULE_STRING_VALUE => attrKey = SysMLv2SyntaxHighlighter.STRING
      case SysMLv2Lexer.RULE_ML_NOTE => attrKey = SysMLv2SyntaxHighlighter.BLOCK_COMMENT
      case SysMLv2Lexer.RULE_SL_NOTE => attrKey = SysMLv2SyntaxHighlighter.LINE_COMMENT
      case SysMLv2Lexer.RULE_REGULAR_COMMENT => attrKey = SysMLv2SyntaxHighlighter.DOC_COMMENT
      case SysMLv2Lexer.RULE_DECIMAL_VALUE | SysMLv2Lexer.RULE_EXP_VALUE => attrKey = SysMLv2SyntaxHighlighter.CONSTANT
      case _ if SysMLv2Parser.isKeyword(ttype) => attrKey = SysMLv2SyntaxHighlighter.KEYWORD
      case _ => return SysMLv2SyntaxHighlighter.EMPTY_KEYS
    }
    Array[TextAttributesKey](attrKey)
  }
}


object SysMLv2ParserDefinition {
  val FILE: IFileElementType = new IFileElementType(SysMLv2Language.INSTANCE)
  val ID: TokenIElementType = try {
    PSIElementTypeFactory.defineLanguageIElementTypes(SysMLv2Language.INSTANCE, SysMLv2Parser.VOCABULARY, SysMLv2Parser.ruleNames)
    val tokenIElementTypes: util.List[TokenIElementType] = PSIElementTypeFactory.getTokenIElementTypes(SysMLv2Language.INSTANCE)
    tokenIElementTypes.get(SysMLv2Lexer.RULE_ID)
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      throw t
  }
  val COMMENTS: TokenSet = PSIElementTypeFactory.createTokenSet(SysMLv2Language.INSTANCE, SysMLv2Lexer.RULE_ML_NOTE, SysMLv2Lexer.RULE_SL_NOTE)
  val WHITESPACE: TokenSet = PSIElementTypeFactory.createTokenSet(SysMLv2Language.INSTANCE, SysMLv2Lexer.RULE_WS)
  val STRING: TokenSet = PSIElementTypeFactory.createTokenSet(SysMLv2Language.INSTANCE, SysMLv2Lexer.RULE_STRING_VALUE)
}

class SysMLv2ParserDefinition extends ParserDefinition {
  override def createLexer(project: Project): Lexer = {
    val lexer = new SysMLv2Lexer(null)
    new ANTLRLexerAdaptor(SysMLv2Language.INSTANCE, lexer)
  }

  override def createParser(project: Project): PsiParser = {
    val parser = new SysMLv2Parser(null)
    val r = new ANTLRParserAdaptor(SysMLv2Language.INSTANCE, parser) {
      override protected def parse(parser: Parser, root: IElementType): ParseTree = {
        if (root.isInstanceOf[IFileElementType]) return parser.asInstanceOf[SysMLv2Parser].entryRuleRootNamespace
        parser.asInstanceOf[SysMLv2Parser].rulePrimaryExpression
      }

      override protected def createListener(parser: Parser, root: IElementType, builder: PsiBuilder): ANTLRParseTreeToPSIConverter = new ANTLRParseTreeToPSIConverter(language, parser, builder)
    }
    r
  }

  override def getWhitespaceTokens: TokenSet = SysMLv2ParserDefinition.WHITESPACE

  override def getCommentTokens: TokenSet = SysMLv2ParserDefinition.COMMENTS

  override def getStringLiteralElements: TokenSet = SysMLv2ParserDefinition.STRING

  override def spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) = SpaceRequirements.MAY

  override def getFileNodeType: IFileElementType = SysMLv2ParserDefinition.FILE

  override def createFile(viewProvider: FileViewProvider) = new SysMLv2PSIFileRoot(viewProvider)

  override def createElement(node: ASTNode): PsiElement = new ANTLRPsiNode(node)
}


class SysMLv2FindUsagesProvider extends FindUsagesProvider {
  override def canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement.isInstanceOf[IdentifierPSINode]

  override def getWordsScanner: WordsScanner = null

  override def getHelpId(psiElement: PsiElement): String = null

  override def getType(element: PsiElement): String = ""

  override def getDescriptiveName(element: PsiElement): String = element.getText

  override def getNodeText(element: PsiElement, useFullName: Boolean): String = element.getText
}

class SysMLv2FileTypeFactory extends FileTypeFactory {
  override def createFileTypes(fileTypeConsumer: FileTypeConsumer): Unit = {
    fileTypeConsumer.consume(SysMLv2FileType.INSTANCE, SysMLv2FileType.FILE_EXTENSION)
  }
}

object SysMLv2FileType {
  val FILE_EXTENSION = "sysml"
  val INSTANCE = new SysMLv2FileType
}

class SysMLv2FileType protected extends LanguageFileType(SysMLv2Language.INSTANCE) {
  override def getName = "SysML"

  override def getDescription = "SysML v2 file"

  override def getDefaultExtension: String = SysMLv2FileType.FILE_EXTENSION

  override def getIcon: Icon = Icons.SYSMLV2_ICON
}

object SysMLv2ExternalAnnotator {
  class Issue(private[sysml] var msg: String, private[sysml] var offendingNode: PsiElement) {
  }
}

class SysMLv2ExternalAnnotator extends ExternalAnnotator[PsiFile, util.List[SysMLv2ExternalAnnotator.Issue]] {
  override def collectInformation(file: PsiFile): PsiFile = file

  override def doAnnotate(file: PsiFile): util.List[SysMLv2ExternalAnnotator.Issue] = {
    val issues = new util.ArrayList[SysMLv2ExternalAnnotator.Issue]
    issues
  }

  override def apply(file: PsiFile, issues: util.List[SysMLv2ExternalAnnotator.Issue], holder: AnnotationHolder): Unit = {
    import scala.jdk.CollectionConverters._
    for (issue <- issues.asScala) {
      val range = issue.offendingNode.getTextRange
      holder.createErrorAnnotation(range, issue.msg)
    }
  }
}

object SysMLv2ColorSettingsPage {
  private val DESCRIPTORS = Array[AttributesDescriptor](new AttributesDescriptor("Identifier", SysMLv2SyntaxHighlighter.ID), new AttributesDescriptor("Keyword", SysMLv2SyntaxHighlighter.KEYWORD), new AttributesDescriptor("String", SysMLv2SyntaxHighlighter.STRING), new AttributesDescriptor("Line comment", SysMLv2SyntaxHighlighter.LINE_COMMENT), new AttributesDescriptor("Block comment", SysMLv2SyntaxHighlighter.BLOCK_COMMENT))
}

class SysMLv2ColorSettingsPage extends ColorSettingsPage {
  override def getAdditionalHighlightingTagToDescriptorMap: util.Map[String, TextAttributesKey] = null

  override def getIcon: Icon = Icons.SYSMLV2_ICON

  override def getHighlighter = new SysMLv2SyntaxHighlighter

  override def getDemoText: String =
    """library package Base_Types {
      |
      |	import AADL_Properties::*;
      |	import AADL_Data_Model::*;
      |	import SI::*;
      |
      |	alias Boolean for ScalarValues::Boolean;
      |
      |    alias Integer for ScalarValues::Integer;
      |
      |    datatype Integer_8 :> Integer {
      |    	@Properties {
      |    		Data_Size = 8;
      |    		Number_Representation = Signed;
      |    	}
      |    }
      |
      |    datatype Integer_16 :> Integer {
      |    	@Properties {
      |    		Data_Size = 16;
      |    		Number_Representation = Signed;
      |    	}
      |    }
      |
      |    datatype Integer_32 :> Integer {
      |    	@Properties {
      |    		Data_Size = 32;
      |    		Number_Representation = Signed;
      |    	}
      |    }
      |
      |    datatype Integer_64 :> Integer {
      |    	@Properties {
      |    		Data_Size = 64;
      |    		Number_Representation = Signed;
      |    	}
      |    }
      |
      |    datatype Unsigned_8 :> Integer {
      |    	@Properties {
      |    		Data_Size = 8;
      |    		Number_Representation = Unsigned;
      |    	}
      |    }
      |
      |    datatype Unsigned_16 :> Integer {
      |    	@Properties {
      |    		Data_Size = 16;
      |    		Number_Representation = Unsigned;
      |    	}
      |    }
      |
      |    datatype Unsigned_32 :> Integer {
      |    	@Properties {
      |    		Data_Size = 32;
      |    		Number_Representation = Unsigned;
      |    	}
      |    }
      |
      |    datatype Unsigned_64 :> Integer {
      |    	@Properties {
      |    		Data_Size = 64;
      |    		Number_Representation = Unsigned;
      |    	}
      |    }
      |
      |	datatype Natural :> Integer {
      |		@Properties {
      |			// TODO: how to properly express ranges in sysml
      |
      |			// TODO: AADL's high range is 2^32 but kerml apparently
      |			//   stores ScalarValues::Integer as signed 32 bit ints so
      |			//   2147483647 is the largest value allowed
      |
      |			Integer_Range = Range(0, 2147483647);
      |		}
      |	}
      |
      |    alias Float for ScalarValues::Rational;
      |
      |    datatype Float_32 :> Float {
      |    	@Properties {
      |    		Data_Size = 32;
      |    		IEEE754_Precision = Simple;
      |    	}
      |    }
      |
      |    datatype Float_64 :> Float {
      |    	@Properties {
      |    		Data_Size = 64;
      |    		IEEE754_Precision = Double;
      |    	}
      |    }
      |
      |    alias String for ScalarValues::String;
      |
      |    datatype Character;
      |}""".stripMargin

  override def getAttributeDescriptors: Array[AttributesDescriptor] = SysMLv2ColorSettingsPage.DESCRIPTORS

  override def getColorDescriptors: Array[ColorDescriptor] = ColorDescriptor.EMPTY_ARRAY

  override def getDisplayName = "SysMLv2"
}


class SysMLv2ASTFactory extends DefaultASTFactoryImpl {
  override def createComposite(t: IElementType): CompositeElement = super.createComposite(t)

  override def createLeaf(t: IElementType, text: CharSequence): LeafElement = {
    t match {
      case elementType: TokenIElementType if elementType.getANTLRTokenType == SysMLv2Lexer.RULE_ID =>
        return new IdentifierPSINode(t, text)
      case _ =>
    }
    val leaf = super.createLeaf(t, text)
    leaf
  }
}

class SysMLv2FoldingBuilder extends FoldingBuilderEx with DumbAware {
  override def buildFoldRegions(psiElement: PsiElement, document: Document, b: Boolean): Array[FoldingDescriptor] = {
    var r = Vector[FoldingDescriptor]()
    def rec(element: PsiElement): Unit = {
      import SysMLv2Lexer._
      val children = element.getChildren
      for (i <- 0 until children.length) {
        def searchRightIndex(rt: Int): Int = {
          for (j <- i + 1 until children.length) {
            children(j).getNode.getElementType match {
              case et: TokenIElementType if et.getANTLRTokenType == rt => return j
              case _ =>
            }
          }
          -1
        }
        rec(children(i))
        children(i).getNode.getElementType match {
          case et: TokenIElementType =>
            val t = et.getANTLRTokenType
            val j = t match {
              case LBRACE => searchRightIndex(RBRACE)
              case OP_SLASH_STAR_LBRACE => searchRightIndex(OP_RBRACE_STAR_SLASH)
              case _ => -1
            }
            if (j >= 0) {
              val left = children(i)
              val right = children(j)
              r = r :+ new FoldingDescriptor(left.getNode, new TextRange(left.getTextRange.getStartOffset,
                right.getTextRange.getEndOffset))
            }
          case _ =>
        }
      }
    }
    rec(psiElement)
    r.toArray
  }

  override def isCollapsedByDefault(astNode: ASTNode): Boolean = false

  override def getPlaceholderText(astNode: ASTNode): String = "..."

}