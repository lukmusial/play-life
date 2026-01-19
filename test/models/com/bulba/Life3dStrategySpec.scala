package models.com.bulba

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.stagingstrategy.Life3dStagingStrategy


class Life3dStrategySpec extends AnyFlatSpec with Matchers {

  //B9 10 11/S 6 7 8 9 10 11
  val strategy = Life3dStagingStrategy

  "Any live cell"  should "die if with fewer than six neighbors" in {
    for(x <- 0 to 5) {
      Life3dStagingStrategy.stage(LiveCell, neighborsWithLiveCells(x)) should be (DeadCell)
    }
  }

  it should "die if more than 11 live neighbors" in {
    for(x <- 12 to 26) {
      Life3dStagingStrategy.stage(LiveCell, neighborsWithLiveCells(x)) should be (DeadCell)
    }
  }

  it should "live if 6 to 11 neighbors" in {
    for(x <- 6 to 11) {
      Life3dStagingStrategy.stage(LiveCell, neighborsWithLiveCells(x)) should be (LiveCell)
    }
  }

  "Any dead cell" should "come to life if between 9 and 11 neighbors" in {
    for(x <- 9 to 11) {
      Life3dStagingStrategy.stage(DeadCell, neighborsWithLiveCells(x)) should be (LiveCell)
    }
  }

  it should "remain dead otherwise" in {
    for(x <- 0 to 8) {
      Life3dStagingStrategy.stage(DeadCell, neighborsWithLiveCells(x)) should be (DeadCell)
    }
    for(x <- 12 to 26) {
      Life3dStagingStrategy.stage(DeadCell, neighborsWithLiveCells(x)) should be (DeadCell)
    }
  }


  def neighborsWithLiveCells(count : Int) : Seq[Cell] = {
    val liveCells : Seq[Cell] = for(x <- 0 until count) yield LiveCell
    val deadCells : Seq[Cell] = for(x <- 0 until (8-count)) yield DeadCell
    liveCells ++ deadCells
  }

}
