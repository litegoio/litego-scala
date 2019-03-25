val currentScalaVersion = "2.12.7"

val circeVersion   = "0.9.3"
val akkaStreamJson = "3.5.0"

lazy val commonSettings = Seq(
  version := "0.3",
  organization := "io.litego",
  name := "litego-scala",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalaVersion := currentScalaVersion,
  scalacOptions := Seq(
    "-target:jvm-1.8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
    "-Xlint",
    "-Xcheckinit",
    "-Ywarn-adapted-args",
    "-Ywarn-value-discard",
    "-Ywarn-inaccessible",
    "-Ywarn-dead-code",
    "-language:postfixOps"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),

  // Sonatype publishing
  publishMavenStyle := true,
  publishTo := sonatypePublishTo.value,
  sonatypeProfileName := "io.litego",
  autoScalaLibrary := false,
  autoScalaLibrary in test := false,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <url>https://litego.io</url>
      <scm>
        <url>https://github.com/litegoio/litego-scala.git</url>
        <connection>git@github.com:litegoio/litego-scala.git</connection>
      </scm>
      <developers>
        <developer>
          <id>litegoio</id>
          <name>LiteGo.io</name>
          <url>https://github.com/litegoio</url>
        </developer>
      </developers>
    )
)

lazy val commonDependencies = Seq(
  "com.typesafe.akka"          %% "akka-http"         % "10.1.5",
  "com.typesafe.akka"          %% "akka-stream"       % "2.5.17",
  "de.knutwalker"              %% "akka-stream-circe" % akkaStreamJson,
  "de.knutwalker"              %% "akka-http-circe"   % akkaStreamJson,
  "io.circe"                   %% "circe-core"        % circeVersion,
  "io.circe"                   %% "circe-generic"     % circeVersion,
  "io.circe"                   %% "circe-parser"      % circeVersion,
  "com.typesafe.scala-logging" %% "scala-logging"     % "3.9.0"
)

lazy val testDependencies = Seq(
  "org.scalatest"              %% "scalatest"         % "3.0.5" % "test",
  "ch.qos.logback"             % "logback-classic"    % "1.2.3" % "test"
)

lazy val litegoScala = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(libraryDependencies ++= testDependencies)
