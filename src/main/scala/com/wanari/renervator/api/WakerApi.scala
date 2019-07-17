package com.wanari.renervator.api

import cats.data.OptionT
import cats.effect._
import com.wanari.renervator.Database
import com.wanari.renervator.Database.Host
import com.wanari.renervator.api.WakerApi.{HostListDTO, IdDTO}
import com.wanari.renervator.service.NetworkingService
import io.circe.Json
import org.http4s._
import org.http4s.dsl.io._

class WakerApi(database: Database, networkingService: NetworkingService) {
  import io.circe.generic.auto._
  import org.http4s.circe.CirceEntityDecoder._
  import org.http4s.circe.CirceEntityEncoder._

  val route = HttpRoutes.of[IO] {
    case req @ POST -> Root / "wakeItUp" =>
      (for {
        idWrapper <- OptionT.liftF[IO, IdDTO](req.as[IdDTO])
        host <- OptionT[IO, Host](database.get(idWrapper.id))
        sent <- OptionT.liftF(networkingService.sendMagicPocket(host))
      } yield {
        sent.fold(_ => NotFound(), _ => Ok(Json.obj()))
      }).fold(NotFound())(identity).flatMap(identity)

    case GET -> Root =>
      database.all.flatMap(l => Ok(HostListDTO(l)))
  }
}

object WakerApi {

  case class IdDTO(id: Long)
  case class HostListDTO(hosts: List[HostDTO])
  case class HostDTO(id: Long, name: String, isOnline: Boolean)

  object HostListDTO {
    def apply(hosts: List[Host])(implicit dummy: DummyImplicit): HostListDTO = {
      HostListDTO(hosts.map(HostDTO(_)))
    }
  }

  object HostDTO {
    def apply(h: Host): HostDTO = HostDTO(h.id, h.name, h.isOnline)
  }
}
