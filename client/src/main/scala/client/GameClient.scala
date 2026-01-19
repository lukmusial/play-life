package client

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom
import org.scalajs.dom.window

import models.com.bulba._
import models.com.bulba.canvas.{RandomCanvas, Vector2dCanvas, Canvas}

/**
 * Scala.js client for Game of Life.
 * Runs the simulation entirely in the browser, eliminating network latency.
 */
@JSExportTopLevel("GameClient")
object GameClient {
  private var gameState: Option[GameState] = None
  private var running: Boolean = false
  private var lastFrameTime: Double = 0
  private var frameCount: Int = 0
  private var currentFps: Int = 0
  private var lastFpsUpdate: Double = 0
  private var animationFrameId: Int = 0

  /**
   * Initialize a new game with random cells.
   * @param width Grid width
   * @param height Grid height
   * @return Initial grid data as hex-encoded strings
   */
  @JSExport
  def init(width: Int, height: Int): js.Array[String] = {
    val canvas = RandomCanvas(width, height)
    gameState = Some(new GameState(canvas))
    running = false
    frameCount = 0
    lastFpsUpdate = window.performance.now()
    toJsArray(gameState.get.toHex)
  }

  /**
   * Advance the simulation by one step.
   * @return Updated grid data as hex-encoded strings
   */
  @JSExport
  def step(): js.Array[String] = {
    gameState match {
      case Some(gs) =>
        gs.advance()
        toJsArray(gs.toHex)
      case None =>
        js.Array[String]()
    }
  }

  /**
   * Get current grid dimensions.
   * @return Array of [height, width]
   */
  @JSExport
  def getDimensions(): js.Array[Int] = {
    gameState match {
      case Some(gs) =>
        js.Array(gs.canvas.gridHeight, gs.canvas.gridWidth)
      case None =>
        js.Array(0, 0)
    }
  }

  /**
   * Check if game is initialized.
   */
  @JSExport
  def isInitialized(): Boolean = gameState.isDefined

  /**
   * Start automatic animation loop using requestAnimationFrame.
   * @param drawCallback JavaScript function to call with grid data each frame
   */
  @JSExport
  def startAnimation(drawCallback: js.Function1[js.Array[String], Unit]): Unit = {
    if (running) return
    running = true
    lastFrameTime = window.performance.now()
    lastFpsUpdate = lastFrameTime
    frameCount = 0

    def animate(timestamp: Double): Unit = {
      if (!running) return

      // Advance simulation
      gameState.foreach { gs =>
        gs.advance()
        val data = toJsArray(gs.toHex)
        drawCallback(data)
      }

      // Update FPS counter
      frameCount += 1
      val elapsed = timestamp - lastFpsUpdate
      if (elapsed >= 1000) {
        currentFps = ((frameCount * 1000) / elapsed).toInt
        frameCount = 0
        lastFpsUpdate = timestamp
        updateFpsDisplay()
      }

      lastFrameTime = timestamp
      animationFrameId = window.requestAnimationFrame(animate _)
    }

    animationFrameId = window.requestAnimationFrame(animate _)
  }

  /**
   * Stop the animation loop.
   */
  @JSExport
  def stopAnimation(): Unit = {
    running = false
    if (animationFrameId != 0) {
      window.cancelAnimationFrame(animationFrameId)
      animationFrameId = 0
    }
  }

  /**
   * Check if animation is running.
   */
  @JSExport
  def isRunning(): Boolean = running

  /**
   * Get current FPS.
   */
  @JSExport
  def getFps(): Int = currentFps

  private def updateFpsDisplay(): Unit = {
    val fpsElement = dom.document.getElementById("fpsCounter")
    if (fpsElement != null) {
      fpsElement.textContent = s"FPS: $currentFps (Scala.js)"
      fpsElement.asInstanceOf[dom.html.Element].style.color =
        if (currentFps > 30) "#00ff00"
        else if (currentFps > 15) "#ffff00"
        else "#ff0000"
    }
  }

  private def toJsArray(seq: Seq[String]): js.Array[String] = {
    val arr = new js.Array[String](seq.length)
    for (i <- seq.indices) {
      arr(i) = seq(i)
    }
    arr
  }
}
