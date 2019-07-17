package com.wanari.renervator

import cats.effect.IO
import com.wanari.renervator.Database.Host

object Config {

  def readConfig = IO{
    List(Host("test", "127.0.0.1", "AA:AA:AA:AA:AA:AA"))
  }

}
