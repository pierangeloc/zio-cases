package io.tuliplogic.http

import io.tuliplogic.model.user.{User, UserId}
import io.tuliplogic.repository.{UserRepository, userRepo}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import zio._
import zio.interop.catz._

/**
 * 
 * zio-cases - 2019-06-14
 * Created with ♥ in Amsterdam
 */
class UserService[R <: UserRepository] {

  type F[A] = TaskR[R, A]

  private val http4sDsl = new Http4sDsl[F]{}
  import http4sDsl._

  val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" / userId =>
      Ok(userRepo.find(UserId(userId)))

    case req@POST -> Root / "users" =>
      req.decode[User] { user =>
        Ok(userRepo.create(user))
      }

    case DELETE -> Root / "users" / userId =>
      userRepo.delete(UserId(userId))
        .flatMap(_ => NoContent())

    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }

}
