package models.com.bulba

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.canvas.Vector2dCanvas

class Vector2dCanvasSpec extends AnyFlatSpec with Matchers {

  "2d array canvas" should "populate set of changed cells" in {
      val canvas = new Vector2dCanvas(Vector(Vector(LiveCell, LiveCell), Vector(DeadCell, LiveCell), Vector(DeadCell, DeadCell)), Set((0,1), (0, 0), (1, 0), (1,1)))
      canvas.stage().changedCells should be(Set((1,0)))
  }

  "2d array canvas" should "not stage cells that have not changed since previous iteration" in {
    val canvas = new Vector2dCanvas(Vector(Vector(LiveCell, LiveCell),
      Vector(DeadCell, LiveCell),
      Vector(DeadCell, DeadCell)),
      Set.empty)
    val stagedCanvas = canvas.stage()
    stagedCanvas.changedCells should be(Set.empty)

  }


}
