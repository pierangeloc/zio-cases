package io.tuliplogic

import io.tuliplogic.http.HttpServer
import io.tuliplogic.repository.UserRepository
import io.tuliplogic.repository.UserRepository.MemUserRepository
import scalaz.zio.{Ref, TaskR, ZIO}
import scalaz.zio.clock.Clock
import io.tuliplogic.http.UserService
import io.tuliplogic.model.user.{User, UserId}
import scalaz.zio.App
import scalaz.zio.console._

/**
 * 
 * zio-cases - 2019-06-06
 * Created with ♥ in Amsterdam
 */
object Main extends App {
  type AppEnvironment = Clock with UserRepository
  type AppTask[A] = TaskR[AppEnvironment, A]

  override def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] =
    (
      for {
      userRepoState <- Ref.make(Map[UserId, User]())
      userService = new UserService[AppEnvironment]
      httpServer = new HttpServer(userService)
      p <- httpServer.serve.provideSome[Environment] { baseEnv =>
          new Clock with MemUserRepository {
            override val state: Ref[Map[UserId, User]] = userRepoState
            override val clock: Clock.Service[Any] = baseEnv.clock
          }
        }
      } yield p
    ).foldM(err => putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))

}

