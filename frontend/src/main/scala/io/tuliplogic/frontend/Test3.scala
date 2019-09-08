

object ZioFs2Playground extends App {
  import cats._
  import implicits._
  import cats.effect._
  import fs2.Stream.Compiler
  import fs2._
  import zio._
  import zio.interop.catz._

  implicitly[Sync[Task]]
  implicitly[Compiler[Task, Task]]
  override def run(
    args: List[String]): ZIO[ZioFs2Playground.Environment, Nothing, Int] =
    for {
      env <- ZIO.environment[Environment]
        out <- print3(env).compile.drain
    } yield 0

  def print3(env: Environment): Stream[Task, Unit] =
    Stream(1, 2, 3)
      .evalMap { i =>
        console
          .putStrLn(i.toString)
          .mapError(_.asInstanceOf[Throwable])
          .provide(env)
      }
}

object T2 {
  import zio._
  import cats.effect._
  import zio.interop.catz._

  implicitly[Sync[Task]]
}
