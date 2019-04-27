package io.tuliplogic.fractals.canvas

import io.tuliplogic.fractals.ColoredPoint
import scalafx.scene.canvas.Canvas
import scalaz.zio.ZIO

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  */
sealed trait ZCanvas[DrawOn] {
  def canvas: ZCanvas.Service[DrawOn, Any]
}

object ZCanvas {

  trait Service[-DrawOn, -R] {
    def drawPoint(p: ColoredPoint)(drawOn: DrawOn): ZIO[R, Nothing, Unit]
  }

  trait ZCanvasFxLive extends ZCanvas[Canvas] {

    val canvas = new ZCanvas.Service[Canvas, Any] {
      override def drawPoint(coloredPoint: ColoredPoint)(drawOn: Canvas): ZIO[Any, Nothing, Unit] = {
        def setColor: ZIO[Canvas, Nothing, Unit] = ZIO.access(_.graphicsContext2D.setFill(coloredPoint.color))
        def drawOval: ZIO[Canvas, Nothing, Unit] = ZIO.access(_.graphicsContext2D.fillOval(coloredPoint.pixel.x.toDouble, coloredPoint.pixel.y.toDouble, 1.0, 1.0))
        (setColor *> drawOval).provide(drawOn)
      }
    }
  }

}