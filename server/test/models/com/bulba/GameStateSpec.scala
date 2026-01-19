package models.com.bulba

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import models.com.bulba.canvas.StringCanvas


class GameStateSpec extends AnyFlatSpec with Matchers {
  val entry1Canvas = """
    0000
    0010
    0110
    0000
                     """.stripMargin
  val result1Canvas = """
    0000
    0110
    0110
    0000
                      """.stripMargin

  val entry2Canvas = """
    0000
    0010
    0100
    0000
                     """.stripMargin
  val result2Canvas = """
    0000
    0000
    0000
    0000
                     """.stripMargin

  "GameState with String Canvas"  should "use the cell rules to advance a canvas (generally)" in {
    val state  = new GameState(StringCanvas(entry1Canvas))
    state.advance()
    val canvas = state.canvas
    canvas should not be StringCanvas(entry1Canvas)
  }

  "GameState with String Canvas"  should "use the cell rules to advance a canvas (make cell alive)" in {
    val state = new GameState(StringCanvas(entry1Canvas))
    state.advance()
    val canvas = state.canvas
    val resultCanvas = StringCanvas(result1Canvas)
    canvas should be (resultCanvas)
  }

  "GameState with String Canvas"  should "use the cell rules to advance a canvas (kill starved cells)" in {
    val state = new GameState(StringCanvas(entry2Canvas))
    state.advance()
    val canvas = state.canvas
    val resultCanvas = StringCanvas(result2Canvas)
    canvas should be (resultCanvas)
    canvas should not be StringCanvas(entry2Canvas)
  }

}
