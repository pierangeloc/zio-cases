package io.tuliplogic.fractals

import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import zio.console.putStr
import zio.{App, Task, ZIO}
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.syntax.all._


object HttpMain extends App {

  override def run(args: List[String]): ZIO[HttpMain.Environment, Nothing, Int] =
    ZIO.runtime[Any].flatMap { implicit rts =>
      BlazeServerBuilder[Task]
        .bindHttp(8080, "localhost")
        .withHttpApp(
          Router[Task]( "/zio-mandelbrot" -> Html.service).orNotFound
        )
        .serve
        .compile
        .drain
        .foldM(err => putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
    }
}


object Html {

  import scalatags.Text.all._
  import org.http4s._
  import org.http4s.headers._

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

    HttpRoutes.of[Task] {
      case GET -> Root =>
      Ok(home.render).map(
        _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)
        )
      )
    }

  }

}