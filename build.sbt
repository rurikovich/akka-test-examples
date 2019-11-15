
import com.typesafe.sbt.MultiJvmPlugin.multiJvmSettings

import sbt.Keys.{libraryDependencies, resolvers}

name := "akka-test-examples"

version := "1.0"

scalaVersion := "2.12.2"

val akkaVersion = "2.5.22"

lazy val core = project in file("modules/core")

lazy val akkaTestExamplesProject = (project in file("."))
  .settings(multiJvmSettings: _*)
  .enablePlugins(PlayScala)
  .configs(MultiJvm)
  .settings(
    parallelExecution in Test := false, // do not run test cases in parallel
    resolvers ++= Seq(
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
      "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
    ),
    libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice),

    libraryDependencies ++= Seq(
      "net.ruippeixotog" %% "scala-scraper" % "2.1.0",
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion, //убрать
      "ch.qos.logback" % "logback-classic" % "1.0.10",
      "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.5.26"
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test exclude("org.scalatestplus.play", "scalatestplus-play"),
      "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
      "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0" % Test,
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0" % Test,
      "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % Test excludeAll(
        ExclusionRule(organization = "de.flapdoodle.embed", name = "de.flapdoodle.embed.mongo"),
        ExclusionRule(organization = "org.mongodb.scala", name = "mongo-scala-driver")
      ),
      "com.typesafe.akka" %% "akka-http" % "10.1.10" %Test,
      "com.typesafe.akka" %% "akka-stream" % "2.5.26" %Test
    ),
    unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test"),
    unmanagedSourceDirectories in MultiJvm := Seq(baseDirectory(_ / "test/multi_jvm")).join.value
  ).dependsOn(core)

// exclude("org.mongodb.scala", "mongo-scala-driver")