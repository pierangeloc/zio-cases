package io.tuliplogic.fractals

import zio.ZIO

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  */
package object canvas extends ZCanvas.Service[ZCanvas]  {
  override def drawPoint(p: ColoredPoint): ZIO[ZCanvas, Nothing, Unit] = ZIO.accessM(_.canvas.drawPoint(p))
}