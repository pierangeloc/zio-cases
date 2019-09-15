package io.tuliplogic.frontend

import java.nio.ByteBuffer

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2._
import fs2.concurrent.Queue
import io.tuliplogic.frontend.FrontendAppFs2CE.putStrLn
import org.scalajs.dom
import org.scalajs.dom.experimental.{Fetch, ReadableStreamReader}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Event, HTMLElement}
import org.scalajs.dom.{ImageData, Node, document}
import scalatags.JsDom.all._

import scala.concurrent.duration._
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.typedarray._
import scala.util.Random

/**
 * FrontendApp entry point
 */
@JSExportTopLevel("FrontendAppFs2WrappedCE")
object FrontendAppFs2WrappedCE extends IOApp {
  type Ctx2D = dom.CanvasRenderingContext2D
  type Reff[A] = Ref[IO, A]

  sealed trait Error extends Throwable
  object Error {
    case class Generic(msg: String) extends Error
  }

  class SafeCanvas[F[_]: Sync](private val c: Canvas) {

    val width: Int = c.width
    val height: Int = c.height
    private val ctx = c.getContext("2d").asInstanceOf[Ctx2D]
    private val imageData = ctx.getImageData(0, 0, c.width, c.height)
    putImageDataOnCanvas(c)(0)
    //handle periodic refresh
    private def putImageDataOnCanvas(c: Canvas)(tFrame: Double): Unit = {
      dom.window.requestAnimationFrame(putImageDataOnCanvas(c))
      //    println(s"putting imageData on canvas at t = $tFrame")
      ctx.putImageData(imageData, 0, 0)
    }

    def update(cps: (ColoredPoint)*): F[Unit] = Sync[F].delay {
      for {
        cp <- cps
      } yield {
        val pixelIndex: Int = (cp.pixel.y * imageData.width + cp.pixel.x) * 4
        imageData.data(pixelIndex) = cp.color.red.toInt
        imageData.data(pixelIndex + 1) = cp.color.green.toInt
        imageData.data(pixelIndex + 2) = cp.color.blue.toInt
        imageData.data(pixelIndex + 3) = 255
      }
    }

    def updateChunk(cps: Chunk[ColoredPoint]): F[Unit] = Sync[F].delay {
      cps.foreach { cp => {
        val pixelIndex: Int = (cp.pixel.y * imageData.width + cp.pixel.x) * 4
        imageData.data(pixelIndex) = cp.color.red.toInt
        imageData.data(pixelIndex + 1) = cp.color.green.toInt
        imageData.data(pixelIndex + 2) = cp.color.blue.toInt
        imageData.data(pixelIndex + 3) = 255
      }
      }
    }
  }

  object SafeCanvas {
    def addTo[F[_]: Sync](parentNode: HTMLElement, width: Int, height: Int, idd: String): F[SafeCanvas[F]] = Sync[F].delay {
      val c = canvas(id := idd, widthA:= 640, heightA := 480, color := "blue").render
      parentNode.appendChild(c)
      new SafeCanvas[F](c)
    }
  }

  class SafeTextInput[F[_]: Sync](val content: Ref[F, String]) {
    def getContent: F[String] = content.get
    def updateContent(s: String): F[Unit] = content.set(s)
  }

  object SafeTextInput {
    def addTo[F[_]](parentNode: HTMLElement, initialValue: String, idd: String)(implicit F: ConcurrentEffect[F]): F[SafeTextInput[F]] = for {
      ref <- Ref.of[F, String]("")
      _   <- Sync[F].delay {
        val textInput = input(`type` := "text").render
        textInput.addEventListener(
          `type` = "change",
          listener = (e: Event) => F.toIO(ref.set(textInput.value)).unsafeRunAsyncAndForget
        )
        parentNode.appendChild(textInput)
      }
    } yield new SafeTextInput[F](ref)
  }

  // credits to https://tech.ovoenergy.com/frontend-development-with-scala-js/
  def addButton[F[_]](parentNode: HTMLElement, label: String)(implicit F: ConcurrentEffect[F]): Stream[F, Event] = {
    def addElement(q: Queue[F, Event]): F[Node] = F.delay {
      val newElement = input(
        `type` := "button",
        `value` := label
      ).render
      newElement.addEventListener(
        `type` = "click",
        listener = (e: Event) => F.toIO(q.enqueue1(e)).unsafeRunAsyncAndForget
      )
      parentNode.appendChild(newElement)
    }

    def removeElement(element: Node): F[Unit] =
      F.delay(parentNode.removeChild(element))

    Stream.eval(Queue.circularBuffer[F, Event](maxSize = 10))
      .flatMap { queue =>
        Stream.bracket(addElement(queue))(removeElement)
          .flatMap(_ => queue.dequeue)
      }
  }

  val plotFractalsUrl = "http://localhost:8080/zio-mandelbrot/plot-fractals"

  def putStrLn[F[_]: Sync](s: String): F[Unit] = Sync[F].delay(println(s))

  override def run(args: List[String]): IO[ExitCode] =
    buildDom[IO]().compile.drain.redeem(_ => ExitCode.Error, _ => ExitCode.Success)


  def buildDom[F[_]: ConcurrentEffect](): Stream[F, Unit] =
    for {
      body         <- Stream.eval(Sync[F].delay(document.body))
      sc           <- Stream.eval(SafeCanvas.addTo[F](body, 640, 480, "canvas"))
      buttonEvents <- addButton(body, "paint noise fs2")
//      _            <- Stream.eval(showNoiseStreams(sc))
      _            <- Stream.eval(showFractals(sc))
    } yield ()

  def randomCp[F[_]](pixel: Pixel)(implicit F: Sync[F]): F[ColoredPoint] = for {
    start   <- F.delay(System.currentTimeMillis)
      rr      <- F.delay(Random.nextInt(256))
      rg      <- F.delay(Random.nextInt(256))
      rb      <- F.delay(Random.nextInt(256))
      elapsed <- F.delay(System.currentTimeMillis() - start)
  } yield ColoredPoint(pixel, Color(rr, rg, rb))

  def randomCpChunk[F[_]](ps: Chunk[Pixel])(implicit F: Sync[F]): F[Chunk[ColoredPoint]] = F.delay {
    ps.map {pixel => ColoredPoint(pixel, Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)))}
  }

  def showNoiseStreams[F[_]: ConcurrentEffect](sc: SafeCanvas[F]): F[Unit] = {
    val pixels = Stream.chunk{
      for {
        rows <- Chunk(0 to sc.width: _*)
        cols <- Chunk(0 to sc.height: _*)
      } yield Pixel(rows, cols)
    }

    for {
      startTime <- Sync[F].delay(System.currentTimeMillis())
      _         <- pixels.covary[F].chunkN(640 * 480 / 5).evalMap { pChunk =>
                      randomCpChunk(pChunk).flatMap{ cpChunk => sc.updateChunk(cpChunk)
                      }
                    }.compile.drain
      endTime   <- Sync[F].delay(System.currentTimeMillis())
      _         <- putStrLn(s"show red streams: elapsed = ${endTime - startTime}")
    } yield ()
  }

  import io.circe.generic.auto._
  import io.circe.parser.decode

  def showFractals[F[_]: ConcurrentEffect](sc: SafeCanvas[F]): F[Unit] =
    (putStrLn[F]("computing and displaying fractals") >> getStreamBytes
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .map(s => decode[ColoredPoint](s))
      .collect { case Right(cp) => cp }
      .chunkN(640 * 480 / 50)
      .evalMap(cpChunk => sc.updateChunk(cpChunk))
      .compile.drain)

  def getStreamBytes[F[_]: ConcurrentEffect]: Stream[F, Byte] =
    for {
      reader <- Stream.eval(
          promiseToTask(Fetch.fetch(plotFractalsUrl)).map(_.body.getReader)
        )
      bb <- processReader(reader)
      b  <- Stream.emits(bb.array())
    } yield b

  def processReader[F[_]: ConcurrentEffect](reader: ReadableStreamReader[Uint8Array]): Stream[F, ByteBuffer] =
    Stream.eval(promiseToTask(reader.read())).repeat
      .takeWhile(_.done == false)
      .map { chunk =>
        ByteBuffer.wrap(new Int8Array(chunk.value.buffer).toArray)
      }

  def promiseToTask[F[_], A](p: => Promise[A])(implicit F: ConcurrentEffect[F]): F[A] = F.liftIO(IO.fromFuture(IO(p.toFuture)))

}

