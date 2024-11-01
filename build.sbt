// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.10")
ThisBuild / version          := "0.5.4"


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
  // MS: maybe we should enable this again
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
      "edu.berkeley.cs" %% "chisel3" % "3.5.6",
      "edu.berkeley.cs" %% "dsptools" % "1.5.6",
      "edu.berkeley.cs" %% "chiseltest" % "0.5.6" % "test",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.6" cross CrossVersion.full),
    // addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
  .settings(publishSettings: _*)
