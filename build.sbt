scalaVersion := "2.11.8"

val stream = "2.4.16"
val http = "10.0.1"

libraryDependencies := Seq(
  "com.typesafe.akka"      %% "akka-stream"          % stream,
  "com.typesafe.akka"      %% "akka-http"            % http,
  "com.typesafe.akka"      %% "akka-http-spray-json" % http,
  "com.typesafe.akka"      %% "akka-http-xml"        % http,
  "com.typesafe.akka"      %% "akka-remote"          % stream,
  "com.typesafe.akka"      %% "akka-contrib"         % stream,
  "org.scala-lang.modules" %% "scala-pickling"       % "0.10.0",
  "org.scala-lang.modules" %% "scala-xml"            % "1.0.4",
  "com.typesafe.akka"      %% "akka-stream-testkit"  % stream,
  "com.typesafe.akka"      %% "akka-http-testkit"    % http  % Test,
  "org.scalatest"          %% "scalatest"            % "3.0.1" % Test
)
