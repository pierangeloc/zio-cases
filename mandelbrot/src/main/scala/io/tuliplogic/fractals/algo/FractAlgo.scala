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
    def iterations(c: Complex, bailout: Int, maxIterations: Int): ZIO[R, Nothing, Int]
  }

  trait MandelbrotAlgo extends FractAlgo {
    def service: Service[Any] = new Service[Any] {
      def iterations(c: Complex, bailout: Int, maxIterations: Int): ZIO[Any, Nothing, Int] = {

        @tailrec
        def run(z: Complex, iter: Int): Int =
          if (iter >= maxIterations ||  z.squaredAbs > bailout)
            iter
          else
            run(z * z + c, iter + 1)


        val now = System.nanoTime()
        val res = run(Complex.zero, 0)
        ZIO.effectTotal(res)
      }
    }

  }

  trait JuliaAlgo extends FractAlgo {
    def service: Service[Any] = new Service[Any] {
      def iterations(c: Complex, bailout: Int, maxIterations: Int): ZIO[Any, Nothing, Int] = ???
    }
  }
}
