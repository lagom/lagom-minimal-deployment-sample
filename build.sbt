import com.typesafe.sbt.packager.docker.DockerAlias

organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.8"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `lagom-minimal-deployment-sample` = (project in file("."))
  .settings(headerSettings)
  .aggregate(`hello-api`, `hello-impl`, `hello-proxy-api`, `hello-proxy-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies += lagomScaladslApi
  ).settings(
    headerSettings
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .settings(headerSettings)
  .settings(
    dockerAliases in Docker += DockerAlias(None, None, "hello-lagom", None),
    packageName in Docker := "hello-lagom",
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.0-RC2",
      lagomScaladslPubSub,
      macwire,
      scalaTest
    )
  ).settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)

lazy val `hello-proxy-api` = (project in file("hello-proxy-api"))
  .settings(
    libraryDependencies +=lagomScaladslApi
  ).settings(
    headerSettings
  )

lazy val `hello-proxy-impl` = (project in file("hello-proxy-impl"))
  .enablePlugins(LagomScala)
  .settings(headerSettings)
  .settings(
    dockerAliases in Docker += DockerAlias(None, None, "hello-proxy-lagom", None),
    packageName in Docker := "hello-proxy-lagom",
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % "0.0.12",
      lagomScaladslTestKit,
      macwire,
      scalaTest
  )
)
  .dependsOn(`hello-proxy-api`, `hello-api`)

// This sample application doesn't need either Kafka or Cassandra so we disable them
// to make the devMode startup faster.
lagomCassandraEnabled in ThisBuild := false
lagomKafkaEnabled in ThisBuild := false

def headerSettings: Seq[Setting[_]] = Seq(
  headerLicense := Some(HeaderLicense.Custom("Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>")),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")))
)
