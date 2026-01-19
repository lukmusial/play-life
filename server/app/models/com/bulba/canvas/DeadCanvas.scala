package models.com.bulba.canvas

import models.com.bulba.{DeadCell, Cell}
import models.com.bulba.stagingstrategy.Life3dStagingStrategy

class DeadCanvas[+S <: Seq[Cell], +T <: Seq[S]] extends Canvas[S, T] {

  protected implicit val strategy = Life3dStagingStrategy

  override def getCell(x: Int, y: Int): Cell = DeadCell

  override def getNeighbors(x: Int, y: Int): S = Seq.empty[Cell].asInstanceOf[S]

  override def stage(): Canvas[S, T] = this

  override val canvas: T = Seq.empty[S].asInstanceOf[T]

  override def haveNeighborsChanged(x: Int, y: Int): Boolean = false

  override val changedCells: Set[(Int, Int)] = Set.empty
}
