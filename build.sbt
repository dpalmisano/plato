name := """plato"""
organization := "com.dpalmisano"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies ++= Seq(
  jdbc,
  evolutions,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-core" % "2.7.21" % Test,
  "org.twitter4j" % "twitter4j-stream" % "4.0.6",
  "com.typesafe.play" %% "anorm" % "2.5.0",
  "org.postgresql" % "postgresql" % "42.0.0"
)

enablePlugins(JavaServerAppPackaging, DockerPlugin, GitVersioning, GitBranchPrompt)
git.useGitDescribe := true

git.baseVersion := "0.0.0"

javaOptions += "-Dconfig.resource=test.conf"
javaOptions += "-Dlogger.resource=logback-test.xml"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.dpalmisano.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.dpalmisano.binders._"
