package io.tuliplogic.fractals.canvas

import io.tuliplogic.fractals.{Color, ColoredPoint}
import scalafx.scene.canvas.Canvas
import zio.{Ref, ZIO}

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  *
  */
// TODO: use a Canvas provider that provides a canvas abstraction
trait ZCanvas {
  def canvas: ZCanvas.Service[Any]
}

object ZCanvas {

  trait Service[R] {
    def drawPoint(p: ColoredPoint): ZIO[R, Nothing, Unit]
  }

  class CanvasFx(cvs: Ref[Canvas]) extends ZCanvas {

    private def setColor(coloredPoint: ColoredPoint)(canvas: Canvas): ZIO[Any, Nothing, Unit] =
      ZIO.effectTotal(
        canvas.graphicsContext2D.setFill(Color.toCanvasColor(coloredPoint.color))
      )

    private def drawOval(coloredPoint: ColoredPoint)(canvas: Canvas): ZIO[Any, Nothing, Unit] =
      ZIO.effectTotal(
        canvas.graphicsContext2D.fillOval(coloredPoint.pixel.x.toDouble, coloredPoint.pixel.y.toDouble, 1.0, 1.0)
      )

    def canvas: Service[Any] = new ZCanvas.Service[Any] {
      override def drawPoint(coloredPoint: ColoredPoint): ZIO[Any, Nothing, Unit] = for {
        theCvs <- cvs.get
        _      <- setColor(coloredPoint)(theCvs) *> drawOval(coloredPoint)(theCvs)
      } yield ()
    }
  }

}