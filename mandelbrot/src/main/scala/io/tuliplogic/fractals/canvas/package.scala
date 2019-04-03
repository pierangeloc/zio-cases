package io.tuliplogic.fractals

import scalaz.zio.ZIO
import scalafx.scene.canvas.{Canvas => SCanvas}
import scalaz.zio.console.Console

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  * TODO: I don't like this Canvas with SCanvas... think about it
  */
package object canvas extends Canvas.Service[Canvas with SCanvas] {
  override def drawPoint(p: ColoredPoint): ZIO[Canvas with SCanvas, Nothing, Unit] = ZIO.accessM(_.canvas.drawPoint(p))
}