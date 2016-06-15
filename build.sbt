name := "vue-playframework"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

pipelineStages := Seq(gzip)

libraryDependencies ++= Seq(
  cache,
  filters,
  ws,
  specs2 % Test
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
