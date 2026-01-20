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
  private var fpsLimit: Int = 60  // 0 = unlimited
  private var minFrameTime: Double = 1000.0 / 60  // milliseconds per frame

  // Cached rendering resources
  private var cachedImageData: dom.ImageData = _
  private var cachedPixels32: Uint32Array = _
  private var cachedCtx: dom.CanvasRenderingContext2D = _
  private var gridWidth: Int = 0
  private var gridHeight: Int = 0

  // Fast path: typed arrays for game state (bypasses Scala collections)
  private var grid: Uint8Array = _
  private var nextGrid: Uint8Array = _
  private var useFastPath: Boolean = true

  // 3D grid state
  private var grid3d: Uint8Array = _
  private var nextGrid3d: Uint8Array = _
  private var numLayers: Int = 0

  // 3D Life rules
  // Format: (birthMin, birthMax, surviveMin, surviveMax, useVonNeumann)
  private var currentRule: String = "4555"
  // 3D Life rules: (birthMin, birthMax, surviveMin, surviveMax, useVonNeumann)
  // With 26 neighbors (Moore), average neighbor count at 30% density is ~7.8
  // With 6 neighbors (Von Neumann), average is ~1.8
  private val rules: Map[String, (Int, Int, Int, Int, Boolean)] = Map(
    "original" -> (9, 11, 6, 11, false),   // B9-11/S6-11 - Original 3D Life
    "4555" -> (5, 5, 4, 5, false),         // B5/S4,5 - Most life-like, stable structures
    "5766" -> (6, 7, 5, 6, false),         // B6,7/S5,6 - Slow growth, interesting patterns
    "pyroclastic" -> (4, 5, 5, 5, false),  // B4-5/S5 - Explosive bursts, unstable
    "crystal" -> (6, 6, 5, 7, false),      // B6/S5-7 - Slow crystalline growth, stable
    "vonneumann" -> (1, 2, 2, 4, true)     // B1-2/S2-4 - Von Neumann 6-neighbor, sparse patterns
  )

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
    running = false
    frameCount = 0
    lastFpsUpdate = window.performance.now()
    gridWidth = height  // Note: width/height are swapped in the data model
    gridHeight = width

    // Initialize fast path typed arrays
    val size = gridWidth * gridHeight
    grid = new Uint8Array(size)
    nextGrid = new Uint8Array(size)

    // Random initialization
    var i = 0
    while (i < size) {
      grid(i) = if (js.Math.random() > 0.9) 1 else 0
      i += 1
    }

    initRenderCache()
    renderFromTypedArray()
  }

  /**
   * Fast advance using typed arrays - no Scala object allocation.
   */
  private def advanceFast(): Unit = {
    var row = 0
    while (row < gridWidth) {
      var col = 0
      while (col < gridHeight) {
        val idx = row * gridHeight + col
        val neighbors = countNeighbors(row, col)
        val alive = grid(idx) == 1

        nextGrid(idx) = if (alive) {
          if (neighbors == 2 || neighbors == 3) 1 else 0
        } else {
          if (neighbors == 3) 1 else 0
        }
        col += 1
      }
      row += 1
    }

    // Swap grids
    val temp = grid
    grid = nextGrid
    nextGrid = temp
  }

  private def countNeighbors(row: Int, col: Int): Int = {
    var count = 0
    var dr = -1
    while (dr <= 1) {
      var dc = -1
      while (dc <= 1) {
        if (!(dr == 0 && dc == 0)) {
          val r = row + dr
          val c = col + dc
          if (r >= 0 && r < gridWidth && c >= 0 && c < gridHeight) {
            count += grid(r * gridHeight + c)
          }
        }
        dc += 1
      }
      dr += 1
    }
    count
  }

  private def renderFromTypedArray(): Unit = {
    if (cachedCtx == null || cachedPixels32 == null) return

    val canvasWidth = cachedImageData.width
    var row = 0
    while (row < gridWidth) {
      val rowOffset = row * canvasWidth
      var col = 0
      while (col < gridHeight) {
        val alive = grid(row * gridHeight + col) == 1
        cachedPixels32(rowOffset + col) = if (alive) ALIVE_COLOR else DEAD_COLOR
        col += 1
      }
      row += 1
    }
    cachedCtx.putImageData(cachedImageData, 0, 0)
  }

  // ============ 3D Game of Life ============

  /**
   * Set the 3D Life rule.
   * Available rules: "original", "4555", "5766", "pyroclastic", "crystal", "vonneumann"
   */
  @JSExport
  def setRule(ruleName: String): Unit = {
    if (rules.contains(ruleName)) {
      currentRule = ruleName
      dom.console.log(s"3D rule set to: $ruleName")
    } else {
      dom.console.log(s"Unknown rule: $ruleName, keeping current: $currentRule")
    }
  }

  /**
   * Get available rule names.
   */
  @JSExport
  def getAvailableRules(): js.Array[String] = {
    js.Array("original", "4555", "5766", "pyroclastic", "crystal", "vonneumann")
  }

  /**
   * Get current rule name.
   */
  @JSExport
  def getCurrentRule(): String = currentRule

  /**
   * Initialize 3D game with typed arrays.
   * @param layers Number of z-layers
   * @param width Grid width (x)
   * @param height Grid height (y)
   */
  @JSExport
  def init3D(layers: Int, width: Int, height: Int): Unit = {
    numLayers = layers
    gridWidth = width
    gridHeight = height

    val size = layers * width * height
    grid3d = new Uint8Array(size)
    nextGrid3d = new Uint8Array(size)

    // Random initialization (30% alive for 3D)
    var i = 0
    while (i < size) {
      grid3d(i) = if (js.Math.random() > 0.7) 1 else 0
      i += 1
    }

    running = false
    frameCount = 0
    lastFpsUpdate = window.performance.now()
  }

  /**
   * Get 3D grid data for Three.js rendering.
   * Returns array of layer data, each layer is array of row data.
   */
  @JSExport
  def get3DState(): js.Array[js.Array[js.Array[Int]]] = {
    val result = new js.Array[js.Array[js.Array[Int]]](numLayers)

    var layer = 0
    while (layer < numLayers) {
      val layerData = new js.Array[js.Array[Int]](gridWidth)
      var row = 0
      while (row < gridWidth) {
        val rowData = new js.Array[Int](gridHeight)
        var col = 0
        while (col < gridHeight) {
          rowData(col) = grid3d(layer * gridWidth * gridHeight + row * gridHeight + col)
          col += 1
        }
        layerData(row) = rowData
        row += 1
      }
      result(layer) = layerData
      layer += 1
    }
    result
  }

  /**
   * Advance 3D simulation one step using the current rule.
   */
  @JSExport
  def advance3D(): Unit = {
    val (birthMin, birthMax, surviveMin, surviveMax, useVonNeumann) = rules(currentRule)
    val layerSize = gridWidth * gridHeight

    // Debug: log rule application on first frame after rule change
    if (frameCount == 0) {
      dom.console.log(s"Applying rule: $currentRule - B$birthMin-$birthMax/S$surviveMin-$surviveMax, VonNeumann=$useVonNeumann")
    }

    var layer = 0
    while (layer < numLayers) {
      var row = 0
      while (row < gridWidth) {
        var col = 0
        while (col < gridHeight) {
          val idx = layer * layerSize + row * gridHeight + col
          val neighbors = if (useVonNeumann) {
            countNeighborsVonNeumann(layer, row, col)
          } else {
            countNeighborsMoore(layer, row, col)
          }
          val alive = grid3d(idx) == 1

          nextGrid3d(idx) = if (alive) {
            if (neighbors >= surviveMin && neighbors <= surviveMax) 1 else 0
          } else {
            if (neighbors >= birthMin && neighbors <= birthMax) 1 else 0
          }
          col += 1
        }
        row += 1
      }
      layer += 1
    }

    // Swap grids
    val temp = grid3d
    grid3d = nextGrid3d
    nextGrid3d = temp
  }

  /**
   * Count neighbors using Moore neighborhood (26 neighbors).
   */
  private def countNeighborsMoore(layer: Int, row: Int, col: Int): Int = {
    val layerSize = gridWidth * gridHeight
    var count = 0

    var dz = -1
    while (dz <= 1) {
      var dr = -1
      while (dr <= 1) {
        var dc = -1
        while (dc <= 1) {
          if (!(dz == 0 && dr == 0 && dc == 0)) {
            val z = layer + dz
            val r = row + dr
            val c = col + dc
            if (z >= 0 && z < numLayers && r >= 0 && r < gridWidth && c >= 0 && c < gridHeight) {
              count += grid3d(z * layerSize + r * gridHeight + c)
            }
          }
          dc += 1
        }
        dr += 1
      }
      dz += 1
    }
    count
  }

  /**
   * Count neighbors using Von Neumann neighborhood (6 face-adjacent neighbors).
   */
  private def countNeighborsVonNeumann(layer: Int, row: Int, col: Int): Int = {
    val layerSize = gridWidth * gridHeight
    var count = 0

    // Check 6 face-adjacent neighbors: up, down, left, right, front, back
    if (layer > 0) count += grid3d((layer - 1) * layerSize + row * gridHeight + col)
    if (layer < numLayers - 1) count += grid3d((layer + 1) * layerSize + row * gridHeight + col)
    if (row > 0) count += grid3d(layer * layerSize + (row - 1) * gridHeight + col)
    if (row < gridWidth - 1) count += grid3d(layer * layerSize + (row + 1) * gridHeight + col)
    if (col > 0) count += grid3d(layer * layerSize + row * gridHeight + (col - 1))
    if (col < gridHeight - 1) count += grid3d(layer * layerSize + row * gridHeight + (col + 1))

    count
  }

  /**
   * Start 3D animation loop.
   * @param drawCallback JavaScript function to call with 3D state each frame
   */
  @JSExport
  def start3DAnimation(drawCallback: js.Function1[js.Array[js.Array[js.Array[Int]]], Unit]): Unit = {
    if (running) return
    running = true
    useOptimizedRendering = true
    lastFrameTime = window.performance.now()
    lastFpsUpdate = lastFrameTime
    frameCount = 0

    def animate3D(timestamp: Double): Unit = {
      if (!running) return

      // FPS limiting - skip frame if not enough time has passed
      val timeSinceLastFrame = timestamp - lastFrameTime
      if (fpsLimit > 0 && timeSinceLastFrame < minFrameTime) {
        animationFrameId = window.requestAnimationFrame(animate3D _)
        return
      }

      val t0 = window.performance.now()
      advance3D()
      val t1 = window.performance.now()
      val state = get3DState()
      drawCallback(state)
      val t2 = window.performance.now()

      // Log timing every second
      if (frameCount == 0) {
        dom.console.log(s"3D advance: ${t1-t0}ms, render: ${t2-t1}ms")
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
      animationFrameId = window.requestAnimationFrame(animate3D _)
    }

    animationFrameId = window.requestAnimationFrame(animate3D _)
  }

  /**
   * Single step for 3D.
   */
  @JSExport
  def step3D(): js.Array[js.Array[js.Array[Int]]] = {
    advance3D()
    get3DState()
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

      // FPS limiting - skip frame if not enough time has passed
      val timeSinceLastFrame = timestamp - lastFrameTime
      if (fpsLimit > 0 && timeSinceLastFrame < minFrameTime) {
        animationFrameId = window.requestAnimationFrame(animateOptimized _)
        return
      }

      val t0 = window.performance.now()
      advanceFast()
      val t1 = window.performance.now()
      renderFromTypedArray()
      val t2 = window.performance.now()

      // Log timing every second
      if (frameCount == 0) {
        dom.console.log(s"advance: ${t1-t0}ms, render: ${t2-t1}ms")
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
    if (grid != null) {
      advanceFast()
      renderFromTypedArray()
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

  /**
   * Set FPS limit (0 = unlimited).
   */
  @JSExport
  def setFpsLimit(limit: Int): Unit = {
    fpsLimit = limit
    minFrameTime = if (limit > 0) 1000.0 / limit else 0
  }

  /**
   * Get current FPS limit.
   */
  @JSExport
  def getFpsLimit(): Int = fpsLimit

  private var useOptimizedRendering: Boolean = false

  private def updateFpsDisplay(): Unit = {
    val fpsElement = dom.document.getElementById("fpsCounter")
    if (fpsElement != null) {
      fpsElement.textContent = s"FPS: $currentFps"
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
