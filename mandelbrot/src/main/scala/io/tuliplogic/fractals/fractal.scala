package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.ZCanvas
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy.{ParallelPoints, ParallelPointsAllPar, ParallelRows, ParallelSliced}
import scalaz.zio.console.Console
import scalaz.zio.{Semaphore, ZIO, clock, console}
import scalaz.zio.clock.Clock
import scalaz.zio.duration._

object fractal {

  sealed trait ComputationStrategy
  object ComputationStrategy {
    case class  ParallelPoints(parallelism: Int) extends ComputationStrategy
    case object ParallelPointsAllPar             extends ComputationStrategy
    case object ParallelRows                     extends ComputationStrategy
    case class  ParallelSliced(sliceSize: Int)   extends ComputationStrategy
  }

  def computeColor(complexRectangle: ComplexRectangle, maxIterations: Int, bailout: Int)(p: Pixel): ZIO[Coloring with FractAlgo, Nothing, ColoredPoint] = for {
    iter  <- algo.iterations(complexRectangle.pixelToComplex(p), bailout, maxIterations)
    color <- coloring.getColor(iter, maxIterations)
  } yield ColoredPoint(p, color)

  //f = computeColor(complexRectangle, maxIterations, maxSquaredModule).tupled

  def onAllPoints[R, A](
    resolution: Frame,
  )(strategy: ComputationStrategy)(f: Pixel => ZIO[R, Nothing, A]): ZIO[R, Nothing, List[A]] =
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

  def calculate(strategy: ComputationStrategy)
    (maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int): ZIO[Console with Clock with Coloring with FractAlgo, Nothing, (List[ColoredPoint], Long)] = for {
    startCalc        <- clock.nanoTime
    _                <- console.putStrLn(s"Computing with strategy: $strategy")
    resolution       <- ZIO.succeed(Frame(frameWidth, frameHeight))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- onAllPoints(resolution)(strategy)(computeColor(complexRectangle, maxIterations, maxSquaredModule))
    computationNanos <- clock.nanoTime.map(_ - startCalc)
    _                <- console.putStrLn(s"calculated colors of all ${coloredPoints.size} points; Took ${computationNanos / 1000000} ms")
  } yield (coloredPoints, computationNanos)

  def calculateAllAndDrawAll[DrawOn](strategy: ComputationStrategy)
    (maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int)
    (drawOn: DrawOn): ZIO[Console with Clock with ZCanvas[DrawOn] with Coloring with FractAlgo, Nothing, Unit] = for {
    coloredPoints <- calculate(strategy)(maxIterations, maxSquaredModule, frameWidth, frameHeight)
    startDraw     <- clock.nanoTime
    _             <- ZIO.foreach(coloredPoints._1)(coloredPoint => canvas.canvasService[DrawOn].drawPoint(coloredPoint)(drawOn))
    endDraw       <- clock.nanoTime
    _             <- console.putStrLn(s"Drawing canvas took ${(endDraw - startDraw) / 1000000} ms ")
  } yield ()

  def calculateAndDraw[DrawOn](strategy: ComputationStrategy)
    (maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int)
    (drawOn: DrawOn): ZIO[Console with Clock with ZCanvas[DrawOn] with Coloring with FractAlgo, Nothing, Unit] = for {
    startCalc        <- clock.nanoTime
    _                <- console.putStrLn(s"Computing with strategy: $strategy")
    resolution       <- ZIO.succeed(Frame(frameWidth, frameHeight))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    semaphore        <- Semaphore.make(1)
    _                <- onAllPoints(resolution)(strategy) { p =>
    //TODO: the withPermit should be performed in the drawPoint, it's not a concern of this method

                          computeColor(complexRectangle, maxIterations, maxSquaredModule)(p)
                            .flatMap(coloredPoint =>
                              semaphore.withPermit(ZIO.sleep(100.microsecond) *> canvas.canvasService[DrawOn].drawPoint(coloredPoint)(drawOn)
                              )
                            )
//                          computeColor(complexRectangle, maxIterations, maxSquaredModule)(p).flatMap(coloredPoint => canvas.canvasService[DrawOn].drawPoint(coloredPoint)(drawOn))
                        }
    computationNanos <- clock.nanoTime.map(_ - startCalc)
    _                <- console.putStrLn(s"calculated and drawn all ${resolution.height * resolution.width} points; Took ${computationNanos / 1000000} ms")
  } yield ()


}
