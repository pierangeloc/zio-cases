package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.Canvas
import io.tuliplogic.fractals.canvas.Canvas.CanvasLive
import io.tuliplogic.fractals.coloring.Coloring
import scalaz.zio.console.Console
import scalaz.zio.{App, DefaultRuntime, Runtime, UIO, ZIO, clock, console}
import scalaz.zio.clock.Clock
import scalafx.scene.canvas.{Canvas => SCanvas}

/**
  *
  * mandelbrot - 2019-03-29
  * Created with ♥ in Amsterdam
  */
object fractal {

  def computeColor(x: Int, y: Int, complexRectangle: ComplexRectangle, maxIterations: Int, bailout: Int): ZIO[Coloring with FractAlgo, Nothing, ColoredPoint] = for {
    iter <- algo.iterate(complexRectangle.pixelToComplex(x, y), bailout, maxIterations)
    color <- coloring.getColor(iter, maxIterations)
  } yield ColoredPoint(x, y, color)

  def calculationProgram(par: Option[Long]): ZIO[Console with Clock with Coloring with FractAlgo, Nothing, (List[ColoredPoint], Long)] = for {
    startCalc        <- clock.nanoTime
    maxIterations    <- ZIO.succeed(1000)
    bailout          <- ZIO.succeed(8)
    resolution       <- ZIO.succeed(Frame(600, 400))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- par.fold(ZIO.foreachPar(resolution.allPoints) {
      case (x, y) => computeColor(x, y, complexRectangle, maxIterations, bailout)
    })(n => ZIO.foreachParN(n)(resolution.allPoints) {
      case (x, y) => computeColor(x, y, complexRectangle, maxIterations, bailout)
    })
    computationNanos  <- clock.nanoTime.map(_ - startCalc)

    _                <- console.putStr(s"calculated all ${coloredPoints.size} points; Computation of colors took ${computationNanos / 1000000} ms")
  } yield (coloredPoints, computationNanos)


  def calculationAndDrawingProgram: ZIO[Canvas with SCanvas with Console with Clock with Coloring with FractAlgo, Nothing, Unit] = for {
    coloredPoints <- calculationProgram(None)
    startDraw     <- clock.nanoTime
    _             <- ZIO.foreach(coloredPoints._1)(coloredPoint => canvas.drawPoint(coloredPoint))
    endDraw       <- clock.nanoTime
    _             <- console.putStr(s"Drawing canvas took ${(endDraw - startDraw) / 1000000} ms ")
  } yield ()

}
