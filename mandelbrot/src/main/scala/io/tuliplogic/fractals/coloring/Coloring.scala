package io.tuliplogic.fractals.coloring

import scalafx.scene.paint.Color
import scalaz.zio.{UIO, ZIO}

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
trait Coloring {
  def coloring: Coloring.Service[Any]
}

object Coloring {

  trait Service[R] {
    def getColor(iter: Int, maxIterations: Int): ZIO[R, Nothing, Color]
  }

  trait AColoring extends Coloring {
    def coloring: Coloring.Service[Any] = new Service[Any] {
      def getColor(iter: Int, maxIterations: Int): UIO[Color] = {
        val res  = if (iter == maxIterations) {
          UIO.succeed(Color.Black)
        } else {
          val c = 3.0 * math.log(iter.toDouble) / math.log(maxIterations.toDouble - 1.0)
          if (c < 1) UIO.succeed(Color.rgb((255 * c).toInt, 0, 0))
          else if (c < 2) UIO.succeed(Color.rgb(255, (255 * (c - 1)).toInt, 0))
          else UIO.succeed(Color.rgb(255, 255, (255 * (c -  2)).toInt))
        }

        res
      }
    }


  }
}
