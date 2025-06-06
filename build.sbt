import org.jetbrains.sbtidea.Keys._

lazy val `sireum-intellij-plugin` = project.in(file("."))
    .enablePlugins(SbtIdeaPlugin)
    .settings(
      version := "5.0.0-SNAPSHOT",
      scalaVersion := "2.13.16",
      ThisBuild / intellijPluginName := "sireum-intellij-plugin",
      ThisBuild / intellijBuild      := "243.26053.27",
      ThisBuild / intellijPlatform   := IntelliJPlatform.IdeaCommunity,
      resolvers += "jitpack" at "https://jitpack.io",
      libraryDependencies += "org.sireum" % "forms" % "4.20250411.25adf97" excludeAll(
        ExclusionRule(organization = "asm"),
        ExclusionRule(organization = "com.intellij"),
        ExclusionRule(organization = "com.jgoodies"),
        ExclusionRule(organization = "jdom"),
      ),
      Global    / intellijAttachSources := true,
      Compile / javacOptions ++= Seq("--release", "17"),
      Compile / scalacOptions ++= Seq("-release", "17", "-deprecation", "-Ydelambdafy:method", "-feature", "-unchecked"),
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
