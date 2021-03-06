package io.tuliplogic.fractals.canvas

import io.tuliplogic.fractals.ColoredPoint
import scalafx.scene.canvas.{Canvas => SCanvas}
import scalaz.zio.ZIO

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  */
sealed trait Canvas {
  val canvas: Canvas.Service[SCanvas]
}

object Canvas {

  trait Service[R] {
    def drawPoint(p: ColoredPoint): ZIO[R, Nothing, Unit]
  }

  trait CanvasLive extends Canvas {

    val canvas = new Canvas.Service[SCanvas] {
      override def drawPoint(coloredPoint: ColoredPoint): ZIO[SCanvas, Nothing, Unit] = {
        def setColor: ZIO[SCanvas, Nothing, Unit] = ZIO.access(_.graphicsContext2D.setFill(coloredPoint.color))

        def drawOval: ZIO[SCanvas, Nothing, Unit] = ZIO.access(_.graphicsContext2D.fillOval(coloredPoint.x.toDouble, coloredPoint.y.toDouble, 1.0, 1.0))

        //issue: how to share context of this scanvas if points are drawn in parallel ? maybe with a semaphore with 1 permit
        setColor *> drawOval
      }
    }
  }

}