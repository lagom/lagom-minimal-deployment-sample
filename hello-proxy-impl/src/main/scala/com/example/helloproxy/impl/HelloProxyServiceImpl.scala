/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.helloproxy.impl

import java.util.UUID

import com.example.hello.api.HelloService
import com.example.helloproxy.api.HelloProxyService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Implementation of the HelloStreamService.
  */
class HelloProxyServiceImpl(helloService: HelloService, config: Config)(implicit exCtx: ExecutionContext) extends HelloProxyService {

  // Use a random UUID on the response as a poor man's node ID
  val uuid = UUID.randomUUID()

  def proxyViaHttp(id: String) = ServiceCall { _ =>
    val eventualString: Future[String] =
      helloService
        .hello(id)
        .invoke()
        .map { resp => s"      $resp (via proxy node $uuid)" }
    eventualString
  }
}
