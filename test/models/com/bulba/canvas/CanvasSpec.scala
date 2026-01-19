package models.com.bulba.canvas

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.stagingstrategy.StagingStrategy
import models.com.bulba.{LiveCell, DeadCell, Cell}

class CanvasSpec extends AnyFlatSpec with Matchers {


  "Canvas" should "encode cells to a sequence of characters" in {
    val canvas: Seq[Seq[Cell]] = Vector(
      Vector(DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, LiveCell, LiveCell),
      Vector(DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell),
      Vector(LiveCell, LiveCell, LiveCell, LiveCell, LiveCell, LiveCell, LiveCell, LiveCell),
      Vector(LiveCell, LiveCell, LiveCell, LiveCell),
      Vector(LiveCell, LiveCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, LiveCell)
    )
    val testCanvas = new TestCanvas(canvas).toHex.iterator
    testCanvas.next() should be("5")
    testCanvas.next() should be("8")
    testCanvas.next().charAt(0).asInstanceOf[Int] should be (3)
    testCanvas.next().charAt(0).asInstanceOf[Int] should be (0)
    testCanvas.next().charAt(0).asInstanceOf[Int] should be (255)
    testCanvas.next().charAt(0).asInstanceOf[Int] should be (15)
    val lastCanvas = testCanvas.next()
    lastCanvas.charAt(0).asInstanceOf[Int] should be (192)
    lastCanvas.charAt(1).asInstanceOf[Int] should be (1)
  }

}


class TestCanvas(val canvas: Seq[Seq[Cell]]) extends Canvas[Seq[Cell], Seq[Seq[Cell]]] {
    def stage(): Canvas[Seq[Cell], Seq[Seq[Cell]]] = this

    def haveNeighborsChanged(x: Int, y: Int): Boolean = false

    val changedCells: Set[(Int, Int)] = Set.empty

    def getNeighbors(x: Int, y: Int): Seq[Cell] = Seq.empty

    def getCell(x: Int, y: Int): Cell = DeadCell

    protected implicit val strategy: StagingStrategy = null
}
