package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.Canvas
import io.tuliplogic.fractals.canvas.Canvas.CanvasLive
import io.tuliplogic.fractals.coloring.Coloring
import scalaz.zio.console.Console
import scalaz.zio.{DefaultRuntime, Runtime, UIO, ZIO, clock, console}
import scalaz.zio.clock.Clock
import scalafx.scene.canvas.{Canvas => SCanvas}

/**
 * 
 * mandelbrot - 2019-03-29
 * Created with ♥ in Amsterdam
 */
object fractal {

  def computeColor(x: Int, y: Int, complexRectangle: ComplexRectangle, maxIterations: Int, bailout: Int): ZIO[Coloring with FractAlgo, Nothing, ColoredPoint] = for {
    iter  <- algo.iterate(complexRectangle.pixelToComplex(x, y), bailout, maxIterations)
    color <- coloring.getColor(iter, maxIterations)
  } yield ColoredPoint(x, y, color)

  def program: ZIO[Canvas with SCanvas with Console with Clock with Coloring with FractAlgo, Nothing, Unit] = for {
    start            <- clock.nanoTime
    maxIterations    <- ZIO.succeed(1000)
    bailout          <- ZIO.succeed(8)
    resolution       <- ZIO.succeed(Frame(400, 600))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- ZIO.foreachPar(resolution.allPoints) {
                          case (x, y) => computeColor(x, y, complexRectangle, maxIterations, bailout)
                        }
    end              <- clock.nanoTime
    _                <- console.putStr(s"calculated all ${coloredPoints.size} points; it took ${(end - start) / 1000000} ms; coloredPoints = \n${coloredPoints.take(20).mkString("\n")}")
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
  val env = new SCanvas(600, 400) with CanvasLive with Console.Live with Clock.Live with Coloring.AColoring with FractAlgo.MandelbrotAlgo {}

  rts.unsafeRun (fractal.program.provide(env))
}