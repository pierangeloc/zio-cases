package io.tuliplogic.fractals

import java.io.IOException

import io.tuliplogic.fractals.algo.FractalAlgo.MandelbrotAlgo
import io.tuliplogic.fractals.canvas.ZCanvas
import io.tuliplogic.fractals.coloring.Coloring.AColoring
import io.tuliplogic.fractals.fractal.ComputationStrategy
import zio.{App, Queue, ZIO}
import zio.console
import zio.console.{Console, putStr}

object ConsoleMain extends App {

  val maxIterations = 5000
  val divergenceThreshold = 8
  val complexRectangle = ComplexRectangle(-2, 1, -1, 1, Frame(640, 480))

  def consumeFromQueue(queue: Queue[ColoredPoint]): ZIO[Console, IOException, Unit] = for {
    _ <- console.getStrLn
    pts <- queue.takeUpTo(10)
    _ <- console.putStrLn(pts.mkString(",\n"))
    _ <- console.putStrLn("press enter to continue consuming...")
  } yield ()

  def calculateAndPutOnQueue(queue: Queue[ColoredPoint]): ZIO[Any, Nothing, Unit] =
    Compute.program(ComputationStrategy.ParallelRows, maxIterations, divergenceThreshold)(complexRectangle)
      .provideSomeM {
        ZCanvas.queueCanvas.map { queueCanvas =>
          new ZCanvas with AColoring with MandelbrotAlgo {
            override def canvas: ZCanvas.Service[Any] = queueCanvas.canvas
          }
        }
      }.provide(queue).unit

  val calculateAndPublishOnQueue: ZIO[Console, IOException, Unit] = for {
    queue   <- Queue.unbounded[ColoredPoint]
    _       <- console.putStrLn("Let's start this computation, press a key")
    _       <- console.getStrLn
    calculationFiber <- calculateAndPutOnQueue(queue).fork
    _       <- console.putStrLn("Press enter to consume...")

    consumptionFiber <- consumeFromQueue(queue).forever

  } yield ()

  override def run(args: List[String]): ZIO[ConsoleMain.Environment, Nothing, Int] =
    calculateAndPublishOnQueue.foldM(err => putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
}
