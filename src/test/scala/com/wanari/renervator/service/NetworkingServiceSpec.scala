package com.wanari.renervator.service

import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._


class NetworkingServiceSpec extends WordSpecLike with Matchers{

  import concurrent.ExecutionContext.Implicits._

  trait Scope {
    val service = new NetworkingService
  }

  "Ping" should {

    "work" in new Scope {
      service.ping("127.0.0.1").unsafeRunSync shouldBe true
    }

  }


}
