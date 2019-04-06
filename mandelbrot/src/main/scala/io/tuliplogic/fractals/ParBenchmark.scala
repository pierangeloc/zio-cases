package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.coloring.Coloring
import scalaz.zio.clock.Clock
import scalaz.zio.{App, ZIO, console}
import scalaz.zio.console.Console

/**
  *
  * zio-cases - 2019-04-05
  * Created with ♥ in Amsterdam
  */
object ParBenchmark extends App {
  val env = new Console.Live with Clock.Live with Coloring.AColoring with FractAlgo.MandelbrotAlgo {}

  def benchmark: ZIO[Console with Clock with Coloring with FractAlgo, Nothing, List[(Long, Long)]] = for {
    pars <- ZIO.succeed(Stream.iterate(8L)(_ * 2).takeWhile(_ <= 2 * 240000L))
    parsResults <- ZIO.foreach(pars) { par =>
      console.putStrLn(s"Benchmarking with par = $par") *>
        fractal.calculationProgramParallelPoints(5000, 8, 600, 400, Some(par)).map { case (_, time) => (par, time) }
    }
  } yield parsResults

  override def run(args: List[String]): ZIO[ParBenchmark.Environment, Nothing, Int] =
    (console.putStrLn(" -- start benchmark -- ")
      *> benchmark
      *> console.putStrLn(" -- end benchmark -- ")).provide(env).const(0) orElse ZIO.succeed(1)
}

