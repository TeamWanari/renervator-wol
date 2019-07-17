package com.wanari.renervator.service

import com.wanari.renervator.Database
import com.wanari.renervator.Database.Host

import scala.concurrent.duration._

class PingerService(database: Database, networkingService: NetworkingService) {
  import cats.effect._
  import cats.implicits._


  def loop(implicit timerIo: Timer[IO], contextShift: ContextShift[IO]): IO[Unit] = {
    for {
      hosts <- database.all
      pings <- hosts.parTraverse(pingHost)
      _ <- Timer[IO].sleep(30.seconds)
      b <- loop
    } yield ()
  }

  private def pingHost(host: Host): IO[Unit] = {
    for {
      isOnline <- networkingService.ping(host)
      updatedHost = host.copy(isOnline = isOnline)
      _ <- database.updateHost(updatedHost)
      _ <- IO(println(isOnline))
      _ <- IO(println(updatedHost))
    } yield ()
  }

}
