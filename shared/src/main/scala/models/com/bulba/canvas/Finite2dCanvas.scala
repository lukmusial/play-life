package models.com.bulba.canvas

import models.com.bulba.{DeadCell, Cell}
import models.com.bulba.stagingstrategy.Life2dStagingStrategy

trait Finite2dCanvas[+S <: Seq[Cell], +T <: Seq[S]] extends Canvas[S, T] {

  protected implicit val strategy = Life2dStagingStrategy

  // Cache dimensions for faster bounds checking
  protected lazy val canvasHeight: Int = canvas.length
  protected lazy val canvasWidth: Int = if (canvasHeight > 0) canvas(0).length else 0

  def getCell(x: Int, y: Int): Cell = {
    if (x < 0 || y < 0 || x >= canvasHeight || y >= canvasWidth) DeadCell
    else canvas(x)(y)
  }

  def getNeighbors(x: Int, y: Int): S = {
    // Use Array for speed, convert to Seq at end
    val neighbors = new Array[Cell](8)
    var idx = 0
    var i1 = x - 1
    while (i1 <= x + 1) {
      var y1 = y - 1
      while (y1 <= y + 1) {
        if (!(i1 == x && y1 == y)) {
          neighbors(idx) = getCell(i1, y1)
          idx += 1
        }
        y1 += 1
      }
      i1 += 1
    }
    neighbors.toSeq.asInstanceOf[S]
  }
}
