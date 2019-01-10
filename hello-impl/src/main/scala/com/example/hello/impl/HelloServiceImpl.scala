/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.hello.impl

import java.util.UUID

import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.Future

class HelloServiceImpl() extends HelloService {


  // Use a random UUID on the response as a poor man's node ID
  val uuid = UUID.randomUUID()

  override def hello(id: String) = ServiceCall { _ =>
    Future.successful(s"Hi $id! (at node $uuid)")
  }
}
