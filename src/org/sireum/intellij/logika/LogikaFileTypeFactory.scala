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

import javax.swing.Icon
import com.intellij.openapi.util.IconLoader

import com.intellij.openapi.fileTypes.{PlainTextLanguage, LanguageFileType, FileTypeConsumer, FileTypeFactory}

class LogikaFileTypeFactory extends FileTypeFactory {
  override def createFileTypes(fileTypeConsumer: FileTypeConsumer): Unit = {
    for (e <- LogikaFileType.extensions) {
      fileTypeConsumer.consume(LogikaFileType, e)
    }
  }
}

object LogikaFileType extends LanguageFileType(PlainTextLanguage.INSTANCE) {

  final val extensions = Set("logika", "lgk")
  final val icon = IconLoader.getIcon("/logika/icon/logika-4.png")

  override val getDefaultExtension: String = "logika"

  override val getName: String = "Logika"

  override val getIcon: Icon = icon

  override val getDescription: String = "Sireum Logika files"
}
