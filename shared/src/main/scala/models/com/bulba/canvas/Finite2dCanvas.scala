package models.com.bulba.canvas

import models.com.bulba.{DeadCell, Cell}
import models.com.bulba.stagingstrategy.Life2dStagingStrategy

trait Finite2dCanvas[+S <: Seq[Cell], +T <: Seq[S]] extends Canvas[S, T] {

  protected implicit val strategy = Life2dStagingStrategy

  def getCell(x: Int, y: Int): Cell = (x, y) match {
    case (a, _) if a < 0 => DeadCell
    case (_, b) if b < 0 => DeadCell
    case (a, b) if a >= canvas.length || b >= canvas(a).length => DeadCell
    case (_, _) => canvas(x)(y)
  }

  def getNeighbors(x: Int, y: Int): S = (for {
    i1 <- x - 1 to x + 1
    y1 <- y - 1 to y + 1
    if !(i1 == x && y1 == y)
  } yield getCell(i1, y1)).asInstanceOf[S]
}
