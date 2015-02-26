scalaVersion := "2.11.5"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M3",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M3",
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % "1.0-M3",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

updateOptions := updateOptions.value.withConsolidatedResolution(true)
