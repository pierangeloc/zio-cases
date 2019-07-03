package io.tuliplogic.fractals
import zio.ZIO

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
package object algo extends FractalAlgo.Service[FractalAlgo] {
  override def iterations(c: Complex, divergeThreshold: Int, maxIterations: Int): ZIO[FractalAlgo, Nothing, Int] =
    ZIO.accessM(_.service.iterations(c, divergeThreshold, maxIterations))
}
