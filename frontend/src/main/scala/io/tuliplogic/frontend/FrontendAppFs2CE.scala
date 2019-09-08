package io.tuliplogic.frontend

import java.nio.ByteBuffer

import cats.implicits._
import cats.effect._
import fs2._
import org.scalajs.dom.document
import org.scalajs.dom.experimental.{Fetch, ReadableStreamReader}
import org.scalajs.dom.raw.Event
import scalatags.JsDom.all._

import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.typedarray._

/**
 * FrontendApp entry point
 */
@JSExportTopLevel("FrontendAppFs2CE")
object FrontendAppFs2CE extends IOApp {

  sealed trait Error extends Throwable
  object Error {
    case class Generic(msg: String) extends Error
  }

  val plotFractalsUrl = "http://localhost:8080/zio-mandelbrot/plot-fractals"
  def putStrLn(s: String): IO[Unit] = IO.delay(println(s))

  override def run(args: List[String]): IO[ExitCode] =
    (
      buildDom() *>
      putStrLn("Eccoci!")
    ).redeem(_ => ExitCode.Error, _ => ExitCode.Success)


  def buildDom(): IO[Unit] = for {
    body <- IO.delay(document.body)
    canvas <-
      List(
        h1("ZIO fractals").render,
        canvas(id := "canvas", width:= 640, height := 480, color := "blue").render,
        {
          val b = button("draw").render
          b.addEventListener(
            `type` = "click",
            listener = zioEventListener
          )
          b
        },
      ).traverse(el => IO.delay(body.appendChild(el)))
    }
   yield ()

  def zioEventListener: Event => Unit = e =>
    (greet() >> getFractalsData.compile.drain).unsafeRunAsyncAndForget()

  def greet() = putStrLn("Eccoci!")

  def getFractalsData: Stream[IO, ByteBuffer] =
    for {
      reader <- Stream.eval(
          promiseToTask(Fetch.fetch(plotFractalsUrl)).map(_.body.getReader)
        )
      bb <- processReader(reader).take(50)
    } yield bb

  def processReader(reader: ReadableStreamReader[Uint8Array]): Stream[IO, ByteBuffer] =
    Stream.eval(promiseToTask(reader.read())).repeat
      .evalMap{ chunk =>
        (putStrLn(s"chunk done: ${chunk.done}") >> IO.pure(chunk))
      }
      .takeWhile(_.done == false)
      .evalMap { chunk =>
        putStrLn(s"chunk done: ${chunk.done}") >> IO.pure(
            ByteBuffer.wrap(new Int8Array(chunk.value.buffer).toArray)
        )
      }

  def promiseToTask[A](p: Promise[A]): IO[A] = IO.fromFuture(IO(p.toFuture))

//  def getFractalsData() = {
//    import com.softwaremill.sttp._
//    import com.softwaremill.sttp.impl.monix.FetchMonixBackend
//    import monix.execution.Scheduler.Implicits.global
//
//    implicit val sttpBackend = FetchMonixBackend()
//
//    val response: eval.Task[Response[Observable[ByteBuffer]]] = for {
//      resp <- sttp
//        .get(uri"$plotFractalsUrl")
//        .response(asStream[Observable[ByteBuffer]])
//        .send()
//      observable <- MonixTask.fromEither(resp.body.leftMap(Error.Generic).map(_.toReactivePublisher))
//    } yield ???




}
