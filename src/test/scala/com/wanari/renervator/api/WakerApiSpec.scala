package com.wanari.renervator.api

import cats.effect.IO
import com.wanari.renervator.Database
import com.wanari.renervator.Database.Host
import com.wanari.renervator.api.WakerApi.{HostDTO, HostListDTO, IdDTO}
import com.wanari.renervator.service.NetworkingService
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.{Response, Status, Uri}
import org.scalatest.{Matchers, WordSpec}

class WakerApiSpec extends WordSpec with Matchers {

  val database: List[Host] => Database = (hosts: List[Host]) => {
    val database = new Database
    database.init(hosts).unsafeRunSync()
    database
  }

  "WakerAPI" when {
    "POST /wakeItUp" should {
      "return NotFound when id is not found in the database" in {
        val wakerApi = new WakerApi(database(List.empty[Host]), new MockedNetworkingService(Right(())))

        val request = POST(IdDTO(1L).asJson, Uri.uri("/wakeItUp")).unsafeRunSync
        val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
        response match {
          case Some(response) => response.status shouldBe Status.NotFound
          case _ => fail()
        }
      }

      "return NotFound when NetworkingService#sendMagicPocket fails" in {
        val wakerApi = new WakerApi(database(List.empty[Host]), new MockedNetworkingService(Left("I'm failed")))

        val request = POST(IdDTO(1L).asJson, Uri.uri("/wakeItUp")).unsafeRunSync
        val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
        response match {
          case Some(response) => response.status shouldBe Status.NotFound
          case _ => fail()
        }
      }

      "return OK when database is initialized" in {
        val wakerApi = new WakerApi(database(List(Host("localhost", "localhost", "", isOnline = false, 1L))), new MockedNetworkingService(Right(())))

        val request = POST(IdDTO(1L).asJson, Uri.uri("/wakeItUp")).unsafeRunSync
        val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
        response match {
          case Some(response) =>
            response.status shouldBe Status.Ok
            response.as[Json].unsafeRunSync shouldBe Json.obj()
          case _ => fail()
        }
      }

    }
    "POST /wol" when {
      "when database base is empty" should {
        "return the data from the database" in {
          val wakerApi = new WakerApi(database(List.empty[Host]), new MockedNetworkingService(Right(())))

          val request = GET(Uri.uri("/wol")).unsafeRunSync
          val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
          response match {
            case Some(response) =>
              response.status shouldBe Status.Ok
              response.as[Json].unsafeRunSync() shouldBe HostListDTO(List.empty[Host]).asJson
            case _ => fail()
          }
        }

      }
    }
    "when database base is initialized" should {
      "return the data from the database" in {
        val host = Host("localhost", "localhost", "", isOnline = false, 1L)
        val expectedHosts = HostDTO(1L, "localhost", false)
        val wakerApi = new WakerApi(database(List(host)), new MockedNetworkingService(Right(())))

        val request = GET(Uri.uri("/wol")).unsafeRunSync
        val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
        response match {
          case Some(response) =>
            response.status shouldBe Status.Ok
            response.as[Json].unsafeRunSync() shouldBe HostListDTO(List(expectedHosts)).asJson
          case _ => fail()
        }
      }

    }
  }
}

class MockedNetworkingService(result: Either[String, Unit]) extends NetworkingService {
  override def ping(ip: String): IO[Boolean] = IO(true)

  override def sendMagicPocket(ip: String, mac: String): IO[Either[String, Unit]] = IO(result)
}