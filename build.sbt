import com.typesafe.sbt.packager.docker._
import com.amazonaws.regions.{Region, Regions}


name := """plato"""
organization := "com.dpalmisano"

lazy val root = (project in file(".")).enablePlugins(PlayScala, BuildInfoPlugin, JavaServerAppPackaging, DockerPlugin, GitVersioning, GitBranchPrompt,ScoverageSbtPlugin,EcrPlugin)

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

coverageEnabled in(Test, compile) := true
coverageEnabled in(Compile, compile) := false

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
  Cmd("RUN", "ntpd", "-gq"), // this is needed to synch container system time, otherwise twitter auth won't work
  Cmd("USER", "daemon"),
  Cmd("ENTRYPOINT", "bin/plato", "-Dconfig.resource=prod.conf", "-Dlogger.resource=logback-prod.xml"),
  ExecCmd("CMD")
)

region           in ecr := Region.getRegion(Regions.EU_WEST_1)
repositoryName   in ecr := (packageName in Docker).value
localDockerImage in ecr := (packageName in Docker).value + ":" + (version in Docker).value
version          in ecr := (version in Docker).value

// Create the repository before authentication takes place (optional)
login in ecr <<= (login in ecr) dependsOn (createRepository in ecr)

// Authenticate and publish a local Docker image before pushing to ECR
push in ecr <<= (push in ecr) dependsOn (publishLocal in Docker, login in ecr)
