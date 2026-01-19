package models.com.bulba.canvas

import models.com.bulba.{Cell, VC, VVC}

case class Vector2dCanvas(override val canvas: VVC, override val changedCells: Set[(Int, Int)])
  extends Finite2dCanvas[VC, VVC] {

  protected def stagedCells: VVC = {
    val staged = for (i <- canvas.indices) yield {
      val row = canvas(i).indices.foldLeft(Vector.empty[Cell]) { (acc, j) =>
        haveNeighborsChanged(i, j) match {
          case true => acc :+ getCell(i, j).stage(getNeighbors(i, j), strategy)
          case false => acc :+ getCell(i, j)
        }
      }
      row
    }
    staged.toVector
  }

  def stage(): Vector2dCanvas = {
    val staged = stagedCells
    val changed = for (x <- staged.indices; y <- staged(x).indices; if canvas(x)(y) != staged(x)(y)) yield (x, y)
    Vector2dCanvas(staged, changed.toSet)
  }

  override def haveNeighborsChanged(x: Int, y: Int): Boolean = {
    for { i1 <- x - 1 to x + 1; y1 <- y - 1 to y + 1 } if (changedCells.contains((i1, y1))) return true
    false
  }
}
