package io.tuliplogic.frontend

import zio.{App, Task, UIO, ZIO, console}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import dom.document
import org.scalajs.dom.raw.Event
import scalatags.JsDom._
import scalatags.JsDom.all._
import zio.console.{Console, putStr}

/**
 * FrontendApp entry point
 */
@JSExportTopLevel("FrontendApp")
object FrontendApp extends App {

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
          b.addEventListener(`type` = "click", listener = (e: Event) => rts.unsafeRun(greet().provide(env)))
          b
        },
      ))(el => Task.effect(body.appendChild(el)))
    }
   yield ()

  def greet(): ZIO[Console, Nothing, Unit] = {
    console.putStrLn("Eccoci!")
  }

}
