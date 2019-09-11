package io.tuliplogic.frontend

case class Pixel(x: Int, y: Int)
case class Color(red: Double, green: Double, blue: Double)

final case class ColoredPoint(pixel: Pixel, color: Color) {
  override def toString: String = s"(${pixel.x}, ${pixel.y}, [${color.red}, ${color.green}, ${color.blue}])"
}
