name := "humantalks-play-akka-http"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

PlayKeys.playRunHooks += Webpack(baseDirectory.value)

pipelineStages := Seq(gzip)

libraryDependencies ++= Seq(
  cache,
  filters,
  ws,
  specs2 % Test
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
