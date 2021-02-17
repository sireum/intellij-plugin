/*
 Copyright (c) 2016, Robby, Kansas State University
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

import java.io._
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import com.zaxxer.nuprocess.{NuAbstractProcessHandler, NuProcessBuilder}

import scala.sys.process.ProcessIO

object Exec {

  sealed abstract class Result

  object Timeout extends Result

  case class ExceptionRaised(e: Exception) extends Result

  case class StringResult(s: String, exitValue: Int) extends Result

  {
    System.setProperty("jna.nosys", "true")
  }
}

final class Exec {

  val env: scala.collection.mutable.HashMap[String, String] = scala.collection.mutable.HashMap()

  def process(args: Seq[String],
              writeInput: OutputStream => Unit,
              processOutput: InputStream => Unit,
              extraEnv: (String, String)*): scala.sys.process.Process = {
    scala.sys.process.Process({
      val pb = new java.lang.ProcessBuilder(args: _*)
      pb.redirectErrorStream(true)
      val m = pb.environment
      for ((k, v) <- env ++ extraEnv) {
        m.put(k, v)
      }
      pb
    }).run(new ProcessIO(writeInput, processOutput, errorF).daemonized())
  }

  def run(waitTime: Long, args: Seq[String], input: Option[String],
          extraEnv: (String, String)*): Exec.Result =
    run(waitTime, args, input, None, extraEnv: _*)

  def run(waitTime: Long, args: Seq[String], input: Option[String],
          dir: Option[File], extraEnv: (String, String)*): Exec.Result = {
    import scala.jdk.CollectionConverters._
    val commands = new java.util.ArrayList(args.asJavaCollection)
    val m = scala.collection.mutable.HashMap[String, String]()
    for ((k, v) <- System.getenv().asScala ++ env ++ extraEnv) {
      m.put(k, v)
    }
    val npb = new NuProcessBuilder(commands, m.asJava)
    val sb = new java.lang.StringBuilder()
    npb.setProcessListener(new NuAbstractProcessHandler {
      def append(buffer: ByteBuffer): Unit = {
        val bytes = new Array[Byte](buffer.remaining)
        buffer.get(bytes)
        bytes.foreach(b => sb.append(b.toChar))
      }

      override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
        if (!closed) append(buffer)
      }

      override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
        if (!closed) append(buffer)
      }
    })
    val p = npb.start()
    if (p != null && p.isRunning) {
      input match {
        case Some(in) => p.writeStdin(ByteBuffer.wrap(in.getBytes))
        case _ =>
      }
      p.closeStdin(false)
      val t = if (waitTime <= 0) 0 else waitTime
      val exitCode = p.waitFor(t, TimeUnit.MILLISECONDS)
      if (exitCode != Int.MinValue) return Exec.StringResult(sb.toString, exitCode)
      if (p.isRunning) try {
        p.destroy(false)
        p.waitFor(500, TimeUnit.MICROSECONDS)
      } catch {
        case _: Throwable =>
      }
      if (p.isRunning)
        try p.destroy(true)
        catch {
          case _: Throwable =>
        }
      Exec.Timeout
    } else Exec.ExceptionRaised(new RuntimeException(s"Could not execute command: ${args.mkString(" ")}"))
  }

  def errorF(is: InputStream): Unit = {
    try while (is.read != -1) {} finally is.close()
  }
}