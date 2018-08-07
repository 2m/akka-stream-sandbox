scalaVersion := "2.12.6"

val stream = "2.5.14"
val http = "10.0.11"

libraryDependencies := Seq(
  "com.typesafe.akka"      %% "akka-stream"          % stream,
  "com.typesafe.akka"      %% "akka-http"            % http,
  "com.typesafe.akka"      %% "akka-http-spray-json" % http,
  "com.typesafe.akka"      %% "akka-http-xml"        % http,
  "com.typesafe.akka"      %% "akka-remote"          % stream,
  "com.typesafe.akka"      %% "akka-contrib"         % stream,
  "org.scala-lang.modules" %% "scala-xml"            % "1.0.6",
  "com.typesafe.akka"      %% "akka-stream-testkit"  % stream,
  "com.chuusai"            %% "shapeless"            % "2.3.2",
  "com.typesafe.akka"      %% "akka-http-testkit"    % http    % "test",
  "org.scalatest"          %% "scalatest"            % "3.0.5" % "test"
)

resolvers += Resolver.bintrayRepo("fcomb", "maven")
