package models.com.bulba.canvas

import models.com.bulba.Cell
import models.com.bulba.stagingstrategy.StagingStrategy
import scala.collection.parallel.CollectionConverters._

trait Canvas[+S <: Seq[Cell], +T <: Seq[S]] {

  val canvas: T

  protected implicit val strategy: StagingStrategy

  def getCell(x: Int, y: Int): Cell

  def getNeighbors(x: Int, y: Int): S

  val changedCells: Set[(Int, Int)]

  def haveNeighborsChanged(x: Int, y: Int): Boolean

  override def toString: String = {
    canvas map (_.mkString("")) mkString "\n"
  }

  def toNumericSequence: Seq[Seq[Long]] = {

    def rowToSeqLong(row: Seq[Cell]): Seq[Long] = {
      if (row.length > 53) {
        val rows = row.splitAt(53)
        Seq(java.lang.Long.parseLong(rows._1.mkString, 2)) ++ rowToSeqLong(rows._2)
      } else Seq(java.lang.Long.parseLong(row.mkString, 2))
    }
    canvas.par.map(rowToSeqLong(_)).seq
  }

  def toHex: Seq[String] = {

    def rowToHex(row: Seq[Cell]): String = {
      row.length match {
        case x if x == 0 => ""
        case x if x < 8 => Integer.parseInt(row.mkString, 2).asInstanceOf[Char].toString
        case _ => Integer.parseInt(row.take(8).mkString, 2).asInstanceOf[Char] + rowToHex(row.drop(8))
      }
    }

    Seq(canvas.length.toString, canvas.head.length.toString) ++ canvas.par.map(rowToHex(_)).seq
  }

  def stage(): Canvas[S, T]

}








