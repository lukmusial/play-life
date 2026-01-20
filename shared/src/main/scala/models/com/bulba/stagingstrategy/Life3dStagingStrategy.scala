package models.com.bulba.stagingstrategy

import models.com.bulba.{LiveCell, DeadCell, Cell}

// B9 10 11/S 6 7 8 9 10 11
object Life3dStagingStrategy extends StagingStrategy {
  private val range9To11 = 9 to 11
  private val range6to11 = 6 to 11

  def stage(cell: Cell, neighbors: Seq[Cell]): Cell = {
    (cell, neighbors.count(_.equals(LiveCell))) match {
      case (DeadCell, x) if range9To11 contains x => LiveCell
      case (LiveCell, x) if range6to11 contains x => LiveCell
      case _ => DeadCell
    }
  }
}
