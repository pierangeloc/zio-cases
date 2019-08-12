package io.tuliplogic.fractals

import zio.ZIO

package object config extends Config.Service[Config] {
  override def maxIterations: ZIO[Config, Nothing, Int] = ZIO.accessM(_.configService.maxIterations)
  override def divergenceThreshold: ZIO[Config, Nothing, Int] = ZIO.accessM(_.configService.divergenceThreshold)
  override def complexRectangle: ZIO[Config, Nothing, ComplexRectangle] = ZIO.accessM(_.configService.complexRectangle)
  override def computationStrategy: ZIO[Config, Nothing, ComputationStrategy] = ZIO.accessM(_.configService.computationStrategy)
}