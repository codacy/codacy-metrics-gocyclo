enablePlugins(JavaAppPackaging)
scalaVersion := "2.13.6"
name := "codacy-metrics-gocyclo"

libraryDependencies ++= Seq("com.codacy" %% "codacy-metrics-scala-seed" % "0.2.2")
