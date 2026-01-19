package client

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.typedarray._
import org.scalajs.dom
import org.scalajs.dom.window
import org.scalajs.dom.html

import models.com.bulba._
import models.com.bulba.canvas.{RandomCanvas, Vector2dCanvas, Canvas}

/**
 * Scala.js client for Game of Life.
 * Runs the simulation entirely in the browser, eliminating network latency.
 *
 * Optimized rendering path:
 * - Uses Uint8Array for raw cell data (no hex encoding)
 * - Uses Uint32Array for 32-bit pixel writes
 * - Caches ImageData to avoid allocations
 * - Direct canvas rendering in Scala.js
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

  // Cached rendering resources
  private var cachedImageData: dom.ImageData = _
  private var cachedPixels32: Uint32Array = _
  private var cachedCtx: dom.CanvasRenderingContext2D = _
  private var gridWidth: Int = 0
  private var gridHeight: Int = 0

  // Precomputed colors as 32-bit ABGR values (little-endian)
  private val ALIVE_COLOR: Int = 0xFFE6CA77  // #77CAE6 with alpha 255
  private val DEAD_COLOR: Int = 0xFF000000   // Black with alpha 255
  private val FADE_COLORS: Array[Int] = Array(
    0xB4E6CA77,  // alpha 180
    0x8CE6CA77,  // alpha 140
    0x64E6CA77,  // alpha 100
    0x32E6CA77   // alpha 50
  )

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
    gridWidth = width
    gridHeight = height
    initRenderCache()
    toJsArray(gameState.get.toHex)
  }

  /**
   * Initialize optimized rendering with direct canvas access.
   * Call this once after init() for maximum performance.
   */
  @JSExport
  def initOptimized(width: Int, height: Int): Unit = {
    val canvas = RandomCanvas(width, height)
    gameState = Some(new GameState(canvas))
    running = false
    frameCount = 0
    lastFpsUpdate = window.performance.now()
    gridWidth = width
    gridHeight = height
    initRenderCache()
    renderFull()  // Full render for initial display
  }

  private def initRenderCache(): Unit = {
    val canvasEl = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
    if (canvasEl != null) {
      cachedCtx = canvasEl.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      val w = canvasEl.width
      val h = canvasEl.height
      cachedImageData = cachedCtx.createImageData(w, h)
      // Create a Uint32Array view of the pixel data for fast 32-bit writes
      cachedPixels32 = new Uint32Array(cachedImageData.data.buffer)
      // Initialize to black
      for (i <- 0 until cachedPixels32.length) {
        cachedPixels32(i) = DEAD_COLOR
      }
    }
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
   * Start optimized animation loop with direct rendering.
   * This is the fastest mode - no hex encoding, no callback overhead.
   */
  @JSExport
  def startOptimized(): Unit = {
    if (running) return
    if (cachedCtx == null) initRenderCache()
    running = true
    useOptimizedRendering = true
    lastFrameTime = window.performance.now()
    lastFpsUpdate = lastFrameTime
    frameCount = 0

    def animateOptimized(timestamp: Double): Unit = {
      if (!running) return

      // Advance simulation and render directly
      gameState.foreach { gs =>
        val t0 = window.performance.now()
        gs.advance()
        val t1 = window.performance.now()
        renderFullFast()
        val t2 = window.performance.now()

        // Log timing every second
        if (frameCount == 0) {
          dom.console.log(s"advance: ${t1-t0}ms, render: ${t2-t1}ms")
        }
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
      animationFrameId = window.requestAnimationFrame(animateOptimized _)
    }

    animationFrameId = window.requestAnimationFrame(animateOptimized _)
  }

  // Track cells that are currently fading (need updates even if unchanged)
  private var fadingCells: scala.collection.mutable.Set[(Int, Int)] = scala.collection.mutable.Set.empty

  /**
   * Render directly to canvas using cached ImageData and 32-bit writes.
   * Uses delta rendering: only updates changed cells and fading cells.
   */
  private def renderDirect(): Unit = {
    if (cachedCtx == null || cachedPixels32 == null) return

    gameState.foreach { gs =>
      val canvasData = gs.canvas.canvas
      val canvasWidth = cachedImageData.width
      val changedCells = gs.canvas.changedCells

      // Process changed cells
      val newFading = scala.collection.mutable.Set.empty[(Int, Int)]

      changedCells.foreach { case (row, col) =>
        if (row >= 0 && row < canvasData.length && col >= 0 && col < canvasData(row).length) {
          val pixelIdx = row * canvasWidth + col
          val cell = canvasData(row)(col)

          if (cell.isAlive) {
            cachedPixels32(pixelIdx) = ALIVE_COLOR
          } else {
            // Cell just died - start fading
            cachedPixels32(pixelIdx) = FADE_COLORS(0)
            newFading += ((row, col))
          }
        }
      }

      // Process fading cells
      val stillFading = scala.collection.mutable.Set.empty[(Int, Int)]
      fadingCells.foreach { case (row, col) =>
        if (row >= 0 && row < canvasData.length && col >= 0 && col < canvasData(row).length) {
          val pixelIdx = row * canvasWidth + col
          val cell = canvasData(row)(col)

          if (cell.isAlive) {
            // Cell came back alive, no longer fading
            cachedPixels32(pixelIdx) = ALIVE_COLOR
          } else if (!changedCells.contains((row, col))) {
            // Continue fading
            val currentColor = cachedPixels32(pixelIdx)
            val newColor = currentColor match {
              case c if c == FADE_COLORS(0) => FADE_COLORS(1)
              case c if c == FADE_COLORS(1) => FADE_COLORS(2)
              case c if c == FADE_COLORS(2) => FADE_COLORS(3)
              case c if c == FADE_COLORS(3) => DEAD_COLOR
              case _ => DEAD_COLOR
            }
            cachedPixels32(pixelIdx) = newColor
            if (newColor != DEAD_COLOR) {
              stillFading += ((row, col))
            }
          }
        }
      }

      fadingCells = stillFading ++ newFading
      cachedCtx.putImageData(cachedImageData, 0, 0)
    }
  }

  /**
   * Full render (for initial display).
   * Renders all cells, not just changes.
   */
  private def renderFull(): Unit = {
    if (cachedCtx == null || cachedPixels32 == null) return

    gameState.foreach { gs =>
      val canvasData = gs.canvas.canvas
      val canvasWidth = cachedImageData.width

      var row = 0
      while (row < canvasData.length) {
        val rowData = canvasData(row)
        var col = 0
        while (col < rowData.length) {
          val pixelIdx = row * canvasWidth + col
          if (rowData(col).isAlive) {
            cachedPixels32(pixelIdx) = ALIVE_COLOR
          } else {
            cachedPixels32(pixelIdx) = DEAD_COLOR
          }
          col += 1
        }
        row += 1
      }

      fadingCells.clear()
      cachedCtx.putImageData(cachedImageData, 0, 0)
    }
  }

  /**
   * Fast full render optimized for animation.
   * No fade effect - just alive/dead colors.
   */
  private def renderFullFast(): Unit = {
    if (cachedCtx == null || cachedPixels32 == null) return

    gameState match {
      case Some(gs) =>
        val canvasData = gs.canvas.canvas
        val canvasWidth = cachedImageData.width
        val height = canvasData.length

        var row = 0
        while (row < height) {
          val rowData = canvasData(row)
          val width = rowData.length
          val rowOffset = row * canvasWidth
          var col = 0
          while (col < width) {
            val color = if (rowData(col) == LiveCell) ALIVE_COLOR else DEAD_COLOR
            cachedPixels32(rowOffset + col) = color
            col += 1
          }
          row += 1
        }

        cachedCtx.putImageData(cachedImageData, 0, 0)
      case None => ()
    }
  }

  /**
   * Step and render directly (for single-step mode).
   */
  @JSExport
  def stepOptimized(): Unit = {
    gameState.foreach { gs =>
      gs.advance()
      renderDirect()
    }
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

  private var useOptimizedRendering: Boolean = false

  private def updateFpsDisplay(): Unit = {
    val fpsElement = dom.document.getElementById("fpsCounter")
    if (fpsElement != null) {
      val mode = if (useOptimizedRendering) "Optimized" else "Scala.js"
      fpsElement.textContent = s"FPS: $currentFps ($mode)"
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
