name := "humantalks-play-akka-http"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

PlayKeys.playRunHooks += Webpack(baseDirectory.value)

pipelineStages := Seq(gzip)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.7",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  cache,
  filters,
  ws,
  specs2 % Test
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
