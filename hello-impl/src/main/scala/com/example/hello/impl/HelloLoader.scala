/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.hello.impl

import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.playjson.{ JsonSerializer, JsonSerializerRegistry }
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

class HelloLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloService])
}

abstract class HelloApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with ClusterComponents
    with PubSubComponents
    with AhcWSComponents {

  // ClusterComponents requires a JsonSerializerRegistry but we're not using any
  // type that requires a custom Serializer because we're just sending String
  // back and forth.
  override def jsonSerializerRegistry: JsonSerializerRegistry =
    new JsonSerializerRegistry {
      override def serializers = immutable.Seq.empty[JsonSerializer[_]]
    }

  // Bind the service that this server provides
  override lazy val lagomServer =
    serverFor[HelloService](wire[HelloServiceImpl])

}
