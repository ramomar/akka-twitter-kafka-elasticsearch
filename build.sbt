name := "akka-twitter-kafka-elasticsearch"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.1"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "2.0.1"
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.1.1"
libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-core_2.12" % "6.5.1"
libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-http_2.12" % "6.5.1"
libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-http-streams_2.12" % "6.5.1"
