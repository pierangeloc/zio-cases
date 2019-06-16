package io.tuliplogic.http

import cats.effect.Sync
import cats.syntax.all._
import io.tuliplogic.model.user.{User, UserId}
import io.tuliplogic.repository.UserRepository
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import org.http4s.server.Router


/**
 * 
 * zio-cases - 2019-06-14
 * Created with ♥ in Amsterdam
 */
class UserRoutes[F[_]: Sync](userRepository: UserRepository[F]) extends Http4sDsl[F] {

  private val basePath = "/zio-cases-http4s"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" / userId =>
      Ok(userRepository.find(UserId(userId)))

    case req @ POST -> Root / "users" =>
      req.decode[User] { user =>
        Ok(userRepository.create(user))
      }

    case DELETE -> Root / "users" / userId =>
      userRepository.delete(UserId(userId))
        .flatMap(_ => NoContent())

    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }

  val routes: HttpRoutes[F] = Router(basePath -> httpRoutes)

}
