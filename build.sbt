scalaVersion := "2.11.7"

val stream = "1.0-RC4"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % stream,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-xml-experimental" % stream,
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % stream,
  "com.typesafe.akka" %% "akka-remote" % "2.3.11",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.0",
  "org.scala-lang.modules" %% "scala-xml"      % "1.0.4"
)

updateOptions := updateOptions.value.withConsolidatedResolution(true)
