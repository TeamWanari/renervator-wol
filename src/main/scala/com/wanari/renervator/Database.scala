package com.wanari.renervator

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import com.wanari.renervator.Database.Host

import scala.util.Try

class Database {
  import collection.JavaConverters._
  private val map = new ConcurrentHashMap[Long, Host]()

  def get(id: Long): IO[Option[Host]] = {
    IO{
      Try(Option(map.get(id))).toOption.flatten
    }
  }

  def updateHost(host: Host): IO[Unit] = {
    IO{
      map.put(host.id, host)
    }
  }

  def init(hosts: List[Host]): IO[Unit] = {
    IO(
      hosts
        .zipWithIndex
        .map({case (host, id) => host.copy(id = id+1)})
        .foreach{host => map.put(host.id, host)}
    )
  }

  def all: IO[List[Host]] = {
    IO(map.values.asScala.toList)
  }

}

object Database {
  case class Host(name: String, ip: String, mac: String, isOnline: Boolean = false, id: Long = 0)
}
