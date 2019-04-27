package io.tuliplogic.fractals

import io.tuliplogic.fractals.UIComponents.{FractalTypeUI, FrameSizeSelectUI, FramesUI}
import io.tuliplogic.fractals.UIModel.FractalType
import io.tuliplogic.fractals.algo.FractAlgo
import io.tuliplogic.fractals.canvas.ZCanvas.ZCanvasFxLive
import io.tuliplogic.fractals.coloring.Coloring
import io.tuliplogic.fractals.fractal.ComputationStrategy
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color.Black
import scalafx.Includes._
import scalaz.zio.DefaultRuntime
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.{Button, Label, RadioButton, TextField, ToggleGroup}


object UIModel {

  sealed trait FractalType
  object FractalType {
    case object Mandelbrot                               extends FractalType
    case class  Julia(c: Complex = Complex(0.34, -0.05)) extends FractalType
  }

  sealed trait FrameSize {
    val w: Int
    val h: Int
  }
  object FrameSize {
    case object F1 extends FrameSize { val w = 640; val h  = 480  }
    case object F2 extends FrameSize { val w = 1280; val h = 720  }
    case object F3 extends FrameSize { val w = 1920; val h = 1080 }
  }

}

object UIComponents {

  object FractalTypeUI {
    val fractalTypeGrp = new ToggleGroup()
    val mandelbrotTb    = new RadioButton("Mandelbrot")
    val juliaTb         = new RadioButton("Julia")
    fractalTypeGrp.toggles = List(mandelbrotTb, juliaTb)

    val cR = new TextField()
    cR.prefWidth = 120
    val cRLabel = new Label("Re(c)")
    val cI = new TextField()
    cI.prefWidth = 120

    val cILabel = new Label("Im(c)")
    val cBox = new HBox(cRLabel, cR, cILabel, cI)


    def selectedFractalType: FractalType = if (juliaTb.isSelected) FractalType.Julia(Complex(cR.text.value.toDouble, cI.text.value.toDouble))
      else FractalType.Mandelbrot

    val box = new VBox(new Label("Fractal Type"), FractalTypeUI.mandelbrotTb, FractalTypeUI.juliaTb, cBox)
  }

  object FrameSizeSelectUI {
    val canvasSizeGrp = new ToggleGroup()

    val f1 = new RadioButton("640 x 480")
    val f2 = new RadioButton("1280 x 720")
    val f3 = new RadioButton("1920 x 1080")

    val all = List(f1, f2, f3)
    canvasSizeGrp.toggles = List(f1, f2, f3)

    val box = new VBox(new Label("Canvas Size"), f1, f2, f3)
    box.style = """

      -fx-border-color: white;
      -fx-border-width: 1;
      -fx-padding: 6;
      -fx-margin: 6;"""

    def selectedFrameSizeIndex: Int = if (f1.isSelected) 0
      else if (f2.isSelected) 1
      else 2

  }


  object FramesUI {

    val borderStyle =  """
      -fx-border-color: white;
      -fx-border-width: 1;
      -fx-padding: 6;
      -fx-margin: 6;"""

    val f1 = new Canvas(640, 480)
    val f2 = new Canvas(1280, 720)
    val f3 = new Canvas(1920, 1080)
    val all = List(f1, f2, f3)
    all.foreach {
      _.style = borderStyle
    }

    val box = new HBox{ children = all }

    def makeVisible(index: Int): Option[Canvas] = if (index >= 0 && index < all.size) {
      val withIndex = all.zipWithIndex
      val (selected, unselected) = withIndex.partition { case (_, ix) => ix == index }

      unselected.foreach { case (c, _) =>
        c.visible = false
        c.managed = false
      }

      selected.foreach { case (c, _) =>
        c.visible = true
        c.managed = true
      }
      selected.headOption.map(_._1)
    } else None

  }

}

object FractalUI extends JFXApp {

  val rts = new DefaultRuntime {}

  def env(w: Int, h: Int, fractalType: FractalType): ZCanvasFxLive with Console.Live with Clock.Live with Clock with Coloring.AColoring with FractAlgo =
    fractalType match {
      case FractalType.Mandelbrot =>
        new ZCanvasFxLive with Console.Live with Clock.Live with Coloring.AColoring with FractAlgo.MandelbrotAlgo {}
      case FractalType.Julia(c_) =>
        new ZCanvasFxLive with Console.Live with Clock.Live with Coloring.AColoring with FractAlgo.JuliaAlgo {
          val c = c_
        }
    }

  val computeButton = new Button("Compute")
  computeButton.onAction = (_: ActionEvent) => {
    val selectedFrameIndex = FrameSizeSelectUI.selectedFrameSizeIndex
    FramesUI.makeVisible(selectedFrameIndex)
    val selectedFrame: Canvas = FramesUI.all(selectedFrameIndex)
    selectedFrame.graphicsContext2D.clearRect(0, 0, selectedFrame.width.toDouble, selectedFrame.height.toDouble)
    val width = selectedFrame.width.toInt
    val height = selectedFrame.height.toInt
    val selectedFractalType = FractalTypeUI.selectedFractalType

    val environment = env(width, height, selectedFractalType)

//    rts.unsafeRunAsync(fractal.calculateAllAndDrawAll(ComputationStrategy.ParallelRows)(width, height)(selectedFrame).provide(environment))(_ => ())
    rts.unsafeRun(fractal.calculateAllAndDrawAll(ComputationStrategy.ParallelRows)(width, height)(selectedFrame).provide(environment))
  }

  val testFormButton = new Button("Test")
  testFormButton.onAction = (e: ActionEvent) => {
    println("selected fractal type:" + FractalTypeUI.selectedFractalType + "frameSize: " + FrameSizeSelectUI.selectedFrameSizeIndex)
  }


  val box = new HBox {
    padding = Insets(20)
    children = Seq(
      new VBox(FractalTypeUI.box, FrameSizeSelectUI.box, computeButton, testFormButton), FramesUI.box
    )
  }

  stage = new PrimaryStage {
    title = "Functional Julia"
    //    val label = new Label("Compute")

    scene = new Scene {
      fill = Black
      content = box
    }
  }

}

