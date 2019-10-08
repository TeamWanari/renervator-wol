package com.wanari.renervator.api

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._

import scala.concurrent.ExecutionContext.global

class FrontendApi(implicit ctx: ContextShift[IO]) {

  private def static(file: String, request: Request[IO]): IO[Response[IO]] =
    StaticFile.fromResource("/static/" + file, global, Some(request)).getOrElseF(NotFound())

  val route: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root => static("index.html", request)
    case request @ GET -> Root / path if List(".js", ".css", ".map", ".html", ".webm").exists(path.endsWith) =>
      static(path, request)
  }

}
