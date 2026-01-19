package models.com.bulba

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.canvas.Vector3dCanvas


class Vector3dCanvasSpec extends AnyFlatSpec with Matchers {

  // D L L
  // L D D
  // D L D
  val canvasAbove = Vector(
    Vector(DeadCell, LiveCell, LiveCell),
    Vector(LiveCell, DeadCell, DeadCell),
    Vector(DeadCell, LiveCell, DeadCell))

  // L L L
  // L D D
  // D D L
  val canvasBelow = Vector(
    Vector(LiveCell, LiveCell, LiveCell),
    Vector(LiveCell, DeadCell, DeadCell),
    Vector(DeadCell, DeadCell, LiveCell))

  // D D D
  // D D L
  // D D D
  val canvas = Vector(
    Vector(DeadCell, DeadCell, DeadCell),
    Vector(DeadCell, DeadCell, LiveCell),
    Vector(DeadCell, DeadCell, DeadCell))

  val canvases = Vector(canvasBelow, canvas, canvasAbove)

  lazy val lay: Layers[VC, VVC]  = new Layers(for (i <- 0 until 3) yield new Vector3dCanvas(canvases(i), i, lay, (for (x <- 0 until 3; y<-0 until 3) yield (x, y)).toSet))

  "3d canvas" should "return correct neighbors" in {
    lay(1).getNeighbors(1,1) should be (Seq(
      DeadCell, LiveCell, LiveCell,
      LiveCell, DeadCell, DeadCell,
      DeadCell, LiveCell, DeadCell,

      DeadCell, DeadCell, DeadCell,
      DeadCell, LiveCell,
      DeadCell, DeadCell, DeadCell,

      LiveCell, LiveCell, LiveCell,
      LiveCell, DeadCell, DeadCell,
      DeadCell, DeadCell, LiveCell
    ))
  }

  "3d canvas" should "stage correctly" in {
    val staged = lay(1).stage()
    staged.getCell(2,2) should be (DeadCell)
    staged.getCell(1,1) should be (LiveCell)
    staged.getCell(1,2) should be (LiveCell)
    staged.getCell(0,2) should be (DeadCell)
  }

   "3d canvas" should "return dead neighbors for border cells" in {
     val deadCanvas = for (i <- 0 until 26) yield DeadCell
      lay(1).getNeighbors(-5, -5) should be (deadCanvas)
      lay(1).getNeighbors(10, 10) should be (deadCanvas)
  }

  "3d canvas" should "only stage cells for which neighbors changed in previous iteration" in {
    lazy val lay: Layers[VC, VVC]  = new Layers(for (i <- 0 until 3) yield new Vector3dCanvas(canvases(i), i, lay, Set.empty))
    val staged = lay(1).stage()
    staged.canvas should be(lay(1).canvas)
  }

  "3d canvas" should "populate set of changed cells" in {
    lay(1).stage().changedCells should be (Set((1,1)))
    lay(0).stage().changedCells should be (Set((0,2), (0,0), (2,2), (0,1), (1,0)))
    lay(2).stage().changedCells should be (Set((0,1), (0,2), (1,0), (2,1)))
  }

}
