package io.tuliplogic.fractals

import io.tuliplogic.fractals.fractal.ComputationStrategy
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

object config extends Config.Service[Config] {
  override def maxIterations: ZIO[Config, Nothing, Int] = ZIO.accessM(_.configService.maxIterations)
  override def divergenceThreshold: ZIO[Config, Nothing, Int] = ZIO.accessM(_.configService.divergenceThreshold)
  override def complexRectangle: ZIO[Config, Nothing, ComplexRectangle] = ZIO.accessM(_.configService.complexRectangle)
  override def computationStrategy: ZIO[Config, Nothing, ComputationStrategy] = ZIO.accessM(_.configService.computationStrategy)
}
