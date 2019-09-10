package io.tuliplogic.frontend

import zio.interop.catz._
import zio._
import cats.effect._

/**
 *
 * zio-cases - 09/09/2019
 * Created with ♥ in Amsterdam
 */
object T2 extends CatsApp {

  implicitly[Sync[Task]]
  implicitly[ConcurrentEffect[Task]]

  override def run(args: List[String]): ZIO[T2.Environment, Nothing, Int] = UIO.succeed(0)
}