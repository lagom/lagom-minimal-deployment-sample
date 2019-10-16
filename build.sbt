import com.lightbend.lagom.core.LagomVersion
import com.typesafe.sbt.packager.docker.{ DockerAlias, DockerPermissionStrategy }

organization in ThisBuild := "com.example"
version in ThisBuild := "1.7-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.8"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val lagomScaladslAkkaDiscovery = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current

lazy val `lagom-scala-openshift-smoketests` = (project in file("."))
  .settings(headerSettings)
  .settings(disableDockerPublish)
  .aggregate(`hello-api`, `hello-impl`, `hello-proxy-api`, `hello-proxy-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(headerSettings)
  .settings(disableDockerPublish)
  .settings(
    libraryDependencies += lagomScaladslApi,
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .settings(headerSettings)
  .settings(dockerSettings("hello-lagom"))
  .settings(
    libraryDependencies ++= Seq(
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.0",
      lagomScaladslPubSub,
      macwire,
      scalaTest
    )
  ).settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)

lazy val `hello-proxy-api` = (project in file("hello-proxy-api"))
  .settings(headerSettings)
  .settings(disableDockerPublish)
  .settings(
    libraryDependencies += lagomScaladslApi,
  )

lazy val `hello-proxy-impl` = (project in file("hello-proxy-impl"))
  .enablePlugins(LagomScala)
  .settings(headerSettings)
  .settings(dockerSettings("hello-proxy-lagom"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslAkkaDiscovery,
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

def disableDockerPublish: Seq[Setting[_]] = Seq(
  publish in Docker := {},
)

def dockerSettings(name: String): Seq[Setting[_]] = Seq(
  dockerAliases in Docker += DockerAlias(None, None, name, None),
  packageName in Docker := name,
  dockerPermissionStrategy := DockerPermissionStrategy.Run,
  dockerBaseImage := "adoptopenjdk/openjdk8",
)

ThisBuild / scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked")
