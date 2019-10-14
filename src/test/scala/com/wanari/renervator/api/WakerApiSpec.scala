package com.wanari.renervator.api

import _root_.util.MockedNetworkingService
import cats.effect.{ContextShift, IO}
import com.wanari.renervator.Database
import com.wanari.renervator.Database.Host
import com.wanari.renervator.api.WakerApi.{ErrorResponse, HostDTO, HostInfo, HostListDTO, IdDTO}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.{Response, Status, Uri}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class WakerApiSpec extends WordSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val database: List[Host] => Database = (hosts: List[Host]) => {
    val database = new Database
    database.init(hosts).unsafeRunSync()
    database
  }

  "WakerAPI" when {
    "POST /wakeItUp" should {
      "return NotFound when id is not found in the database" in {
        val wakerApi = new WakerApi(
          database(List.empty[Host]),
          new MockedNetworkingService(Right(()))
        )

        val request = POST(IdDTO(1L).asJson, Uri.uri("/wakeItUp")).unsafeRunSync
        val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
        response match {
          case Some(response) => response.status shouldBe Status.NotFound
          case _ => fail()
        }
      }

      "return NotFound when NetworkingService#sendMagicPocket fails" in {
        val failedMessage = "I'm failed"
        val wakerApi = new WakerApi(
          database(List(Host("localhost", "localhost", "", isOnline = false, 1L))),
          new MockedNetworkingService(Left(failedMessage))
        )

        val request = POST(IdDTO(1L).asJson, Uri.uri("/wakeItUp")).unsafeRunSync
        val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
        response match {
          case Some(response) =>
            response.status shouldBe Status.InternalServerError
            response.as[Json].unsafeRunSync() shouldBe ErrorResponse.Message(failedMessage).asJson
          case _ => fail()
        }
      }

      "return OK when database is initialized" in {
        val wakerApi = new WakerApi(
          database(List(Host("localhost", "localhost", "", isOnline = false, 1L))),
          new MockedNetworkingService(Right(()))
        )

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

    "when database base is empty" should {

      val wakerApi = new WakerApi(
        database(List.empty[Host]),
        new MockedNetworkingService(Right(()))
      )
      "return no data from the database" when {
        "GET /wol" in {
          val request = GET(Uri.uri("/wol")).unsafeRunSync
          val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
          response match {
            case Some(response) =>
              response.status shouldBe Status.Ok
              response.as[Json].unsafeRunSync() shouldBe HostListDTO(List.empty[Host]).asJson
            case _ => fail()
          }
        }

        "GET /hosts/1" in {
          val request = GET(Uri.uri("/hosts/1")).unsafeRunSync
          val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
          response match {
            case Some(response) =>
              response.status shouldBe Status.NotFound
            case _ => fail()
          }
        }
      }
    }

    "when database base is initialized" should {
      val host = Host("localhost", "localhost", "", isOnline = false, 1L)
      val wakerApi = new WakerApi(
        database(List(host)),
        new MockedNetworkingService(Right(()))
      )
      "return data from the database" when {
        "GET /wol" in {
          val expectedHost = HostDTO(1L, "localhost", false)

          val request = GET(Uri.uri("/wol")).unsafeRunSync
          val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
          response match {
            case Some(response) =>
              response.status shouldBe Status.Ok
              response.as[Json].unsafeRunSync() shouldBe HostListDTO(List(expectedHost)).asJson
            case _ => fail()
          }
        }


        "GET /hosts/1" in {

          val request = GET(Uri.uri("/hosts/1")).unsafeRunSync
          val response: Option[Response[IO]] = wakerApi.route.run(request).value.unsafeRunSync()
          response match {
            case Some(response) =>
              response.status shouldBe Status.Ok
              response.as[Json].unsafeRunSync() shouldBe HostInfo(host.name, host.ip, host.mac).asJson
            case _ => fail()
          }
        }
      }

    }
  }
}