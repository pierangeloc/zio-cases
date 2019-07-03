package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractalAlgo
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy
import io.tuliplogic.fractals.fractal.ComputationStrategy.{ParallelPoints, ParallelPointsAllPar, ParallelRows, ParallelSliced}
import zio.{Queue, ZIO}

trait Compute {
  def service: Compute.Service[Any]
}

object Compute {

  trait Service[-R] {
    def computeAndDisplay(computationStrategy: ComputationStrategy)(maxIterations: Int, divergeThreshold: Int)(complexRectangle: ComplexRectangle): ZIO[R, Nothing, Unit]
  }

  class QueueComputeAndDisplay(queue: Queue[ColoredPoint]) extends Compute {

    def computeColor(complexRectangle: ComplexRectangle, maxIterations: Int, divergeThreshold: Int)(p: Pixel):
      ZIO[Coloring with FractalAlgo, Nothing, ColoredPoint] = for {
      iter  <- algo.iterations(complexRectangle.pixelToComplex(p), divergeThreshold, maxIterations)
      color <- coloring.getColor(iter, maxIterations)
    } yield ColoredPoint(p, color)

    def onAllPoints[R, A](
      resolution: Frame,
    )(strategy: ComputationStrategy)(f: Pixel => ZIO[R, Nothing, A]):
      ZIO[R, Nothing, List[A]] =
      strategy match {
        case ParallelPoints(n) =>
          ZIO.foreachParN(n)(resolution.allPoints)(f)
        case ParallelPointsAllPar =>
          ZIO.foreachPar(resolution.allPoints)(f)
        case ParallelRows =>
          ZIO.foreachPar(resolution.rows) { row =>
            ZIO.foreach(row) (f)
          }.map(_.flatten)
        case ParallelSliced(n) =>
          ZIO.foreachPar(resolution.allPoints.sliding(n, n).toList) { row =>
            ZIO.foreach(row) (f)
          }.map(_.flatten)
      }

    override def service: Service[Any] = new Service[Any] {
      override def computeAndDisplay(computationStrategy: ComputationStrategy)
        (maxIterations: Int, divergeThreshold: Int)
        (complexRectangle: ComplexRectangle): ZIO[Coloring with FractalAlgo, Nothing, Unit] =
        onAllPoints(complexRectangle.resolution)(computationStrategy)(computeColor(complexRectangle, maxIterations, divergeThreshold)).unit


    }
  }
}
