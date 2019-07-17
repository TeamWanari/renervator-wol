package com.wanari.renervator

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.implicits._
import com.wanari.renervator.api.{HealthCheckApi, WakerApi}
import com.wanari.renervator.service.{NetworkingService, PingerService}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val database = new Database()
  val networkingService = new NetworkingService()

  val pinger = new PingerService(database, networkingService)

  val healthCheckApi = new HealthCheckApi()
  val wakeupApi = new WakerApi(database, networkingService)

  val apis = healthCheckApi.route <+> wakeupApi.route

  val httpApp = Router("/" -> apis).orNotFound

  def run(args: List[String]): IO[ExitCode] = {
    for {
      hosts <- Config.readConfig
      db <- database.init(hosts)
      _ <- pinger.loop.start
      server <- startServer
    } yield server
  }

  private def startServer: IO[ExitCode] = BlazeServerBuilder[IO]
    .bindHttp(8080, "127.0.0.1")
    .withHttpApp(httpApp)
    .serve
    .compile
    .drain
    .as(ExitCode.Success)

}
