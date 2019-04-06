package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.Canvas
import io.tuliplogic.fractals.coloring.Coloring
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

  def computeColor(x: Int, y: Int, complexRectangle: ComplexRectangle, maxIterations: Int, bailout: Int): ZIO[Coloring with FractAlgo, Nothing, ColoredPoint] = for {
    iter <- algo.iterations(complexRectangle.pixelToComplex(x, y), bailout, maxIterations)
    color <- coloring.getColor(iter, maxIterations)
  } yield ColoredPoint(x, y, color)

  def calculationProgramParallelPoints(maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int, par: Option[Long]): ZIO[Console with Clock with Coloring with FractAlgo, Nothing, (List[ColoredPoint], Long)] = for {
    startCalc        <- clock.nanoTime
    _                <- console.putStrLn("Computing with parallel points method")
    resolution       <- ZIO.succeed(Frame(frameWidth, frameHeight))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- par.fold(ZIO.foreachPar(resolution.allPoints) {
                          case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
                        })(n => ZIO.foreachParN(n)(resolution.allPoints) {
                          case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
                        })
    computationNanos  <- clock.nanoTime.map(_ - startCalc)

    _                <- console.putStrLn(s"calculated all ${coloredPoints.size} points; Computation of colors took ${computationNanos / 1000000} ms")
  } yield (coloredPoints, computationNanos)

  def calculationProgramParallelRows(maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int, par: Option[Long]): ZIO[Console with Clock with Coloring with FractAlgo, Nothing, (List[ColoredPoint], Long)] = for {
    startCalc        <- clock.nanoTime
    _                <- console.putStrLn("Computing with parallel rows method")
    resolution       <- ZIO.succeed(Frame(frameWidth, frameHeight))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- ZIO.foreach(resolution.rows) { row =>
                          ZIO.foreach(row){
                            case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
                          }
                        }.map(_.flatten)
    computationNanos  <- clock.nanoTime.map(_ - startCalc)

    _                <- console.putStrLn(s"calculated all ${coloredPoints.size} points; Computation of colors took ${computationNanos / 1000000} ms")
  } yield (coloredPoints, computationNanos)

  def calculationProgramFullySequential(maxIterations: Int, maxSquaredModule: Int, frameWidth: Int, frameHeight: Int, par: Option[Long]): ZIO[Console with Clock with Coloring with FractAlgo, Nothing, (List[ColoredPoint], Long)] = for {
    startCalc        <- clock.nanoTime
    _                <- console.putStrLn("Computing with fully sequential method")
    resolution       <- ZIO.succeed(Frame(frameWidth, frameHeight))
    complexRectangle <- ZIO.succeed(ComplexRectangle(-2, 1, -1, 1, resolution))
    coloredPoints    <- ZIO.foreach(resolution.allPoints) {
        case (x, y) => computeColor(x, y, complexRectangle, maxIterations, maxSquaredModule)
      }
    computationNanos  <- clock.nanoTime.map(_ - startCalc)

    _                <- console.putStrLn(s"calculated all ${coloredPoints.size} points; Computation of colors took ${computationNanos / 1000000} ms")
  } yield (coloredPoints, computationNanos)

  def calculationAndDrawingProgram(width: Int, height: Int): ZIO[Canvas with SCanvas with Console with Clock with Coloring with FractAlgo, Nothing, Unit] = for {
//    coloredPoints <- calculationProgram(5000, 8, 600, 400, Some(600))
    coloredPoints <- calculationProgramParallelPoints(5000, 8, width, height, Some(600))
//    coloredPoints <- calculationProgramParallelRows(5000, 8, width, height, Some(600))
//    coloredPoints <- calculationProgramFullySequential(5000, 8, width, height, Some(600))
    startDraw     <- clock.nanoTime
    _             <- ZIO.foreach(coloredPoints._1)(coloredPoint => canvas.drawPoint(coloredPoint))
    endDraw       <- clock.nanoTime
    _             <- console.putStrLn(s"Drawing canvas took ${(endDraw - startDraw) / 1000000} ms ")
  } yield ()

}
