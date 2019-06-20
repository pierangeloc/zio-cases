package io.tuliplogic.fractals
import zio.ZIO

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
package object algo extends FractAlgo.Service[FractAlgo] {
  override def iterations(c: Complex, bailout: Int, maxIterations: Int): ZIO[FractAlgo, Nothing, Int] =
    ZIO.accessM(_.service.iterations(c, bailout, maxIterations))
}
