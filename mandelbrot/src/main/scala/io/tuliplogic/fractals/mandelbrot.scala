package io.tuliplogic.fractals

import io.tuliplogic.fractals.canvas.Canvas
import io.tuliplogic.fractals.canvas.Canvas.CanvasLive
import scalafx.scene.paint.Color
import scalaz.zio.console.Console
import scalaz.zio.clock
import scalaz.zio.clock.Clock
import scalaz.zio.{DefaultRuntime, UIO, ZIO, console}
import scalafx.scene.canvas.{Canvas => SCanvas}

import scala.annotation.tailrec

/**
 * 
 * mandelbrot - 2019-03-29
 * Created with ♥ in Amsterdam
 */
object mandelbrot {




  class MandelbrotAlgo(maxIterations:Int) {

    def iterate(c: Complex, bailout: Int): Int = {

      @tailrec
      def run(z: Complex, iter: Int): Int =
        if (iter >= maxIterations ||  z.squaredAbs > bailout)
          iter
        else
          run(Complex(1, 0) * z * z + c, iter + 1)

      run(Complex.zero, 0)
    }

    def getColor(iter: Int): Color = {
      if (iter == maxIterations) return Color.Black

      val c = 3 * math.log(iter) / math.log(maxIterations - 1.0)
      if (c < 1) Color.rgb((255 * c).toInt, 0, 0)
      else if (c < 2) Color.rgb(255, (255 * (c - 1)).toInt, 0)
      else Color.rgb(255, 255, (255 * (c -  2)).toInt)
    }
  }

  object MandelbrotAlgo {
    def apply(nrIterations: Int) = new MandelbrotAlgo(nrIterations)
  }

  def computeColor(x: Int, y: Int, frame: Frame, complexRectangle: ComplexRectangle, mandelbrot: MandelbrotAlgo): ZIO[Any, Nothing, ColoredPoint] = for {
    iter  <- ZIO.effectTotal(mandelbrot.iterate(complexRectangle.pixelToComplex(frame)(x, y),  8))
    color <- ZIO.succeed(mandelbrot.getColor(iter))
  } yield ColoredPoint(x, y, color)

  def program: ZIO[Canvas with Console with Clock, Nothing, Unit] = for {
    start            <- clock.nanoTime
    frame            <- UIO.succeed(Frame())
    complexRectangle <- UIO.succeed(ComplexRectangle(-2, 1, -1, 1))
    mandelbrot       <- UIO.succeed(MandelbrotAlgo(1000))
    coloredPoints    <- ZIO.foreachPar(frame.allPoints) {
                          case (x, y) => computeColor(x, y, frame, complexRectangle, mandelbrot)
                        }
    end              <- clock.nanoTime
    _                <- console.putStr(s"calculated all points; it took ${(end - start) / 1000000} ms; coloredPoints = \n${coloredPoints.take(20).mkString("\n")}")
    _                <- ZIO.foreach(coloredPoints)(coloredPoint => canvas.drawPoint(coloredPoint))
  } yield ()

}

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._

object MandelbrotFX extends JFXApp { self =>

  val canvas = new SCanvas(600, 400)

  stage = new PrimaryStage {
    title = "Functional Mandelbrot"

    scene = new Scene {
      fill = Black
      content = new HBox {
        padding = Insets(20)
        children = Seq(
          canvas
        )
      }
    }
  }

  val rts = new DefaultRuntime {}
  val env = new CanvasLive with Console.Live with Clock.Live {
    override val scanvas: SCanvas = self.canvas
  }

  rts.unsafeRun (mandelbrot.program.provide(env))
}