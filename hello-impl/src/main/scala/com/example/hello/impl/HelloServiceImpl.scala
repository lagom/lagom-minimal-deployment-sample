/*
 * Copyright (C) 2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.hello.impl

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }
import java.util.function.UnaryOperator

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.pubsub.{ PubSubRegistry, TopicId }
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ ExecutionContext, Future }

class HelloServiceImpl(pubSub: PubSubRegistry,
                       applicationLifecycle: ApplicationLifecycle
                      )(implicit exCtx: ExecutionContext,
                        materializer: Materializer
                      ) extends HelloService {

  // Use a random UUID on the response as a poor man's node ID
  val uuid = UUID.randomUUID()

  override def hello(id: String) = ServiceCall { _ =>
    Future.successful(
      s"""
         |${observed.get.map{case (k,v) => s"$k -> $v"}.toSeq.sorted.mkString("\n")}
         |
         |Hi $id! (at node $uuid)
       """.stripMargin
    )
  }

  val topic = pubSub.refFor(TopicId[String]("gossip"))
  // When was the last time a String was observed (where String is the node UUID).
  val observed = new AtomicReference[Map[String, Long]](Map.empty[String, Long])
  val finished = new AtomicBoolean(false)

  topic.subscriber.map(x =>
    observed.updateAndGet(new UnaryOperator[Map[String, Long]] {
      override def apply(t: Map[String, Long]): Map[String, Long] = {
        t + (x -> System.currentTimeMillis())
      }
    })
  ).runWith(Sink.ignore)

  new Thread(
    new Runnable {
      override def run(): Unit = {
        while (!finished.get()) {
          topic.publish(uuid.toString)
          TimeUnit.SECONDS.sleep(5)
        }
      }
    }
  ).start()

  applicationLifecycle.addStopHook {
    () =>
      finished.set(true)
      Future.successful(Done)
  }

}
