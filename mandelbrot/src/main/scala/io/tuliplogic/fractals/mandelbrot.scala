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

  rts.unsafeRun (algo.program.provide(env))
}