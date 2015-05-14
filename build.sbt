scalaVersion := "2.11.6"

val stream = "1.0-RC2"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % stream,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-scala-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % stream,
  "com.typesafe.akka" %% "akka-remote" % "2.3.11",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.0"
)

updateOptions := updateOptions.value.withConsolidatedResolution(true)
