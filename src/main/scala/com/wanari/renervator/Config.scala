package com.wanari.renervator

import cats.effect.IO
import com.wanari.renervator.Database.Host

object Config {

  def readConfig = IO{
    List(Host("test", "127.0.0.1", "AA:AA:AA:AA:AA:AA"))
  }

  // This assumes files are three-columned comma-separated
  def readConfigFromFile(path: String) = IO{
    scala.io.Source.fromFile(path)
    .getLines()
    .map(_.split(","))
    .map { case a =>
      Host(a(0), a(1), a(2))
    }.toList
  }

}
