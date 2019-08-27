package io.tuliplogic.frontend


import java.nio.ByteBuffer

import zio.{App, IO, Task, UIO, ZIO, console}
import cats.syntax.either._

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import dom.document
import org.scalajs.dom.experimental.{Fetch, ReadableStream, ReadableStreamReader}

import scala.scalajs.js.typedarray._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.Event

import scala.scalajs.js.{JSConverters, Promise}
import scalatags.JsDom._
import scalatags.JsDom.all._
import zio.console.{Console, putStr}
import zio.stream.{Stream, ZStream}

/**
 * FrontendApp entry point
 */
@JSExportTopLevel("FrontendApp")
object FrontendApp extends App {

  sealed trait Error extends Throwable
  object Error {
    case class Generic(msg: String) extends Error
  }

  val plotFractalsUrl = "http://localhost:8080/zio-mandelbrot/plot-fractals"

  override def run(args: List[String]): ZIO[FrontendApp.Environment, Nothing, Unit] =
    (
      buildDom() *>
      console.putStrLn("Eccoci!")
    ).foldM(err => putStr(s"Error running application $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))


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
              (greet() *> getFractalsData().foreach {
                bb => console.putStrLn(new String(bb.array(), "UTF-8"))
              }).provide(env)
            )
          )

          b
        },
      ))(el => Task.effect(body.appendChild(el)))
    }
   yield ()

  def greet(): ZIO[Console, Nothing, Unit] = {
    console.putStrLn("Eccoci!")
  }


  def getFractalsData(): ZStream[Console, Throwable, ByteBuffer] =
    for {
      reader <- Stream.fromEffect(
          Task.fromFuture(implicit ec => Fetch.fetch(plotFractalsUrl).toFuture).map(_.body.getReader)
        )
      bb <- processReader(reader).take(50)
    } yield bb

  def processReader(reader: ReadableStreamReader[Uint8Array]): ZStream[Console, Throwable, ByteBuffer] = {
    Stream.fromEffect(promiseToTask(reader.read())).forever.takeWhile(_.done == false)
      .mapM { chunk => console.putStrLn(s"chunk done: ${chunk.done}") *> ZIO.succeed(
        ByteBuffer.wrap(new Int8Array(chunk.value.buffer).toArray)
        )
      }
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
