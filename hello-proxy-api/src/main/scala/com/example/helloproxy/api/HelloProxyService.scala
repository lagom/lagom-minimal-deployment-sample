/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.helloproxy.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Service, ServiceCall }

trait HelloProxyService extends Service {

  def proxyViaHttp(id:String): ServiceCall[NotUsed, String]

  override final def descriptor = {
    import Service._

    named("hello-proxy-lagom-minimal-service")
      .withCalls(
        restCall(Method.GET, "/proxy/rest-hello/:id", proxyViaHttp _)
      ).withAutoAcl(true)
  }
}

