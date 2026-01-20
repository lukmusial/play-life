package models.com.bulba

import models.com.bulba.canvas.Canvas

class GameState(var canvas: Canvas[Seq[Cell], Seq[Seq[Cell]]]) {

  def advance(): Unit = {
    canvas = canvas.stage()
  }

  override def toString: String = {
    canvas.toString
  }

  def toHex: Seq[String] = {
    canvas.toHex
  }
}
