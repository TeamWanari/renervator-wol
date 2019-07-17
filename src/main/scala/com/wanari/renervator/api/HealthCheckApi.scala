package com.wanari.renervator.api

import cats.effect._
import com.wanari.renervator.api.HealthCheckApi.HealthCheckDTO
import org.http4s._
import org.http4s.dsl.io._

class HealthCheckApi {
  import io.circe.generic.auto._
  import org.http4s.circe.CirceEntityEncoder._

  val route = HttpRoutes.of[IO] {
    case GET -> Root / "healthCheck" =>
      Ok(
        HealthCheckDTO(
          true,
          com.wanari.renervator.BuildInfo.version,
          com.wanari.renervator.BuildInfo.builtAtString,
          com.wanari.renervator.BuildInfo.builtAtMillis,
          com.wanari.renervator.BuildInfo.commitHash
        )
      )
  }



}

object HealthCheckApi {

  case class HealthCheckDTO(
    success: Boolean,
    version: String,
    builtAtString: String,
    builtAtMillis: Long,
    commitHash: Option[String]
  )
}
