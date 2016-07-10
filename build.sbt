name := "humantalks-akka-stream"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

PlayKeys.playRunHooks += Webpack(baseDirectory.value)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8"
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
