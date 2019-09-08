package io.tuliplogic.frontend

import java.nio.ByteBuffer

import org.scalajs.dom.document
import org.scalajs.dom.experimental.{Fetch, ReadableStreamReader}
import org.scalajs.dom.raw.Event
import scalatags.JsDom.all._
import cats._
import implicits._
import cats.effect._
import fs2.Stream.Compiler
import fs2._
import zio._
import zio.console.Console
import zio.interop.catz._
import zio.{App, Runtime, Task, UIO, ZIO, console}

import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.typedarray._

/**
 * FrontendApp entry point
 */
@JSExportTopLevel("FrontendApp")
object FrontendAppFs2 extends App {

  implicitly[Sync[Task]]

  sealed trait Error extends Throwable
  object Error {
    case class Generic(msg: String) extends Error
  }

  val plotFractalsUrl = "http://localhost:8080/zio-mandelbrot/plot-fractals"
  type C[A] = RIO[Console, A]

  override def run(args: List[String]): ZIO[FrontendApp.Environment, Nothing, Int] =
    (
      buildDom() *>
      console.putStrLn("Eccoci!")
    ).foldM(err => console.putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))


  def buildDom(): ZIO[Environment, Throwable, Unit] = for {
    rts  <- ZIO.runtime[Environment]
    env  <- ZIO.environment[Environment]
    body <- Task.effectTotal(document.body)
    canvas <- ZIO.foreach(
      List(
        h1("ZIO fractals").render,
        canvas(id := "canvas", width:= 640, height := 480, color := "blue").render,
        {
          val b = button("draw").render
          b.addEventListener(
            `type` = "click",
            listener = (e: Event) => rts.unsafeRun(
              (greet() *> getFractalsData().run(zio.stream.Sink.drain)
//                foreach {
//                bb => console.putStrLn(new String(bb.array(), "UTF-8"))
//              }
                ).provide(env)
            )
          )

          b
        },
      ))(el => Task.effect(body.appendChild(el)))
    }
   yield ()

  def zioEventListener(rts: Runtime[Environment], env: Environment): Event => Unit = e => rts.unsafeRun(
    greet().provide(env) *> getFractalsData(env).compile.drain
  )

  def greet(): ZIO[Console, Throwable, Unit] = {
    console.putStrLn("Eccoci!")
  }


  def getFractalsData(c: Console): Stream[Task, ByteBuffer] =
    for {
      reader <- Stream.eval(
          promiseToTask(Fetch.fetch(plotFractalsUrl)).map(_.body.getReader)
        )
      bb <- processReader(reader)(c).take(50)
    } yield bb

  def processReader(reader: ReadableStreamReader[Uint8Array])(c: Console): Stream[Task, ByteBuffer] =
    Stream.eval(promiseToTask(reader.read())).repeat
      .evalMap{ chunk =>
        (console.putStrLn(s"chunk done: ${chunk.done}") *> UIO.succeed(chunk)).provide(c)
      }
      .takeWhile(_.done == false)
      .evalMap { chunk =>
        (
          console.putStrLn(s"chunk done: ${chunk.done}") *> ZIO.succeed(
            ByteBuffer.wrap(new Int8Array(chunk.value.buffer).toArray)
        )
          ).provide(c)
      }

  def promiseToTask[A](p: Promise[A]): Task[A] = Task.fromFuture(implicit ec => p.toFuture)

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
