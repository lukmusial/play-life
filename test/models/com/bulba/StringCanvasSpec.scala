package models.com.bulba

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.canvas.StringCanvas


class StringCanvasSpec extends AnyFlatSpec with Matchers {

  val entry1Canvas = """
    0000
    0010
    0110
    0000
                     """.stripMargin

  "String canvas" should "return correct neighbors" in {
    StringCanvas(entry1Canvas).getNeighbors(1,1) should be (Seq(DeadCell, DeadCell, DeadCell, DeadCell, LiveCell, DeadCell, LiveCell, LiveCell))
  }

  "String canvas" should "return dead neighbors for border cells" in {
    StringCanvas(entry1Canvas).getNeighbors(2,0) should be (Seq(DeadCell, DeadCell, DeadCell, DeadCell, LiveCell, DeadCell, DeadCell, DeadCell))
    StringCanvas(entry1Canvas).getNeighbors(0,4) should be (Seq(DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell))
    StringCanvas(entry1Canvas).getNeighbors(10,10) should be (Seq(DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell))
    StringCanvas(entry1Canvas).getNeighbors(-5,-5) should be (Seq(DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell, DeadCell))
  }

}
