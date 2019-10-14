package com.wanari.renervator.api

import cats.SemigroupK.ops._
import cats.data.EitherT
import cats.effect._
import cats.syntax.option._
import com.wanari.renervator.Database
import com.wanari.renervator.Database.Host
import com.wanari.renervator.api.WakerApi._
import com.wanari.renervator.service.NetworkingService
import io.circe.Json
import io.circe.generic.auto._
import org.http4s._
import tapir._
import tapir.json.circe._
import tapir.model.StatusCodes

class WakerApi(database: Database, networkingService: NetworkingService)(implicit contextShift: ContextShift[IO]) {

  val wakeItUpApi: Endpoint[IdDTO, ErrorResponse, Json, Nothing] = endpoint
    .post
    .in("wakeItUp")
    .in(jsonBody[IdDTO])
    .out(jsonBody[Json])
    .errorOut(
      oneOf[ErrorResponse](
        statusMapping(StatusCodes.NotFound, jsonBody[ErrorResponse.NotFound.type]),
        statusMapping(StatusCodes.InternalServerError, jsonBody[ErrorResponse.Message])
      )
    )

  val hostListApi: Endpoint[Unit, Unit, HostListDTO, Nothing] = endpoint
    .get
    .in("wol")
    .out(jsonBody[HostListDTO])

  val hostApi: Endpoint[Long, ErrorResponse.NotFound.type, HostInfo, Nothing] = endpoint
    .get
    .in("hosts" / path[Long]("hostId"))
    .out(jsonBody[HostInfo])
    .errorOut(statusCode(model.StatusCodes.NotFound))
    .errorOut(jsonBody[ErrorResponse.NotFound.type])

  import tapir.server.http4s._

  val wakeItUpApiLogic: IdDTO => IO[Either[ErrorResponse, Json]] = (idWrapper: IdDTO) =>
    (for {
      host <- EitherT.fromOptionF[IO, ErrorResponse, Host](database.get(idWrapper.id), ErrorResponse.NotFound)
        _ <- EitherT(networkingService.sendMagicPocket(host)).leftMap[ErrorResponse](ErrorResponse.Message)
        result = Json.obj()
    } yield result).value

  val hostListLogic: Unit => IO[Either[Unit, HostListDTO]] = (_: Unit) =>
    database.all.map[Either[Unit, HostListDTO]](e => Right(HostListDTO(e)))

  val hostLogic: Long => IO[Either[ErrorResponse.NotFound.type, HostInfo]] = (id: Long) =>
    (for {
      host <- EitherT.fromOptionF(database.get(id), ErrorResponse.NotFound)
        hostInfo = HostInfo(host.name, host.ip, host.mac)
    } yield hostInfo).value

  val route: HttpRoutes[IO] =
    wakeItUpApi.serverLogic(wakeItUpApiLogic).toRoutes <+>
      hostListApi.serverLogic(hostListLogic).toRoutes <+>
      hostApi.serverLogic(hostLogic).toRoutes

}

object WakerApi {

  sealed abstract class ErrorResponse(message: Option[String]) extends Product with Serializable

  object ErrorResponse {

    case object NotFound extends ErrorResponse(None)

    final case class Message(message: String) extends ErrorResponse(message.some)

  }

  final case class IdDTO(id: Long)
  final case class HostListDTO(hosts: List[HostDTO])
  final case class HostDTO(id: Long, name: String, isOnline: Boolean)
  case class HostInfo(name: String, ip: String, mac: String)

  object HostListDTO {
    def apply(hosts: List[Host])(implicit dummy: DummyImplicit): HostListDTO = {
      HostListDTO(hosts.map(HostDTO(_)))
    }
  }

  object HostDTO {
    def apply(h: Host): HostDTO = HostDTO(h.id, h.name, h.isOnline)
  }
}
