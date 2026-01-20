package models.com.bulba

import models.com.bulba.stagingstrategy.StagingStrategy

sealed trait Cell {
  def stage(neighbors: Seq[Cell], strategy: StagingStrategy): Cell
  def toNumber: Int
  def isAlive: Boolean
}

case object LiveCell extends Cell {
  def stage(neighbors: Seq[Cell], strategy: StagingStrategy): Cell = {
    strategy.stage(this, neighbors)
  }

  override def toString: String = "1"
  override def toNumber: Int = 1
  override def isAlive: Boolean = true
}

case object DeadCell extends Cell {
  def stage(neighbors: Seq[Cell], strategy: StagingStrategy): Cell = {
    strategy.stage(this, neighbors)
  }

  override def toString: String = "0"
  override def toNumber: Int = 0
  override def isAlive: Boolean = false
}
