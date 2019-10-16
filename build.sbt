import com.typesafe.sbt.packager.docker.{Cmd, DockerAlias}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
organization := "com.codacy"
scalaVersion := "2.13.1"
name := "codacy-metrics-gocyclo"
// App Dependencies
libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-metrics-scala-seed" % "0.2.0",
  "org.specs2" %% "specs2-core" % "4.8.0" % Test)

mappings in Universal ++= {
  (resourceDirectory in Compile).map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

Docker / packageName := packageName.value
dockerBaseImage := "openjdk:8-jre-alpine"
Docker / daemonUser := "docker"
Docker / daemonGroup := "docker"
dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")
dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("ADD", _) =>
    List(
      Cmd("RUN", "adduser -u 2004 -D docker"),
      cmd,
      Cmd("ENV", "GOPATH", "/go"),
      Cmd("ENV", "PATH", "/go/bin:$PATH"),
      Cmd(
        "RUN",
        """apk update &&
          |apk add --no-cache bash &&
          |apk add musl-dev go git &&
          |go get github.com/fzipp/gocyclo &&
          |apk del musl-dev git &&
          |rm -rf /tmp/* &&
          |rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")),
      Cmd("RUN", "mv /opt/docker/docs /docs"))

  case other => List(other)
}
