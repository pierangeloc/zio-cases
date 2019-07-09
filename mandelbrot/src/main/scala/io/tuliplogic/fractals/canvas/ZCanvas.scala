package io.tuliplogic.fractals.canvas

import io.tuliplogic.fractals.{Color, ColoredPoint}
import scalafx.scene.canvas.{Canvas => JCanvas}
import zio.{Queue, ZIO}

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  *
  */
trait ZCanvas {
  def canvas: ZCanvas.Service[Any]
}

object ZCanvas {

  trait Service[R] {
    def drawPoint(p: ColoredPoint): ZIO[R, Nothing, Unit]
  }

  val jCanvas: ZIO[JCanvas, Nothing, ZCanvas] = ZIO.access {

    jCanvas => {
        def setColor(coloredPoint: ColoredPoint): ZIO[Any, Nothing, Unit] = ZIO.effectTotal(
          jCanvas.graphicsContext2D.setFill(Color.toCanvasColor(coloredPoint.color))
        )

        def drawOval(coloredPoint: ColoredPoint): ZIO[Any, Nothing, Unit] = ZIO.effectTotal(
          jCanvas.graphicsContext2D.fillOval(coloredPoint.pixel.x.toDouble, coloredPoint.pixel.y.toDouble, 1.0, 1.0)
        )

        new ZCanvas {
          def canvas: Service[Any] = new Service[Any] {
          override def drawPoint(coloredPoint: ColoredPoint): ZIO[Any, Nothing, Unit] =
            setColor(coloredPoint) *> drawOval(coloredPoint)
        }
      }
    }
  }

  val queueCanvas: ZIO[Queue[ColoredPoint], Nothing, ZCanvas] = ZIO.access {
    queue => new ZCanvas {
        def canvas: Service[Any] = new Service[Any] {
          override def drawPoint(coloredPoint: ColoredPoint): ZIO[Any, Nothing, Unit] =
            queue.offer(coloredPoint).unit
        }
      }
  }

}