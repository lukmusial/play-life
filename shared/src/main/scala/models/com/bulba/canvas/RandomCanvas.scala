package models.com.bulba.canvas

import scala.util.Random
import models.com.bulba._

case class RandomCanvas(width: Int, height: Int) extends Finite2dCanvas[VC, VVC] {
  def stage(): Canvas[VC, VVC] = Vector2dCanvas(canvas, changedCells)

  val canvas: VVC = {
    val interim = for (i <- 0 until width) yield
      for (y <- 0 until height) yield
        if (Random.nextInt(10) > 8) LiveCell else DeadCell
    interim.map(_.toVector).toVector
  }

  override def haveNeighborsChanged(x: Int, y: Int): Boolean = true

  override val changedCells: Set[(Int, Int)] = (for (x <- 0 until width; y <- 0 until height) yield (x, y)).toSet
}
