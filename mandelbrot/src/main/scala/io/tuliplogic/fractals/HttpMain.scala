package io.tuliplogic.fractals

import fs2.Stream
import io.tuliplogic.fractals.algo.FractalAlgo.MandelbrotAlgo
import io.tuliplogic.fractals.canvas.ZCanvas
import io.tuliplogic.fractals.coloring.Coloring.AColoring
import io.tuliplogic.fractals.config.Config.StdConfig
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import zio.console.putStr
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import cats.effect.{Blocker, ConcurrentEffect, Timer}
import org.http4s.syntax.all._
import org.http4s.server.middleware.PushSupport._
import scalatags.Text.all.Modifier
import fs2.concurrent.{Queue => Fs2Q}

object HttpMain extends CatsApp {
  val rootPath = "zio-mandelbrot"

  //TODO: fix this implicits resolution issue, they can be found here but not when building BlazeServerBuilder. For now we pass them explicitly
  val ce = implicitly[ConcurrentEffect[Task]]
  val tr = implicitly[Timer[Task]]

  override def run(args: List[String]): ZIO[HttpMain.Environment, Nothing, Int] = {

    for {
      blocker <- blocking.blockingExecutor.map(ex => Blocker.liftExecutionContext(ex.asEC))
      res     <- ZIO.runtime[Environment].flatMap { implicit rts =>
                BlazeServerBuilder[zio.Task](ce, tr)
                  .bindHttp(8080, "localhost")
                  .withHttpApp(
                    Router[Task](s"/$rootPath" -> Html.service(blocker)).orNotFound
                  )
                  .serve
                  .compile
                  .drain
                  .foldM(err => putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
              }
    } yield res
  }
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
        link(rel:="shortcut icon", media:="image/png", href:="/assets/images/favicon.png"),
        link(
          rel:="stylesheet",
          href:="https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css",
          attr("integrity"):="sha384-HSMxcRTRxnN+Bdg0JdbxYKrThecOKuH5zCYotlSAcp1+c8xmyTe9GYg1l9a69psu",
          attr("crossorigin"):="anonymous")
      ),
      body(
        jsScripts
      )
    ).render
  )

  def service(blocker: Blocker)(implicit timer: Timer[Task]): HttpRoutes[Task] = {
    object dsl extends Http4sDsl[Task]
    import dsl._

    HttpRoutes.of[Task] {
      case GET -> Root => Ok(home).map(
          _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)
          )
        )

      case GET -> "assets" /: staticResource =>
        StaticFile.fromResource[Task](staticResource.toString, blocker, None).getOrElseF(NotFound())

      case req @ GET -> Root / "push" =>
        // http4s intends to be a forward looking library made with http2.0 in mind
        val data = """<html><body><img src="image.jpg"/></body></html>"""
        Ok(data)
          .map(_.withContentType(`Content-Type`(MediaType.text.`html`)))
          .map(_.push("/image.jpg")(req))

      case req @ GET -> Root / "plot-fractals" =>
        Ok(computeFs2Stream.intersperse("\n"))
    }

  }

  type Q = Fs2Q[Task, ColoredPoint]

  def calculateAndPutOnQueue(queue: Q): ZIO[Any, Nothing, Unit] =
    Compute.program.provideSomeM {
      ZCanvas.fs2QueueCanvas.map { queueCanvas =>
        new ZCanvas with AColoring with MandelbrotAlgo with StdConfig {
          override def canvas: ZCanvas.Service[Any] = queueCanvas.canvas
        }
      }
    }.provide(queue).unit

  import scala.concurrent.duration._
  import io.circe.generic.auto._
  import io.circe.syntax._

  def computeFs2Stream(implicit timer: Timer[Task]): Stream[Task, String] = for {
    q  <- Stream.eval(Fs2Q.unbounded[Task, ColoredPoint])
//    q  <- Stream.eval(Fs2Q.bounded[Task, ColoredPoint](1000))
    _  <- Stream.eval(calculateAndPutOnQueue(q).fork)
    cp <- q.dequeueChunk(100)
//      .metered(1.seconds)
//        .take(1000)
//      .evalTap(chunk => Task.effect(println(s"emitting chunk {$chunk}"))) //slow chunks
  } yield cp.asJson.noSpaces

}