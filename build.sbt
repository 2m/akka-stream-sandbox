scalaVersion := "2.11.8"

val stream = "2.4.11"

libraryDependencies := Seq(
  "com.typesafe.akka"      %% "akka-stream"                       % stream,
  "com.typesafe.akka"      %% "akka-http-experimental"            % stream,
  "com.typesafe.akka"      %% "akka-http-spray-json-experimental" % stream,
  "com.typesafe.akka"      %% "akka-http-xml-experimental"        % stream,
  "com.typesafe.akka"      %% "akka-remote"                       % stream,
  "org.scala-lang.modules" %% "scala-pickling"                    % "0.10.0",
  "org.scala-lang.modules" %% "scala-xml"                         % "1.0.4",
  "com.typesafe.akka"      %% "akka-stream-testkit"               % stream,
  "com.typesafe.akka"      %% "akka-http-testkit"                 % stream  % "test",
  "org.scalatest"          %% "scalatest"                         % "3.0.1" % "test"
)
