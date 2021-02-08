/*
 Copyright (c) 2021 Robby, Kansas State University
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

package org.sireum.intellij.logika.lexer

import java.awt.Font

import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Editor}
import com.intellij.openapi.editor.markup.{HighlighterTargetArea, RangeHighlighter, TextAttributes}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.sireum.intellij.Util

object Lexer {
  final val syntaxHighlightingDataKey: Key[Seq[RangeHighlighter]] =
    new Key[Seq[RangeHighlighter]]("Logika Highlighting Data")
  final val propJusts: Set[String] = Set("premise", "andi", "ande1", "ande2", "ori1",
    "Vi1", "ori2", "Vi2", "ore", "Ve", "impliesi", "impliese", "noti",
    "negi", "note", "nege", "bottome", "falsee", "pbc")
  final val predJusts: Set[String] = Set("foralli", "alli", "Ai", "foralle", "alle", "Ae",
    "existsi", "somei", "Ei", "existse", "somee", "Ee")
  final val progJusts: Set[String] = Set("subst1", "subst2", "algebra", "auto")
  final val propOps: Set[String] = Set("not", "neg", "and", "xor", "or", "V", "implies")
  final val predOps: Set[String] = Set("forall", "all", "A", "exists", "some", "E")
  final val progOps: Set[String] = Set("*", "/", "%", "+", "-", "+:", ":+", "<", "<=",
    ">", ">=", "=", "==", "!=", "≤", "≥", "≠", "|^", "<<", ">>", ">>>")
  final val justs: Set[String] = propJusts ++ predJusts ++ progJusts
  final val types: Set[String] = Set(
    "B",
    "Z", "Z8", "Z16", "Z32", "Z64",
    "N", "N8", "N16", "N32", "N64",
    "S8", "S16", "S32", "S64",
    "U8", "U16", "U32", "U64",
    "R", "F32", "F64",
    "BS",
    "ZS", "Z8S", "Z16S", "Z32S", "Z64S",
    "NS", "N8S", "N16S", "N32S", "N64S",
    "S8S", "S16S", "S32S", "S64S",
    "U8S", "U16S", "U32S", "U64S",
    "RS", "F32S", "F64S")
  final val constants: Set[String] = Set("true", "T", "⊤", "false", "F")
  final val constantJusts: Set[String] = Set("_|_", "⊥")
  final val andJustFollow: Set[String] = Set("i", "e1", "e2")
  final val orJustFollow: Set[String] = Set("i1", "i2", "e")
  final val propIeJustFirst: Set[String] = Set("->", "→", "!", "~", "¬")
  final val ieJustFollow: Set[String] = Set("i", "e")
  final val keywords: Set[String] = Set("abstract", "case", "catch", "class", "def",
    "do", "else", "extends", "final", "finally", "for", "forSome",
    "if", "implicit", "import", "lazy", "macro", "match", "new",
    "null", "object", "override", "package", "private",
    "protected", "return", "sealed", "super", "this", "throw",
    "trait", "try", "type", "val", "var", "while", "with", "yield",
    "pre", "requires", "modifies", "post", "ensures")

  sealed trait LogikaHighlightingTextAttributes

  object FunTextAttributes
    extends TextAttributes(null, null, null, null, Font.ITALIC)
      with LogikaHighlightingTextAttributes

  def fore(ta: TextAttributes): TextAttributes =
    new TextAttributes(ta.getForegroundColor, null, null, null, Font.PLAIN)

  def foreIt(ta: TextAttributes): TextAttributes = {
    val r = fore(ta)
    r.setFontType(Font.ITALIC)
    r
  }

  def addSyntaxHighlighter(project: Project, editor: Editor): Unit = {
    val mm = editor.getMarkupModel
    var rhs = Vector[RangeHighlighter]()

    def addRange(start: Int, end: Int, ta: TextAttributes): Unit =
      try rhs :+= mm.addRangeHighlighter(start, end,
        900000, ta, HighlighterTargetArea.EXACT_RANGE)
      catch {
        case _: IndexOutOfBoundsException =>
      }

    /* TODO
    def add(t: Token, ta: TextAttributes): Unit =
      addRange(t.getStartIndex, t.getStopIndex + 1, ta)
    */

    Option(editor.getUserData(syntaxHighlightingDataKey)) match {
      case Some(prevRhs) =>
        for (rh <- prevRhs) {
          mm.removeHighlighter(rh)
        }
      case _ =>
    }
    /* TODO
    val inputStream = CharStreams.fromString(editor.getDocument.getText)
    val lexer = new Antlr4LogikaLexer(inputStream)

    val tokens: CSeq[Token] = {
      import scala.collection.JavaConverters._
      lexer.getAllTokens.asScala.filter(_.getChannel != 2)
    }
    val size = tokens.size
    val isProgramming = tokens.exists(_.getText == "import")
    def peek(i: Int, f: Token => Boolean): Boolean =
      if (i < size) f(tokens(i)) else false

    val cs = editor.getColorsScheme

    import DefaultLanguageHighlighterColors._
    val plainAttr = new TextAttributes(cs.getDefaultForeground, null, null, null, Font.PLAIN)
    val stringAttr = fore(cs.getAttributes(STRING))
    val keywordAttr = fore(cs.getAttributes(KEYWORD))
    val lineCommentAttr = fore(cs.getAttributes(LINE_COMMENT))
    val blockCommentAttr = fore(cs.getAttributes(BLOCK_COMMENT))
    val logikaAttr = fore(lineCommentAttr)
    val typeAttr = foreIt(cs.getAttributes(CLASS_REFERENCE))
    val constantAttr = fore(cs.getAttributes(NUMBER))
    val justAttr = foreIt(cs.getAttributes(CONSTANT))
    val opAttr = fore(cs.getAttributes(CONSTANT))
    val annAttr = fore(cs.getAttributes(METADATA))

    val ext = Util.getFileExt(project)
    if (ext == "scala" || ext == "sc") {
      mm.addRangeHighlighter(0, editor.getDocument.getText.length,
        800000, plainAttr, HighlighterTargetArea.EXACT_RANGE)
    }

    import Antlr4LogikaLexer._

    var i = 0
    while (i < size) {
      val token = tokens(i)
      if (token.getText == "z8\"0\"")
        println("here")
      token.getType match {
        case ID if !isProgramming =>
          if (peek(i + 1, _.getText == "("))
            add(token, FunTextAttributes)
        case NUM =>
          add(token, constantAttr)
        case Antlr4LogikaLexer.STRING =>
          add(token, stringAttr)
        case COMMENT =>
          add(token, blockCommentAttr)
        case Antlr4LogikaLexer.LINE_COMMENT =>
          add(token, lineCommentAttr)
        case REAL | INT =>
          val start = token.getStartIndex
          val i = token.getText.indexOf('"')
          addRange(start, start + i + 1, logikaAttr)
          addRange(start + i + 1, token.getStopIndex, constantAttr)
          addRange(token.getStopIndex, token.getStopIndex + 1, logikaAttr)
        case _ =>
          val text = token.getText
          if (justs.contains(text))
            add(token, justAttr)
          else if (types.contains(text))
            add(token, typeAttr)
          else if (keywords.contains(text))
            add(token, keywordAttr)
          else if (text == "l\"\"\"") {
            add(token, logikaAttr)
            if (i + 1 < size && tokens(i + 1).getText == "{")
              add(tokens(i + 1), logikaAttr)
          } else if (text == "\"\"\"") {
            add(token, logikaAttr)
            val tM1 = tokens(i - 1)
            if (tM1.getText == "}")
              add(tM1, logikaAttr)
          } else if (constants.contains(text))
            add(token, constantAttr)
          else if (constantJusts.contains(text)) {
            if (peek(i + 1, _.getText == "e")) {
              add(token, justAttr)
              add(tokens(i + 1), justAttr)
              i += 1
            } else add(token, constantAttr)
          } else if (text == "&" || text == "∧" || text == "^") {
            if (peek(i + 1,
              t => andJustFollow.contains(t.getText)) &&
              peek(i + 2, _.getType == NUM)) {
              add(token, justAttr)
              add(tokens(i + 1), justAttr)
              i += 1
            } else if (text == "∧" || text == "^") add(token, opAttr)
          } else if (text == "|" || text == "∨" || text == "V") {
            if (peek(i + 1,
              t => orJustFollow.contains(t.getText)) &&
              peek(i + 2, _.getType == NUM)) {
              add(token, justAttr)
              add(tokens(i + 1), justAttr)
              i += 1
            } else if (text == "∨" || text == "V") add(token, opAttr)
          } else if (propIeJustFirst.contains(text)) {
            if (peek(i + 1,
              t => ieJustFollow.contains(t.getText)) &&
              peek(i + 2, _.getType == NUM)) {
              add(token, justAttr)
              add(tokens(i + 1), justAttr)
              i += 1
            } else add(token, opAttr)
          } else if (text == "∀" || text == "A" || text == "∃" || text == "E") {
            if (peek(i + 1, t => ieJustFollow.contains(t.getText)) &&
              peek(i + 2, t => t.getType == ID || t.getType == NUM)) {
              add(token, justAttr)
              add(tokens(i + 1), justAttr)
              i += 1
            } else add(token, justAttr)
          } else if (text == "@") {
            add(token, annAttr)
            if (peek(i + 1, _.getType == ID)) {
              add(tokens(i + 1), annAttr)
              i += 1
            }
          } else if (text == "invariant" || text == "fact") {
            if (peek(i - 1, _.getText == "{"))
              add(token, keywordAttr)
            else add(token, justAttr)
          } else if (text == "assume") {
            if (!peek(i + 1, _.getText == "("))
              add(token, justAttr)
          }
      }
      i += 1
    }
     */
    editor.putUserData(syntaxHighlightingDataKey, rhs)
  }
}
