package io.tuliplogic.fractals

import scalafx.scene.paint

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

case class Pixel(x: Int, y: Int)
case class Color(red: Double, green: Double, blue: Double)

object Color {
  def toCanvasColor(c: Color): paint.Color = paint.Color.rgb(c.red.toInt, c.green.toInt, c.blue.toInt)
}

final case class ColoredPoint(pixel: Pixel, color: Color) {
  override def toString: String = s"(${pixel.x}, ${pixel.y}, [${color.red}, ${color.green}, ${color.blue}])"
}

final case class ColoredBitmap(coloredPoints: List[ColoredPoint])

case class Frame(width: Int, height: Int) {
  def allPoints: List[Pixel] = for {
    xx <- (0 until width).toList
    yy <- (0 until height).toList
  } yield Pixel(xx, yy)

  def rows: List[List[Pixel]] = for {
    yy <- (0 until height).toList
  } yield (0 until width).toList.map(Pixel(_, yy))
}

case class ComplexRectangle(
  xMin: Double = -2.0,
  xMax: Double = 1.0,
  yMin: Double = -1.0,
  yMax: Double = 1.0,
  resolution: Frame
) {

  def pixelToComplex(p: Pixel): Complex =
    Complex(
      xMin + p.x * (xMax - xMin) / resolution.width,
      yMin + p.y * (yMax - yMin) / resolution.height
    )
}