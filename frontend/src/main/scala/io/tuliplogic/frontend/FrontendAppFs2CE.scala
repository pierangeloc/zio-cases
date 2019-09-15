package io.tuliplogic.frontend

import java.nio.ByteBuffer

import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import fs2._
import io.tuliplogic.frontend.FrontendAppFs2CE.{globalImageData, paintOnImageData}
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

  def canvas2dCtx(c: Canvas): Ctx2D = c.getContext("2d").asInstanceOf[Ctx2D]

  var globalImageData: ImageData = null;
  def putImageDataOnCanvas(c: Canvas)(tFrame: Double): Unit = {
    dom.window.requestAnimationFrame(putImageDataOnCanvas(c))
//    println(s"putting imageData on canvas at t = $tFrame")
    if (globalImageData != null) canvas2dCtx(c).putImageData(globalImageData, 0, 0) else ()
  }


  override def run(args: List[String]): IO[ExitCode] =
    (
      for {
        c         <- buildDom()
        imageData <- IO(canvas2dCtx(c).getImageData(0, 0, c.width, c.height))
        _         <- IO{println("kfjashlfjashlfkjsdlfjljfdk"); globalImageData = imageData; println("globalimagedata set" + globalImageData)}
        _         <- IO(putImageDataOnCanvas(c)(0)).start
      } yield ()
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
        listener = showNoise(canvas2dCtx(c).getImageData(0, 0, c.width, c.height))
      )
      b
    }
    redButtonNoStr <- IO.delay {
      val b = button("red no streams").render
      b.addEventListener(
        `type` = "click",
        listener = showRedNoStreams
      )
      b
    }
    redButtonStr <- IO.delay {
        val b = button("red streams").render
        b.addEventListener(
          `type` = "click",
          listener = showRedStreams
        )
      b
    }
      redButtonChunkedStr <- IO.delay {
        val b = button("red chunked 640 streams").render
        b.addEventListener(
          `type` = "click",
          listener = showRedChunkedStreams
        )
        b
      }
      _ <-
      List(
        h1("ZIO fractals").render,
        c,
        fractalButton,
        noiseButton,
        redButtonNoStr,
        redButtonStr,
        redButtonChunkedStr
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


  def showNoise(imageData: ImageData): Event => Unit = e => {
    val pixels = for {
      rows <- Stream.range(0, imageData.height)
      cols <- Stream.range(0, imageData.width)
    } yield Pixel(rows, cols)

    def randomCp(pixel: Pixel): IO[ColoredPoint] = for {
      start   <- IO(System.currentTimeMillis())
      rr      <- IO(Random.nextInt(256))
      rg      <- IO(Random.nextInt(256))
      rb      <- IO(Random.nextInt(256))
      elapsed <- IO(System.currentTimeMillis() - start)
      _       <- if (pixel.x % 100  == 0 && pixel.y % 100 == 0) putStrLn(s"Random pixel generation took $elapsed millis") else IO(())
    } yield ColoredPoint(pixel, Color(rr, rg, rb))

    //running this long stream blocks the event loop. We shoudl find a way to have them running as _microtasks_
    (for {
      _ <- putStrLn("computing stream")
      _ <- pixels.covary[IO].delayBy(10.millis)
            .evalMap { p =>
              for {
//                cp <- randomCp(p)
                cp <- IO(ColoredPoint(p, Color(255, 0, 0)))
                _  <- paintOnImageData(cp, imageData)
              } yield ()
            }.compile.drain
      _ <- putStrLn("computed stream")
    } yield ()
    ).unsafeRunAsyncAndForget()

  }

  def showRedNoStreams: Event => Unit = e => {
    val startTime = System.currentTimeMillis()
    for {
      n <- Chunk(0 to globalImageData.width: _*)
      m <- Chunk(0 to globalImageData.height: _*)
    } yield paintOnImageDataUnsafe(ColoredPoint(Pixel(n, m), Color(255, 0, 0)), globalImageData)
    val endTime = System.currentTimeMillis()
    println(s"show red no streams: elapsed = ${endTime - startTime}")
  }

  def showRedStreams: Event => Unit = e => {
    val pixels = for {
      rows <- Stream.range(0, 640)
      cols <- Stream.range(0, 480)
    } yield Pixel(rows, cols)
    (for {
      startTime <- IO(System.currentTimeMillis())
      _ <- pixels.covary[IO].evalMap(p => paintOnImageData(ColoredPoint(p, Color(255, 0, 0)), globalImageData)).compile.drain
      endTime <- IO(System.currentTimeMillis())
      _ <- putStrLn(s"show red streams: elapsed = ${endTime - startTime}")
    } yield ()).unsafeRunSync()
  }

  def showRedChunkedStreams: Event => Unit = e => {
    val pixels = for {
      rows <- Stream.range(0, 640)
      cols <- Stream.range(0, 480)
    } yield Pixel(rows, cols)
    (for {
      startTime <- IO(System.currentTimeMillis())
        _ <- pixels.chunkN(640).covary[IO].evalMap { pChunk =>
          pChunk.traverse(p => paintOnImageData(ColoredPoint(p, Color(255, 0, 0)), globalImageData))
        }.compile.drain
        endTime <- IO(System.currentTimeMillis())
        _ <- putStrLn(s"show red streams: elapsed = ${endTime - startTime}")
    } yield ()).unsafeRunSync()
  }

  def paintOnCanvas(cp: ColoredPoint, canvas: Canvas): IO[Unit] =
//    (if (cp.pixel.x % 100 == 0 && cp.pixel.y % 100 == 0) putStrLn(s"painting $cp") else IO(())) >>
    IO {

    val ctx = canvas.getContext("2d").asInstanceOf[Ctx2D]
    val fillStyle = s"rgb(${cp.color.red.toInt}, ${cp.color.green.toInt}, ${cp.color.blue.toInt})"
    ctx.fillStyle = fillStyle
    ctx.fillRect(cp.pixel.x, cp.pixel.y, 1, 1)
  }

  def paintOnImageData(cp: ColoredPoint, imageData:ImageData): IO[Unit] =
    IO.apply {
      val pixelIndex: Int = (cp.pixel.y * imageData.width + cp.pixel.x) * 4
      imageData.data(pixelIndex) = cp.color.red.toInt
      imageData.data(pixelIndex + 1) = cp.color.green.toInt
      imageData.data(pixelIndex + 2) = cp.color.blue.toInt
      imageData.data(pixelIndex + 3) = 255
    }

  def paintOnImageDataUnsafe(cp: ColoredPoint, imageData:ImageData): Unit = {
//      println(s"putting on imageData $cp")
      val pixelIndex: Int = (cp.pixel.y * imageData.width + cp.pixel.x) * 4
      imageData.data(pixelIndex) = cp.color.red.toInt
      imageData.data(pixelIndex + 1) = cp.color.green.toInt
      imageData.data(pixelIndex + 2) = cp.color.blue.toInt
      imageData.data(pixelIndex + 3) = 255
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
