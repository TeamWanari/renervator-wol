package com.wanari.renervator.service

import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import cats.effect.IO
import com.wanari.renervator.Database.Host

import scala.concurrent.ExecutionContext
import scala.util.Try

class NetworkingService {

  def ping(host: Host): IO[Boolean] = {
    ping(host.ip)
  }

  def ping(ip: String): IO[Boolean] = {
    IO {
      InetAddress.getByName(ip).isReachable(300)
    }
  }

  def sendMagicPocket(host: Host): IO[Either[String, Unit]] = {
    sendMagicPocket(host.ip, host.mac)
  }

  def sendMagicPocket(ip: String, mac: String): IO[Either[String, Unit]] = {
    getMacAddress(mac).fold[IO[Either[String, Unit]]](IO(Left(s"Not valid macAddress: $mac")))( macArr =>
      IO(sendMagicPacket(macArr, InetAddress.getByName(ip)))
    )
  }

  /*
  https://github.com/szkick/scala-wol/blob/master/src/main/scala/com/szkick/wol/WakeOnLAN.scala
   */
  private val WOL_PORT = 9

  private def hex2bytes(hexText: String): Array[Byte] = hexText.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray

  private def bytes2hex(bytes: Array[Byte]): String = bytes.map(b => f"$b%02X").mkString(":")

  private def getMacAddress(macText: String): Option[Array[Byte]] =
    hex2bytes("""([\dA-F]{2})""".r.findAllMatchIn(macText.toUpperCase).map(_.group(1)).mkString("")) match {
      case macBytes if macBytes.length == 6 => Some(macBytes)
      case _ => None
    }

  private def sendMagicPacket(macBytes: Array[Byte], address: InetAddress): Either[String, Unit] = {
    import cats.implicits._

    def generateMagicBytes(macBytes: Array[Byte]) = (List.fill(6)(0xFF.toByte) ::: List.fill(16)(macBytes.toList).flatten).toArray[Byte]

    def sendDatagramPacket(data: Array[Byte], address: InetAddress, port: Int) {
      val dPacket = new DatagramPacket(data, data.length, address, port)
      new DatagramSocket().send(dPacket)
    }

    Try {
      val magicBytes = generateMagicBytes(macBytes)
      sendDatagramPacket(magicBytes, address, WOL_PORT)
    }.toEither.leftMap(t => t.getMessage)
  }

}
