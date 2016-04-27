scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.json4s"        %% "json4s-jackson"   % "3.3.0", // or json4s-native
  "de.heikoseeberger" %% "akka-http-json4s" % "1.6.0",
  "org.scalatest"     %% "scalatest"        % "2.2.6" % Test
)
