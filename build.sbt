// See README.md for license details.

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / crossScalaVersions := Seq("2.12.13", "2.13.5")
ThisBuild / version          := "0.5-SNAPSHOT"


lazy val publishSettings = Seq (
  // groupId, SCM, license information
  organization := "edu.berkeley.cs",
  homepage := Some(url("https://github.com/freechipsproject/ip-contributions")),
  scmInfo := Some(ScmInfo(url("https://github.com/freechipsproject/ip-contributions"), "git@github.com/freechipsproject/ip-contributions")),
  developers := List(Developer("schoeberl", "schoeberl", "martin@jopdesign.com", url("https://github.com/schoeberl"))),
  licenses += ("Unlicense", url("https://unlicense.org/")),
  publishMavenStyle := true,

  // disable publish with scala version, otherwise artifact name will include scala version 
  // e.g cassper_2.11
  crossPaths := false,

  // add sonatype repository settings
  // snapshot versions publish to sonatype snapshot repository
  // other versions publish to sonatype staging repository
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
)

lazy val root = (project in file("."))
  .settings(
    name := "ip-contributions",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.5-SNAPSHOT",
      "edu.berkeley.cs" %% "dsptools" % "1.5-SNAPSHOT",
      "edu.berkeley.cs" %% "chisel-iotesters" % "2.5-SNAPSHOT" % "test",
      "edu.berkeley.cs" %% "chiseltest" % "0.5-SNAPSHOT" % "test",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
      "-P:chiselplugin:useBundlePlugin"
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.4.3" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
  .settings(publishSettings: _*)
