package io.tuliplogic.http

import io.tuliplogic.repository.UserRepository
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import scalaz.zio.clock.Clock
import scalaz.zio.interop.catz._
import scalaz.zio.{TaskR, ZIO}

class HttpServer[R <: Clock with UserRepository](routes: UserService[R]) {
  private val basePath = "/zio-cases-http4s"

  type AppTask[A] = TaskR[R, A]

  val router: HttpRoutes[AppTask] = Router[AppTask](basePath -> routes.httpRoutes)

  def httpApp: HttpApp[AppTask] = router.orNotFound

  val serve: ZIO[R, Throwable, Unit] = ZIO.runtime[R]
    .flatMap { implicit rts =>
      BlazeServerBuilder[AppTask]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
        .compile
        .drain
    }

}
