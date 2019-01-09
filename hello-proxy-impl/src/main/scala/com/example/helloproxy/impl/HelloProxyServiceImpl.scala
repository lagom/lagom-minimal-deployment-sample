/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.helloproxy.impl

import com.example.hello.api.HelloService
import com.example.helloproxy.api.HelloProxyService
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Implementation of the HelloStreamService.
  */
class HelloProxyServiceImpl(helloService: HelloService)(implicit exCtx: ExecutionContext) extends HelloProxyService {

  def proxyViaHttp(id: String) = ServiceCall { _ =>
    val eventualString: Future[String] = helloService.hello(id).invoke()
    eventualString
  }

}
