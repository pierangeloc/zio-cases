package io.tuliplogic.fractals

import cats.effect.Blocker
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import zio.console.putStr
import zio.{App, Task, UIO, ZIO, blocking}
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.syntax.all._
import scalatags.Text.all.Modifier

import scala.concurrent.ExecutionContext


object HttpMain extends App {
  val rootPath = "zio-mandelbrot"

  override def run(args: List[String]): ZIO[HttpMain.Environment, Nothing, Int] =
    for {
      blocker <- blocking.blockingExecutor.map(ex => Blocker.liftExecutionContext(ex.asEC))
      res <- ZIO.runtime[Environment].flatMap { implicit rts =>
        BlazeServerBuilder[Task]
          .bindHttp(8080, "localhost")
          .withHttpApp(
            Router[Task]( s"/$rootPath" -> Html.service(blocker)).orNotFound
          )
          .serve
          .compile
          .drain
          .foldM(err => putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
      }
    } yield res
}


object Html {

  val jsScript = s"${HttpMain.rootPath}/assets/frontend-fastopt.js"
  val jsScripts: Seq[Modifier] = {
    import scalatags.Text.all._
    List(
      script(src := jsScript)
    )
  }

  import scalatags.Text.all._
  import org.http4s._
  import org.http4s.headers._

  def home: UIO[String] = UIO.succeed(
    html(
      head(
        link(rel:="shortcut icon", media:="image/png", href:="/assets/images/favicon.png")
      ),
      body(
//        h1("ZIO fractals"),
//        canvas(id := "canvas", width:= 640, height := 480, color := "blue"),
//        button("draw", onclick := "FrontendApp.greet()"),
        jsScripts
      )
    ).render
  )

  def service(blocker: Blocker) = {
    object dsl extends Http4sDsl[Task]
    import dsl._

    HttpRoutes.of[Task] {
      case GET -> Root => Ok(home).map(
          _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)
          )
        )
      case GET -> "assets" /: staticResource =>
        StaticFile.fromResource[Task](staticResource.toString, blocker, None).getOrElseF(NotFound())
    }

  }

}