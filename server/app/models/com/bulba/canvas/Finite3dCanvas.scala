package models.com.bulba.canvas

import models.com.bulba.{DeadCell, Cell}
import models.com.bulba.stagingstrategy.Life3dStagingStrategy


trait Finite3dCanvas[+S <: Seq[Cell], +T <: Seq[S]] extends Canvas[S, T] {
  def canvasBelow: Canvas[S, T]

  def canvasAbove: Canvas[S, T]

  protected implicit val strategy = Life3dStagingStrategy

  def getCell(x: Int, y: Int): Cell = (x, y) match {
    case (a, b) if a >= 0 && a < canvas.length && b >=0 && b < canvas(a).length => canvas(a)(b)
    case _ => DeadCell
  }

  def getNeighbors(x: Int, y: Int): S = {
    val neighbors = for {i1 <- x - 1 to x + 1; y1 <- y - 1 to y + 1; if !(i1 == x && y1 == y)} yield getCell(i1, y1)
    val belowCells = for {i1 <- x - 1 to x + 1; y1 <- y - 1 to y + 1} yield canvasBelow.getCell(i1, y1)
    val aboveCells = for {i1 <- x - 1 to x + 1; y1 <- y - 1 to y + 1} yield canvasAbove.getCell(i1, y1)
    (aboveCells ++ neighbors ++ belowCells).asInstanceOf[S]
  }

}