package io.tuliplogic.fractals.coloring

import scalafx.scene.paint.Color
import scalaz.zio.UIO

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
trait Coloring {
  def coloring: Coloring.Service
}

object Coloring {

  trait Service {
    def getColor(iter: Int, maxIterations: Int): UIO[Color]
  }

  trait MyColoring extends Coloring.Service {
    def getColor(iter: Int, maxIterations: Int): UIO[Color] =
      if (iter == maxIterations) UIO.succeed(Color.Black)
      else {
        val c = 3 * math.log(iter) / math.log(maxIterations - 1.0)
        if (c < 1) UIO.succeed(Color.rgb((255 * c).toInt, 0, 0))
        else if (c < 2) UIO.succeed(Color.rgb(255, (255 * (c - 1)).toInt, 0))
        else UIO.succeed(Color.rgb(255, 255, (255 * (c -  2)).toInt))
      }

  }
}
