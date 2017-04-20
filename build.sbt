import com.typesafe.sbt.packager.docker._

name := """plato"""
organization := "com.dpalmisano"

lazy val root = (project in file(".")).enablePlugins(PlayScala, BuildInfoPlugin, JavaServerAppPackaging, DockerPlugin, GitVersioning, GitBranchPrompt)

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

git.useGitDescribe := true

git.baseVersion := "0.0.0"

javaOptions += "-Dconfig.resource=test.conf"
javaOptions += "-Dlogger.resource=logback-test.xml"

buildInfoKeys ++= Seq[BuildInfoKey](
  BuildInfoKey.action("commit") {
    import scala.sys.process._
    "git rev-parse HEAD".!!.stripSuffix("\n")
  },
  BuildInfoKey.action("author") {
    sys.env("LOGNAME")
  }
)

buildInfoOptions += BuildInfoOption.BuildTime
buildInfoOptions += BuildInfoOption.Traits("BuildInfoBase")

buildInfoPackage := "controllers.buildinfo"

dockerCommands := Seq(
  Cmd("FROM", "openjdk:latest"),
  Cmd("MAINTAINER", "dpalmisano@gmail.com"),
  Cmd("WORKDIR", "/opt/docker"),
  Cmd("ADD", "opt",  "/opt"),
  Cmd("RUN", "chown", "-R", "daemon:daemon", "."),
  Cmd("RUN", "apt-get", "update"),
  Cmd("RUN", "apt-get", "upgrade", "-y"),
  Cmd("RUN", "apt-get", "install", "ntp", "-y"),
  Cmd("RUN", "ntpd", "-gq"),
  Cmd("USER", "daemon"),
  Cmd("ENTRYPOINT", "bin/plato", "-Dconfig.resource=prod.conf"),
  ExecCmd("CMD")
)
