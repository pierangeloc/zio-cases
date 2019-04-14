package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.Canvas
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy.{ParallelPoints, ParallelPointsAllPar, ParallelRows, ParallelSliced}
import scalaz.zio.console.Console
import scalaz.zio.{ZIO, clock, console}
import scalaz.zio.clock.Clock
import scalafx.scene.canvas.{Canvas => SCanvas}

/**
  *
  * mandelbrot - 2019-03-29
  * Created with ♥ in Amsterdam
  */
object fractal {

  sealed trait ComputationStrategy
  object ComputationStrategy {
    case class  ParallelPoints(parallelism: Int) extends ComputationStrategy
    case object ParallelPointsAllPar             extends ComputationStrategy
    case object ParallelRows                     extends ComputationStrategy
    case class  ParallelSliced(sliceSize: Int)   extends ComputationStrategy
  }

  def computeColor(x: Int, y: Int, complexRectangle: ComplexRectangle, maxIterations: Int, bailout: Int): ZIO[Coloring with FractAlgo, Nothing, ColoredPoint] = for {
    iter <- algo.iterations(complexRectangle.pixelToComplex(x, y), bailout, maxIterations)
    color <- coloring.getColor(iter, maxIterations)
  } yield ColoredPoint(x, y, color)

  def coloredPoints(
    complexRectangle: ComplexRectangle,
    resolution: Frame,
    maxIterations: Int,
    maxSquaredModule: Int
  )(strategy: ComputationStrategy): ZIO[Coloring with FractAlgo, Nothing, List[ColoredPoint]] =
    strategy match {
      case ParallelPoints(n) =>
        ZIO.foreachParN(n)(resolution.allPoints) {
          case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
        }
      case ParallelPointsAllPar =>
        ZIO.foreachPar(resolution.allPoints) {
          case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
        }
      case ParallelRows =>
        ZIO.foreachPar(resolution.rows) { row =>
          ZIO.foreach(row) {
            case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
          }
        }.map(_.flatten)
      case ParallelSliced(n) =>
        ZIO.foreachPar(resolution.allPoints.sliding(n, n).toList) { row =>
          ZIO.foreach(row) {
            case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
          }
        }.map(_.flatten)
    }

  def calculationProgram(strategy: ComputationStrategy)(maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int): ZIO[Console with Clock with Coloring with FractAlgo, Nothing, (List[ColoredPoint], Long)] = for {
    startCalc        <- clock.nanoTime
    _                <- console.putStrLn(s"Computing with strategy: $strategy")
    resolution       <- ZIO.succeed(Frame(frameWidth, frameHeight))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- coloredPoints(complexRectangle, resolution, maxIterations, maxSquaredModule)(strategy)
    computationNanos <- clock.nanoTime.map(_ - startCalc)

    _                <- console.putStrLn(s"calculated all ${coloredPoints.size} points; Computation of colors took ${computationNanos / 1000000} ms")
  } yield (coloredPoints, computationNanos)

  def calculationAndDrawingProgram(width: Int, height: Int)(strategy: ComputationStrategy): ZIO[Console with Clock with Canvas with SCanvas with Coloring with FractAlgo, Nothing, Unit] = for {
    coloredPoints <- calculationProgram(strategy)(5000, 8, width, height)
    startDraw     <- clock.nanoTime
    _             <- ZIO.foreach(coloredPoints._1)(coloredPoint => canvas.drawPoint(coloredPoint))
    endDraw       <- clock.nanoTime
    _             <- console.putStrLn(s"Drawing canvas took ${(endDraw - startDraw) / 1000000} ms ")
  } yield ()

}
