package io.tuliplogic

import cats.effect.{ConcurrentEffect, Timer}
import io.tuliplogic.http.UserRoutes
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

class HttpServer[F[_]: ConcurrentEffect: Timer](routes: UserRoutes[F]) {

  def httpApp: HttpApp[F] = routes.routes.orNotFound

  val serve: F[Unit] =
    BlazeServerBuilder[F]
    .bindHttp(8080, "0.0.0.0")
    .withHttpApp(httpApp)
    .serve
    .compile
    .drain

}
