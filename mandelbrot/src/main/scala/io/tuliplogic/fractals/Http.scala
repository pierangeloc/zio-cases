package io.tuliplogic.fractals

import cats.Functor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.{App, Task, ZIO}
import zio.interop.catz._


object Http extends App {

  override def run(args: List[String]): ZIO[Http.Environment, Nothing, Int] = ???
}


object Html {

  import scalatags.Text.all._
  import org.http4s._
  import org.http4s.headers._
  import org.http4s.syntax.all._
  import cats.implicits._



  val home = html(
    head(
      link(rel:="shortcut icon", media:="image/png", href:="/assets/images/favicon.png")
    ),
    body(
      h1("ZIO fractals")
    )
  )

  def service = {
    object dsl extends Http4sDsl[Task]
    import dsl._

    implicitly[Functor[Task]]
    HttpRoutes.of[Task] {
      case GET -> Root =>
      Ok(home.render).map(
        _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)
        )
      )
    }.orNotFound
  }

}