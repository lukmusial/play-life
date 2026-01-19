package models.com.bulba.canvas

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import models.com.bulba.{Cell, VC, VVC}

import ExecutionContext.Implicits.global

abstract class VectorCanvas extends Canvas[VC, VVC] {

  protected def stagedCells: VVC = {
    val listOfFutures = for (i <- canvas.indices) yield
      Future {
        val row = canvas(i).indices.foldLeft(Vector.empty[Cell]){(acc, j) =>
          haveNeighborsChanged(i, j) match {
            case true => acc :+ getCell(i, j).stage(getNeighbors(i, j), strategy)
            case false => acc :+ getCell(i, j)
          }
        }
        (i, row)
      }
    Await.result(Future.sequence(listOfFutures), Duration(10, SECONDS)).sortBy(_._1).map(_._2).toVector
  }

}
