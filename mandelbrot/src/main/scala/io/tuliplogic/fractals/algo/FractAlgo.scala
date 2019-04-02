package io.tuliplogic.fractals.algo

import io.tuliplogic.fractals.Complex
import scalaz.zio.ZIO

import scala.annotation.tailrec

/**
  *
  * zio-cases - 2019-04-03
  * Created with ♥ in Amsterdam
  */
trait FractAlgo {
  def service: FractAlgo.Service[Any]
}

object FractAlgo {

  trait Service[R] {
    def iterate(c: Complex, bailout: Int, maxIterations: Int): ZIO[R, Nothing, Int]
  }

  trait MandelbrotAlgo extends Service[Any] {
    def iterate(c: Complex, bailout: Int, maxIterations: Int): ZIO[Any, Nothing, Int] = {

      @tailrec
      def run(z: Complex, iter: Int): Int =
        if (iter >= maxIterations ||  z.squaredAbs > bailout)
          iter
        else
          run(Complex(1, 0) * z * z + c, iter + 1)

      ZIO.effectTotal(run(Complex.zero, 0))
    }
  }

  trait JuliaAlgo extends Service[Any] {
    def iterate(c: Complex, bailout: Int, maxIterations: Int): ZIO[Any, Nothing, Int] = ???
  }
}
