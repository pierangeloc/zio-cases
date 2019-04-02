package io.tuliplogic.fractals
import scalaz.zio.ZIO

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
package object algo extends FractAlgo.Service[FractAlgo] {
  override def iterate(c: Complex, bailout: Int, maxIterations: Int): ZIO[FractAlgo, Nothing, Int] =
    ZIO.accessM(_.service.iterate(c, bailout, maxIterations))
}
