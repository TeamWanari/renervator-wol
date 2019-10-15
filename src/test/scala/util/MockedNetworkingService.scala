package util

import cats.effect.IO
import com.wanari.renervator.service.NetworkingService


class MockedNetworkingService(result: Either[String, Unit]) extends NetworkingService {
  override def ping(ip: String): IO[Boolean] = IO(true)

  override def sendMagicPocket(ip: String, mac: String): IO[Either[String, Unit]] = IO(result)
}