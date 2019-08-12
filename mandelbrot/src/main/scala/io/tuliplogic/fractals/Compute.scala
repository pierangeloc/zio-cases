package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractalAlgo
import io.tuliplogic.fractals.canvas.ZCanvas
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy.{ParallelPoints, ParallelPointsAllPar, ParallelRows, ParallelSliced}
import zio.{UIO, ZIO}

object Compute {

  private def computeColor(p: Pixel): ZIO[Coloring with FractalAlgo with Config, Nothing, ColoredPoint] =
    for {
      complexRectangle    <- config.complexRectangle
      divergenceThreshold <- config.divergenceThreshold
      maxIterations       <- config.maxIterations
      iter                <- algo.iterations(complexRectangle.pixelToComplex(p), divergenceThreshold, maxIterations)
      color               <- coloring.getColor(iter, maxIterations)
    } yield ColoredPoint(p, color)

  private def onAllPoints[R, A](f: Pixel => ZIO[R, Nothing, A]): ZIO[R with Config, Nothing, Unit] =
    for {
      compStrat  <- config.computationStrategy
      compRect   <- config.complexRectangle
      resolution <- UIO.succeed(compRect.resolution)
      _          <- compStrat match {
        case ParallelPoints(n) =>
          ZIO.foreachParN(n)(resolution.allPoints)(f)
        case ParallelPointsAllPar =>
          ZIO.foreachPar(resolution.allPoints)(f)
        case ParallelRows =>
          ZIO.foreachPar(resolution.rows) { row =>
              ZIO.foreach(row)(f)
            }.map(_.flatten)
        case ParallelSliced(n) =>
          ZIO.foreachPar(resolution.allPoints.sliding(n, n).toList) { row =>
              ZIO.foreach(row)(f)
            }.map(_.flatten)
      }
    } yield ()

  def program: ZIO[ZCanvas with Coloring with FractalAlgo with Config, Nothing, Unit] =
    onAllPoints { pixel =>
      for {
        coloredPoint <- computeColor(pixel)
        _            <- canvas.drawPoint(coloredPoint)
      } yield ()
    }

}
