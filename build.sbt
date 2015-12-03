scalaVersion := "2.11.7"

val stream = "2.0-M2"

libraryDependencies := Seq(
  "com.typesafe.akka"      %% "akka-stream-experimental"          % stream,
  "com.typesafe.akka"      %% "akka-http-experimental"            % stream,
  "com.typesafe.akka"      %% "akka-http-spray-json-experimental" % stream,
  "com.typesafe.akka"      %% "akka-http-xml-experimental"        % stream,
  "com.typesafe.akka"      %% "akka-remote"                       % "2.3.11",
  "org.scala-lang.modules" %% "scala-pickling"                    % "0.10.0",
  "org.scala-lang.modules" %% "scala-xml"                         % "1.0.4",
  "com.typesafe.akka"      %% "akka-stream-testkit-experimental"  % stream  % "test",
  "com.typesafe.akka"      %% "akka-http-testkit-experimental"    % stream  % "test",
  "org.scalatest"          %% "scalatest"                         % "2.2.4" % "test"
)
