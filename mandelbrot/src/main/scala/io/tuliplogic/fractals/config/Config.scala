package io.tuliplogic.fractals.config

import io.tuliplogic.fractals.{ComplexRectangle, Frame}
import zio.{UIO, ZIO}

trait Config {
  def configService: Config.Service[Any]
}

object Config {
  trait Service[R] {
    def maxIterations: ZIO[R, Nothing, Int]
    def divergenceThreshold: ZIO[R, Nothing, Int]
    def complexRectangle: ZIO[R, Nothing, ComplexRectangle]

    def computationStrategy: ZIO[R, Nothing, ComputationStrategy]
  }

  trait StdConfig extends Config {
    def configService: Service[Any] = new Service[Any] {
      def maxIterations: UIO[Int] = UIO.succeed(5000)
      def divergenceThreshold: UIO[Int] = UIO.succeed(8)
      def complexRectangle: UIO[ComplexRectangle] = UIO.succeed(ComplexRectangle(-2, 1, -1, 1, Frame(640, 480)))
      def computationStrategy: ZIO[Any, Nothing, ComputationStrategy] = UIO.succeed(ComputationStrategy.ParallelRows)
    }
  }

  object StdConfig extends StdConfig
}

sealed trait ComputationStrategy

object ComputationStrategy {

  case class ParallelPoints(parallelism: Int) extends ComputationStrategy

  case object ParallelPointsAllPar extends ComputationStrategy

  case object ParallelRows extends ComputationStrategy

  case class ParallelSliced(sliceSize: Int) extends ComputationStrategy

}