package models.com.bulba.stagingstrategy

import models.com.bulba.{LiveCell, DeadCell, Cell}

object Life2dStagingStrategy extends StagingStrategy {
  def stage(cell: Cell, neighbors: Seq[Cell]): Cell = {
    (cell, neighbors.count(_.equals(LiveCell))) match {
      case (DeadCell, x) if x == 3 => LiveCell
      case (LiveCell, x) if 2 to 3 contains x => LiveCell
      case _ => DeadCell
    }
  }
}
