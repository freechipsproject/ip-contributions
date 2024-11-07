// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / version          := "5.0.1"


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
  publishTo := Some(Opts.resolver.sonatypeStaging)
)

val chiselVersion = "5.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "ip-contributions",
    // resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "5.0.2" % "test",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
    ),
    addCompilerPlugin("org.chipsalliance" %% "chisel-plugin" % chiselVersion cross CrossVersion.full),
  )
  .settings(publishSettings: _*)
