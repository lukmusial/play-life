package models.com.bulba

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.stagingstrategy.Life2dStagingStrategy


class CellSpec extends AnyFlatSpec with Matchers {

  val strategy = Life2dStagingStrategy

  "Any live cell"  should "die if with fewer than two live neighbors" in {
    val withOneLiveNeighbor = neighborsWithLiveCells(1)
    LiveCell.stage(withOneLiveNeighbor, Life2dStagingStrategy) should be (DeadCell)
    val withNoLiveNeighbors = neighborsWithLiveCells(0)
    LiveCell.stage(withNoLiveNeighbors, Life2dStagingStrategy) should be (DeadCell)
  }
  it should "die if more than three live neighbors" in {
    val withFourLiveNeighbors = neighborsWithLiveCells(4)
    LiveCell.stage(withFourLiveNeighbors, Life2dStagingStrategy) should be (DeadCell)
  }
  it should "live if two or three neighbors" in {
    val withTwoLiveNeighbors = neighborsWithLiveCells(2)
    LiveCell.stage(withTwoLiveNeighbors, Life2dStagingStrategy) should be (LiveCell)
    val withThreeLiveNeighbors = neighborsWithLiveCells(3)
    LiveCell.stage(withThreeLiveNeighbors, Life2dStagingStrategy) should be (LiveCell)
  }
  "Any dead cell" should "come to life if three live neighbors" in {
    val withThreeLiveNeighbors = neighborsWithLiveCells(3)
    DeadCell.stage(withThreeLiveNeighbors, Life2dStagingStrategy) should be (LiveCell)
  }
  it should "remain dead otherwise" in {
    val withTwoLiveNeighbors = neighborsWithLiveCells(2)
    DeadCell.stage(withTwoLiveNeighbors, Life2dStagingStrategy) should be (DeadCell)
    val withOneLiveNeighbor = neighborsWithLiveCells(1)
    DeadCell.stage(withOneLiveNeighbor, Life2dStagingStrategy) should be (DeadCell)
    val withFourLiveNeighbors = neighborsWithLiveCells(4)
    DeadCell.stage(withFourLiveNeighbors, Life2dStagingStrategy) should be (DeadCell)
  }


  def neighborsWithLiveCells(count : Int) : Seq[Cell] = {
    val liveCells : Seq[Cell] = for(x <- 0 until count) yield LiveCell
    val deadCells : Seq[Cell] = for(x <- 0 until (8-count)) yield DeadCell
    liveCells ++ deadCells
  }

}
