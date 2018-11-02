name := "litego-scala"

version := "0.2"

val currentScalaVersion = "2.12.7"

scalaVersion := currentScalaVersion

organization := "io.litego"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-language:postfixOps"
)

val circeVersion   = "0.9.3"
val akkaStreamJson = "3.5.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-http"         % "10.1.5",
  "com.typesafe.akka"          %% "akka-stream"       % "2.5.17",
  "de.knutwalker"              %% "akka-stream-circe" % akkaStreamJson,
  "de.knutwalker"              %% "akka-http-circe"   % akkaStreamJson,
  "io.circe"                   %% "circe-core"        % circeVersion,
  "io.circe"                   %% "circe-generic"     % circeVersion,
  "io.circe"                   %% "circe-parser"      % circeVersion,
  "com.typesafe.scala-logging" %% "scala-logging"     % "3.9.0",
  "org.scalatest"              %% "scalatest"         % "3.0.5" % "test",
  "ch.qos.logback"             % "logback-classic"    % "1.2.3" % "test"
)
