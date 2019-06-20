package io.tuliplogic.fractals

import zio.ZIO

/**
  *
  * zio-cases - 2019-04-02
  * Created with ♥ in Amsterdam
  */
package object canvas  {
  def canvasService[DrawOn]: ZCanvas.Service[DrawOn, ZCanvas[DrawOn]] = new ZCanvas.Service[DrawOn, ZCanvas[DrawOn]] {
    override def drawPoint(p: ColoredPoint)(drawOn: DrawOn): ZIO[ZCanvas[DrawOn], Nothing, Unit] = ZIO.accessM(_.canvas.drawPoint(p)(drawOn))
  }
}