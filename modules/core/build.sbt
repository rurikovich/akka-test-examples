
name := "core"

version := "1.0"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

val akkaVersion = "2.5.22"

libraryDependencies ++= Seq( guice,specs2 % Test )

libraryDependencies ++= Seq(
  "net.ruippeixotog" %% "scala-scraper" % "2.1.0",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.6.0",

  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.0.10"
)

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test

