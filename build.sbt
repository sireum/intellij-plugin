import org.jetbrains.sbtidea.Keys._

lazy val `sireum-intellij-plugin` = project.in(file("."))
    .enablePlugins(SbtIdeaPlugin)
    .settings(
      version := "5.0.0-SNAPSHOT",
      scalaVersion := "2.13.10",
      ThisBuild / intellijPluginName := "sireum-intellij-plugin",
      ThisBuild / intellijBuild      := "231.8770.65",
      ThisBuild / intellijPlatform   := IntelliJPlatform.IdeaCommunity,
      Global    / intellijAttachSources := true,
      Compile / javacOptions ++= Seq("--release", "8"),
      Compile / scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-Ydelambdafy:method", "-feature", "-unchecked"),
      intellijPlugins ++= Seq(
        "org.jetbrains.plugins.terminal".toPlugin,
        "org.intellij.scala".toPlugin
      ),
      Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
      Test / unmanagedResourceDirectories    += baseDirectory.value / "testResources",
      patchPluginXml := pluginXmlOptions { xml =>
        xml.sinceBuild = (ThisBuild / intellijBuild).value
      }
    )