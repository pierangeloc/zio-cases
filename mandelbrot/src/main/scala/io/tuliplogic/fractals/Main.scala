package io.tuliplogic.fractals

import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.Canvas.CanvasLive
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color.Black
import scalaz.zio.DefaultRuntime
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalafx.scene.canvas.{Canvas => SCanvas}

object Main extends JFXApp {
  self =>

  val rts = new DefaultRuntime {}
  val env = new SCanvas(600, 400) with CanvasLive with Console.Live with Clock.Live with Coloring.AColoring with FractAlgo.MandelbrotAlgo {}

  stage = new PrimaryStage {
    title = "Functional Mandelbrot"

    scene = new Scene {
      fill = Black
      content = new HBox {
        padding = Insets(20)
        children = Seq(
          env
        )
      }
    }
  }

  rts.unsafeRun(fractal.calculationAndDrawingProgram(env.getWidth.intValue(), env.getHeight.intValue())(ComputationStrategy.ParallelRows).provide(env))
}


object MainJulia extends JFXApp {
  self =>

  val rts = new DefaultRuntime {}
  val env = new SCanvas(600, 400) with CanvasLive with Console.Live with Clock.Live with Coloring.AColoring with FractAlgo.JuliaAlgo { val c = Complex(0.34, -0.05)}

  stage = new PrimaryStage {
    title = "Functional Mandelbrot"

    scene = new Scene {
      fill = Black
      content = new HBox {
        padding = Insets(20)
        children = Seq(
          env
        )
      }
    }
  }

  rts.unsafeRun(fractal.calculationAndDrawingProgram(env.getWidth.intValue(), env.getHeight.intValue())(ComputationStrategy.ParallelRows).provide(env))
}