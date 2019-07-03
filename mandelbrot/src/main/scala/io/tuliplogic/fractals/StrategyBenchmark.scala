package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractalAlgo
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy
import zio.clock.Clock
import zio.{App, ZIO, console}
import zio.console.Console

/**
  *
  * zio-cases - 2019-04-05
  * Created with ♥ in Amsterdam
  */
object StrategyBenchmark extends App {
  val env = new Console.Live with Clock.Live with Coloring.AColoring with FractalAlgo.MandelbrotAlgo {}
  val maxIter = 5000
  val maxSquaredModule = 8
  val frameWidth = 600
  val frameHeight = 800

  def benchmarkParallelPoints: ZIO[Console with Clock with Coloring with FractalAlgo, Nothing, List[(Int, Long)]] =
    ZIO.foreach(Stream.iterate(8)(_ * 2).takeWhile(_ <= 2 * frameHeight * frameWidth)) { par =>
      console.putStrLn(s"Benchmarking with strategy par = ${ComputationStrategy.ParallelPoints(par)}") *>
        fractal.calculate(ComputationStrategy.ParallelPoints(par))(maxIter, maxSquaredModule, frameWidth, frameHeight)
          .map { case (_, time) => (par, time) }
    }

  def benchmarkParallelSliced: ZIO[Console with Clock with Coloring with FractalAlgo, Nothing, List[(Int, Long)]] =
    ZIO.foreach(Stream.iterate(8)(_ * 2).takeWhile(_ <= 2 * frameHeight * frameWidth)) { par =>
      console.putStrLn(s"Benchmarking with strategy par = ${ComputationStrategy.ParallelPoints(par)}") *>
        fractal.calculate(ComputationStrategy.ParallelSliced(par))(maxIter, maxSquaredModule, frameWidth, frameHeight)
          .map { case (_, time) => (par, time) }
    }

  def benchmarkParallelRows: ZIO[Console with Clock with Coloring with FractalAlgo, Nothing, Long]=
      console.putStrLn(s"Benchmarking with strategy par = ${ComputationStrategy.ParallelRows}") *>
        fractal.calculate(ComputationStrategy.ParallelRows)(maxIter, maxSquaredModule, frameWidth, frameHeight)
          .map { case (_, time) => time }

  def benchmarkParallelPointsAllPar: ZIO[Console with Clock with Coloring with FractalAlgo, Nothing, Long]=
    console.putStrLn(s"Benchmarking with strategy par = ${ComputationStrategy.ParallelPointsAllPar}") *>
      fractal.calculate(ComputationStrategy.ParallelPointsAllPar)(maxIter, maxSquaredModule, frameWidth, frameHeight)
        .map { case (_, time) => time }



  override def run(args: List[String]): ZIO[StrategyBenchmark.Environment, Nothing, Int] =
    (
      console.putStrLn(" -- start strategies benchmark -- ")
      *> console.putStrLn(" -- 1. Parallel points -- ")
      *> benchmarkParallelPoints.flatMap(pts => console.putStrLn(s"Results [nr fibers - total time]: ${pts.mkString("\n")}"))
      *> console.putStrLn(" -- end Parallel points -- ")

      *> console.putStrLn(" -- 2. Parallel sliced -- ")
      *> benchmarkParallelSliced.flatMap(pts => console.putStrLn(s"Results [slice size - total time]: ${pts.mkString("\n")}"))
      *> console.putStrLn(" -- end Parallel sliced -- ")

      *> console.putStrLn(" -- 3. Parallel rows -- ")
      *> benchmarkParallelRows.flatMap(totalTime => console.putStrLn(s"Results [total time]: $totalTime"))
      *> console.putStrLn(" -- end Parallel rows -- ")

      *> console.putStrLn(" -- 3. Parallel rows -- ")
      *> benchmarkParallelPointsAllPar.flatMap(totalTime => console.putStrLn(s"Results [total time]: $totalTime"))
      *> console.putStrLn(" -- end Parallel rows -- ")

      *> console.putStrLn(" -- end benchmark -- ")
    ).provide(env).const(0) orElse ZIO.succeed(1)
}

