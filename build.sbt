// See README.md for license details.

def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // If we're building with Scala > 2.11, enable the compile option
    //  switch to support our anonymous Bundle definitions:
    //  https://github.com/scala/bug/issues/10047
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 => Seq()
      case _ => Seq("-Xsource:2.11")
    }
  }
}

def javacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // Scala 2.12 requires Java 8. We continue to generate
    //  Java 7 compatible code for Scala 2.11
    //  for compatibility with old clients.
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 =>
        Seq("-source", "1.7", "-target", "1.7")
      case _ =>
        Seq("-source", "1.8", "-target", "1.8")
    }
  }
}

name := "ip-contributions"

version := "0.4.0"

// groupId, SCM, license information
organization := "edu.berkeley.cs"
homepage := Some(url("https://github.com/freechipsproject/ip-contributions"))
scmInfo := Some(ScmInfo(url("https://github.com/freechipsproject/ip-contributions"), "git@github.com/freechipsproject/ip-contributions"))
developers := List(Developer("schoeberl", "schoeberl", "martin@jopdesign.com", url("https://github.com/schoeberl")))
licenses += ("Unlicense", url("https://unlicense.org/"))
publishMavenStyle := true

// disable publishw ith scala version, otherwise artifact name will include scala version 
// e.g cassper_2.11
crossPaths := false

// add sonatype repository settings
// snapshot versions publish to sonatype snapshot repository
// other versions publish to sonatype staging repository
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

val scala211 = "2.11.12"
val scala212 = "2.12.8"

scalaVersion := scala212

crossScalaVersions := Seq(scala212, scala211)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "chisel-iotesters" -> "1.5.0",
  "chiseltest"       -> "0.3.1",
  "dsptools"         -> "1.4.1"
  )

libraryDependencies ++= Seq("chiseltest", "chisel-iotesters", "dsptools").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) }

scalacOptions ++= scalacOptionsVersion(scalaVersion.value)

javacOptions ++= javacOptionsVersion(scalaVersion.value)
