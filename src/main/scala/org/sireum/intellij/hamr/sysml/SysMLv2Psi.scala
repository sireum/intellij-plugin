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

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.TextRange
import com.intellij.psi._
import com.intellij.psi.tree.IElementType
import org.antlr.intellij.adaptor.psi.{ANTLRPsiLeafNode, ScopeNode, Trees}

import javax.swing._


class IdentifierPSINode(`type`: IElementType, text: CharSequence) extends ANTLRPsiLeafNode(`type`, text) with PsiNamedElement {
  override def getName: String = getText

  override def setName(name: String): PsiElement = {
    if (getParent == null) return this
    val newID = Trees.createLeafFromText(getProject, SysMLv2Language.INSTANCE, getContext, name, SysMLv2ParserDefinition.ID)
    if (newID != null) return this.replace(newID)
    this
  }

  override def getReference: PsiReference = null
}

abstract class SysMLv2ElementRef(element: IdentifierPSINode) extends PsiReferenceBase[IdentifierPSINode](element, new TextRange(0, element.getText.length)) {
  override def getVariants = new Array[AnyRef](0)

  override def handleElementRename(newElementName: String): PsiElement = {
    myElement.setName(newElementName)
  }

  override def resolve: PsiElement = {
    val scope = myElement.getContext.asInstanceOf[ScopeNode]
    if (scope == null) return null
    scope.resolve(myElement)
  }

  override def isReferenceTo(d: PsiElement): Boolean = {
    var dfn = d
    val refName = myElement.getName
    if (dfn.isInstanceOf[IdentifierPSINode] && isDefSubtree(dfn.getParent)) dfn = dfn.getParent
    if (isDefSubtree(dfn)) {
      val id = dfn.asInstanceOf[PsiNameIdentifierOwner].getNameIdentifier
      val defName = if (id != null) id.getText
      else null
      return refName != null && defName != null && refName == defName
    }
    false
  }

  def isDefSubtree(d: PsiElement): Boolean
}

class SysMLv2PSIFileRoot(viewProvider: FileViewProvider) extends PsiFileBase(viewProvider, SysMLv2Language.INSTANCE) with ScopeNode {
  override def getFileType: FileType = SysMLv2FileType.INSTANCE

  override def toString = "SysML v2 file"

  override def getIcon(flags: Int): Icon = Icons.SYSMLV2_ICON

  override def getContext: ScopeNode = null

  override def resolve(element: PsiNamedElement): PsiElement = null
}
