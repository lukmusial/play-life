package models.com.bulba.canvas

import models.com.bulba.{Cell, VC, VVC}
import scala.collection.mutable.ArrayBuffer

case class Vector2dCanvas(override val canvas: VVC, override val changedCells: Set[(Int, Int)])
  extends Finite2dCanvas[VC, VVC] {

  protected def stagedCells: VVC = {
    val height = canvas.length
    val width = if (height > 0) canvas(0).length else 0
    val result = new Array[Vector[Cell]](height)

    var i = 0
    while (i < height) {
      val rowBuffer = new Array[Cell](width)
      var j = 0
      while (j < width) {
        rowBuffer(j) = if (haveNeighborsChanged(i, j)) {
          getCell(i, j).stage(getNeighbors(i, j), strategy)
        } else {
          getCell(i, j)
        }
        j += 1
      }
      result(i) = rowBuffer.toVector
      i += 1
    }
    result.toVector
  }

  def stage(): Vector2dCanvas = {
    val staged = stagedCells
    val changedBuffer = ArrayBuffer.empty[(Int, Int)]
    var x = 0
    while (x < staged.length) {
      var y = 0
      while (y < staged(x).length) {
        if (canvas(x)(y) != staged(x)(y)) {
          changedBuffer += ((x, y))
        }
        y += 1
      }
      x += 1
    }
    Vector2dCanvas(staged, changedBuffer.toSet)
  }

  override def haveNeighborsChanged(x: Int, y: Int): Boolean = {
    var i1 = x - 1
    while (i1 <= x + 1) {
      var y1 = y - 1
      while (y1 <= y + 1) {
        if (changedCells.contains((i1, y1))) return true
        y1 += 1
      }
      i1 += 1
    }
    false
  }
}
