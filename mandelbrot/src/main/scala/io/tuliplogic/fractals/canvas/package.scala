package io.tuliplogic.fractals

import scalaz.zio.ZIO

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  */
package object canvas extends Canvas.Service[Canvas] {
  override def drawPoint(p: ColoredPoint): ZIO[Canvas, Nothing, Unit] = ZIO.accessM(_.canvas.drawPoint(p))
}