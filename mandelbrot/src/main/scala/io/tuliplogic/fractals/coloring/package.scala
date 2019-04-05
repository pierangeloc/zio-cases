package io.tuliplogic.fractals
import scalafx.scene.paint.Color
import scalaz.zio.{UIO, ZIO}

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
package object coloring extends Coloring.Service[Coloring] {
  override def getColor(iter: Int, maxIterations: Int): ZIO[Coloring, Nothing, Color] = ZIO.accessM(_.coloring.getColor(iter, maxIterations))
}
