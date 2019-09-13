package io.tuliplogic.frontend

import java.nio.ByteBuffer

import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import fs2._
import org.scalajs.dom
import org.scalajs.dom.{ImageData, document}
import org.scalajs.dom.experimental.{Fetch, ReadableStreamReader}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.Event
import scalatags.JsDom.all._

import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.typedarray._
import scala.concurrent.duration._
import scala.util.Random

/**
 * FrontendApp entry point
 */
@JSExportTopLevel("FrontendAppFs2CE")
object FrontendAppFs2CE extends IOApp {
  type Ctx2D = dom.CanvasRenderingContext2D
  type Reff[A] = Ref[IO, A]

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
    fractalButton <- IO.delay {
      val b = button("fractal").render
      b.addEventListener(
        `type` = "click",
        listener = showFractals(c)
      )
      b
    }
    noiseButton <- IO.delay {
      val b = button("noise").render
      b.addEventListener(
        `type` = "click",
        listener = showNoise(c)
      )
      b
    }
    _ <-
      List(
        h1("ZIO fractals").render,
        c,
        fractalButton,
        noiseButton
      ).traverse(el => IO.delay(body.appendChild(el)))
    }
   yield c

  import io.circe.generic.auto._
  import io.circe.parser.decode

  def showFractals(c: Canvas): Event => Unit = e =>
    (putStrLn("computing and displaying fractals") >> getStreamBytes
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

  def canvas2dCtx(c: Canvas): Ctx2D = c.getContext("2d").asInstanceOf[Ctx2D]

  def showNoise(c: Canvas): Event => Unit = e => {
    val pixels = for {
      rows <- Stream.range(0, c.height)
      cols <- Stream.range(0, c.width)
    } yield Pixel(rows, cols)

    def randomCp(pixel: Pixel): IO[ColoredPoint] = for {
      rr <- IO(Random.nextInt(256))
      rg <- IO(Random.nextInt(256))
      rb <- IO(Random.nextInt(256))
//      _  <- putStrLn(s"computed rgb for pixel $pixel")
    } yield ColoredPoint(pixel, Color(rr, rg, rb))

    def putOnCanvas(c: Canvas, imageData: Reff[ImageData])(tFrame: Double): Unit = {
      dom.window.requestAnimationFrame(putOnCanvas(c, imageData))
      imageData.get.map(id => canvas2dCtx(c).putImageData(id, 0, 0)).unsafeRunAsyncAndForget()
    }

    (
      for {
        imageData <- Ref.of[IO, ImageData](canvas2dCtx(c).getImageData(0, 0, c.width, c.height))
        _         <- pixels.covary[IO]
                       .evalMap { p =>
                         for {
                           cp <- randomCp(p)
                           _  <- paintOnImageData(cp, imageData)
                           _  <- IO(putOnCanvas(c, imageData)(0))
                         } yield ()
                       }.compile.drain
      } yield ()
    ).unsafeRunAsyncAndForget()

  }


  def paintOnCanvas(cp: ColoredPoint, canvas: Canvas): IO[Unit] =
//    (if (cp.pixel.x % 100 == 0 && cp.pixel.y % 100 == 0) putStrLn(s"painting $cp") else IO(())) >>
    IO {

    val ctx = canvas.getContext("2d").asInstanceOf[Ctx2D]
    val fillStyle = s"rgb(${cp.color.red.toInt}, ${cp.color.green.toInt}, ${cp.color.blue.toInt})"
    ctx.fillStyle = fillStyle
    ctx.fillRect(cp.pixel.x, cp.pixel.y, 1, 1)
  }

  def paintOnImageData(cp: ColoredPoint, imageData: Reff[ImageData]): IO[Unit] =
    imageData.update { id => {
        val pixelIndex: Int = (cp.pixel.y * id.width + cp.pixel.x) * 4
        id.data(pixelIndex) = cp.color.red.toInt
        id.data(pixelIndex + 1) = cp.color.green.toInt
        id.data(pixelIndex + 2) = cp.color.blue.toInt
        id.data(pixelIndex + 3) = 255
        id
      }
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
