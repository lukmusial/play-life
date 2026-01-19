package models.com.bulba.stagingstrategy

import models.com.bulba.Cell

trait StagingStrategy {
  def stage(cell: Cell, neighbors: Seq[Cell]): Cell
}
