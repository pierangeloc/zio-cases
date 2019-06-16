package io.tuliplogic

import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import io.tuliplogic.http.UserRoutes
import io.tuliplogic.model.user.{User, UserId}
import io.tuliplogic.repository.MemUserRepository
import cats.implicits._

/**
 * 
 * zio-cases - 2019-06-06
 * Created with ♥ in Amsterdam
 */
object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      repoState    <- Ref[IO].of(Map[UserId, User]())
      memoUserRepo <- new MemUserRepository[IO](repoState).pure[IO]
      httpServer   <- new HttpServer[IO](new UserRoutes[IO](memoUserRepo)).pure[IO]
      _            <- httpServer.serve
    } yield ExitCode.Success
}
