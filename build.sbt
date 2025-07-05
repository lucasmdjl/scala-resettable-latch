ThisBuild / organization := "dev.lucasmdjl"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / version := "0.1.0"
ThisBuild / description := "A Scala 3 micro-library providing thread-safe countdown latches with reset capabilities and Scala-friendly APIs."
ThisBuild / scalaVersion := "3.7.1"
ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeCentralHost

lazy val root = (project in file("."))
  .settings(
    name := "scala-resettable-latch",
    idePackagePrefix := Some("dev.lucasmdjl.resettablelatch"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.19" % "test",
      "org.scalacheck" %% "scalacheck" % "1.18.1" % "test",
      "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % "test",
      "dev.lucasmdjl" %% "scala-delayed-future" % "0.3.0" % "test",
    ),
    publishTo := sonatypePublishToBundle.value,
  )
