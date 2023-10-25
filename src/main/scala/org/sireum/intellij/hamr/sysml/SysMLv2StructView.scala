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

import com.intellij.ide.structureView._
import com.intellij.ide.util.treeView.smartTree.{SortableTreeElement, Sorter, TreeElement}
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.{ItemPresentation, NavigationItem}
import com.intellij.openapi.editor.Editor
import com.intellij.psi.{PsiElement, PsiFile, PsiNamedElement}

import javax.swing._


class SysMLv2ItemPresentation(protected val element: PsiElement) extends ItemPresentation {
  override def getIcon(unused: Boolean): Icon = Icons.SYSMLV2_ICON

  override def getPresentableText: String = element.getNode.getText

  override def getLocationString: String = null
}


class SysMLv2RootPresentation(protected val element: PsiFile) extends ItemPresentation {
  override def getIcon(unused: Boolean): Icon = Icons.SYSMLV2_ICON

  override def getPresentableText: String = element.getVirtualFile.getNameWithoutExtension

  override def getLocationString: String = null
}


class SysMLv2StructureViewElement(protected val element: PsiElement) extends StructureViewTreeElement with SortableTreeElement {
  override def getValue: AnyRef = element

  override def navigate(requestFocus: Boolean): Unit = {
    element match {
      case item: NavigationItem => item.navigate(requestFocus)
      case _ =>
    }
  }

  override def canNavigate: Boolean = element.isInstanceOf[NavigationItem] && element.asInstanceOf[NavigationItem].canNavigate

  override def canNavigateToSource: Boolean = element.isInstanceOf[NavigationItem] && element.asInstanceOf[NavigationItem].canNavigateToSource

  override def getAlphaSortKey: String = {
    val s = element match {
      case element: PsiNamedElement => element.getName
      case _ => null
    }
    if (s == null) return "unknown key"
    s
  }

  override def getPresentation: ItemPresentation = new SysMLv2ItemPresentation(element)

  override def getChildren: Array[TreeElement] = new Array[TreeElement](0)
}


class SysMLv2StructureViewFactory extends PsiStructureViewFactory {
  override def getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder = new TreeBasedStructureViewBuilder() {
    override def createStructureViewModel(editor: Editor): StructureViewModel = new SysMLv2StructureViewModel(psiFile.asInstanceOf[SysMLv2PSIFileRoot])
  }
}

class SysMLv2StructureViewModel(root: SysMLv2PSIFileRoot) extends StructureViewModelBase(root, new SysMLv2StructureViewRootElement(root)) with StructureViewModel.ElementInfoProvider {
  override def getSorters: Array[Sorter] = Array[Sorter](Sorter.ALPHA_SORTER)

  override def isAlwaysLeaf(element: StructureViewTreeElement): Boolean = !isAlwaysShowsPlus(element)

  override def isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = {
    val value = element.getValue
    value.isInstanceOf[SysMLv2PSIFileRoot]
  }
}

class SysMLv2StructureViewRootElement(element: PsiFile) extends SysMLv2StructureViewElement(element) {
  override def getPresentation: ItemPresentation = new SysMLv2RootPresentation(element)
}
