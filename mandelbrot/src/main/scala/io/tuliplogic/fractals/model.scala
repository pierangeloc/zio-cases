package io.tuliplogic.fractals

import scalafx.scene.paint.Color

case class Complex(x: Double, y: Double) { self =>
  def +(other: Complex): Complex = Complex(self.x + other.x, self.y + other.y)
  def *(other: Complex): Complex = (self, other) match {
    case (Complex(a, b), Complex(c, d)) => Complex(a * c - b * d, a * d + b * c)
  }
  def squaredAbs: Double = x * x + y * y
}

object Complex {
  val zero = Complex(0, 0)
  val one = Complex(1, 0)
}


final case class ColoredPoint(x: Int, y: Int, color: Color) {
  override def toString: String = s"($x, $y, [${color.red}, ${color.green}, ${color.blue}])"
}

final case class ColoredBitmap(coloredPoints: List[ColoredPoint])



case class Frame(width: Int = 600, height: Int = 400) {
  def allPoints: List[(Int, Int)] = for {
    xx <- (0 until width).toList
    yy <- (0 until height).toList
  } yield (xx, yy)
}

case class ComplexRectangle(
  xMin: Double = -2.0,
  xMax: Double = 1.0,
  yMin: Double = -1.0,
  yMax: Double = 1.0
) {

  def pixelToComplex(frame: Frame)(x: Int, y: Int): Complex =
    Complex(
      xMin + x * (xMax - xMin) / frame.width,
      yMin + y * (yMax - yMin) / frame.height
    )
}