scalaVersion := "2.11.4"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M1",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M1",
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % "1.0-M1",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

updateOptions := updateOptions.value.withConsolidatedResolution(true)
