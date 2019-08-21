package io.tuliplogic.frontend

import zio.{App, Task, UIO, ZIO, console}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import dom.document
import scalatags.JsDom._
import scalatags.JsDom.all._
import zio.console.putStr

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


  def buildDom(): Task[Unit] = for {
    env  <- ZIO.environment
    body <- Task.effectTotal(document.body)
    canvas <- ZIO.foreach(
      List(
        h1("ZIO fractals"),
        canvas(id := "canvas", width:= 640, height := 480, color := "blue"),
        button("draw", onclick := "FrontendApp.greet()"),
      ))(el => Task.effect(body.appendChild(el.render)))
    }
   yield ()

  @JSExport
  def greet(): Unit = {
    println("Eccoci!")
  }

}
