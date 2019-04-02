package io.tuliplogic.fractals.canvas

import io.tuliplogic.fractals.ColoredPoint
import scalafx.scene.canvas.{Canvas => SCanvas}
import scalaz.zio.{UIO, ZIO}

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  */
//canvas component
sealed trait Canvas {
  val canvas: Canvas.Service[Any]
}

object Canvas {

  trait Service[-R] {
    def drawPoint(p: ColoredPoint): ZIO[R, Nothing, Unit]
  }

  trait CanvasLive extends Canvas {
    val scanvas: SCanvas

    val canvas = new Canvas.Service[Any] {
      override def drawPoint(coloredPoint: ColoredPoint): ZIO[Any, Nothing, Unit] = {
        def setColor: UIO[Unit] = ZIO.effectTotal(scanvas.graphicsContext2D.setFill(coloredPoint.color))

        def drawOval: UIO[Unit] = ZIO.effectTotal(scanvas.graphicsContext2D.fillOval(coloredPoint.x.toDouble, coloredPoint.y.toDouble, 1.0, 1.0))

        //issue: how to share context of this scanvas if points are drawn in parallel ? maybe with a semaphore with 1 permit
        setColor *> drawOval
      }
    }
  }

  object CanvasLive {
    def withScanvas(c: SCanvas): CanvasLive = new CanvasLive {
      override val scanvas: SCanvas = c
    }
  }

}