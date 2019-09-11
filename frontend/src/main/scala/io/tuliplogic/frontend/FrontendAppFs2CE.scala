package io.tuliplogic.frontend

import java.nio.ByteBuffer

import cats.implicits._
import cats.effect._
import fs2._
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.experimental.{Fetch, ReadableStreamReader}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.Event
import scalatags.JsDom.all._

import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.typedarray._
import scala.concurrent.duration._

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


  def buildDom(): IO[Canvas] = for {
    body <- IO.delay(document.body)
    c <- IO.delay(canvas(id := "canvas", widthA:= 640, heightA := 480, color := "blue").render)
    b <- IO.delay {
      val b = button("draw").render
      b.addEventListener(
        `type` = "click",
        listener = zioEventListener(c)
      )
      b
    }
    _ <-
      List(
        h1("ZIO fractals").render,
        c,
        b
      ).traverse(el => IO.delay(body.appendChild(el)))
    }
   yield c


  import io.circe.generic.auto._
  import io.circe.parser.decode

  def zioEventListener(c: Canvas): Event => Unit = e =>
    (greet() >> getStreamBytes
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .map(s => decode[ColoredPoint](s))
      .collect { case Right(cp) => cp }
//      .take(100)
//      .metered(100.millis)
      .evalMap(cp => paintOnCanvas(cp, c))
      .compile.drain)
      .unsafeRunAsyncAndForget()
  //TODO: interrupt when the stream is finished

  def greet() = putStrLn("I'll ask for bytes and will start processing!")

  def paintOnCanvas(cp: ColoredPoint, canvas: Canvas): IO[Unit] = IO {
    type Ctx2D =
      dom.CanvasRenderingContext2D
    val ctx = canvas.getContext("2d").asInstanceOf[Ctx2D]
    val fillStyle = s"rgb(${cp.color.red.toInt}, ${cp.color.green.toInt}, ${cp.color.blue.toInt})"
//    println(s"fillStyle: $fillStyle")
//    println(s"ctx.fillRect(${cp.pixel.x}, ${cp.pixel.y}, 0.5, 0.5)")
    ctx.fillStyle = fillStyle
    ctx.fillRect(cp.pixel.x, cp.pixel.y, 1, 1)
  }

  def getStreamBytes: Stream[IO, Byte] =
    for {
      reader <- Stream.eval(
          promiseToTask(Fetch.fetch(plotFractalsUrl)).map(_.body.getReader)
        )
      bb <- processReader(reader)
      b  <- Stream.emits(bb.array())
    } yield b

  def processReader(reader: ReadableStreamReader[Uint8Array]): Stream[IO, ByteBuffer] =
    Stream.eval(promiseToTask(reader.read())).repeat
      .takeWhile(_.done == false)
      .map { chunk =>
        ByteBuffer.wrap(new Int8Array(chunk.value.buffer).toArray)
      }

  def promiseToTask[A](p: => Promise[A]): IO[A] = IO.fromFuture(IO(p.toFuture))

}
