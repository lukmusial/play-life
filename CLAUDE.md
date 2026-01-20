# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Play-Life is a Scala implementation of Conway's Game of Life with both 2D and 3D visualizations. All game computation runs client-side via Scala.js for 60+ FPS performance. The Play Framework server only serves static HTML pages and assets.

## Build and Run Commands

```bash
# Compile Scala.js client (required before running server)
sbt client/fastLinkJS

# Run the Play server (serves at http://localhost:9000)
sbt server/run

# Run all tests
sbt test

# Run a single test class
sbt "server/testOnly test.UISpec"

# Compile everything
sbt compile

# Full optimized Scala.js build (for production)
sbt client/fullLinkJS
```

## URLs

- Landing page: http://localhost:9000/
- 2D visualization: http://localhost:9000/2d
- 3D visualization: http://localhost:9000/3d

## Project Structure

```
play-life/
├── shared/                 # Cross-compiled code (JVM + JS)
│   └── src/main/scala/
│       └── models/com/bulba/
│           ├── Cell.scala           # LiveCell/DeadCell
│           ├── GameState.scala      # Mutable game wrapper
│           ├── canvas/              # 2D canvas implementations
│           └── stagingstrategy/     # Cell evolution rules
├── client/                 # Scala.js browser client
│   └── src/main/scala/
│       └── client/
│           └── GameClient.scala     # Browser game engine (2D + 3D)
├── server/                 # Play Framework server (static pages only)
│   ├── app/
│   │   ├── controllers/
│   │   │   ├── MainController.scala   # Landing + 2D pages
│   │   │   └── ThreedController.scala # 3D page
│   │   └── views/
│   │       ├── landing.scala.html     # Home page
│   │       ├── main.scala.html        # 2D view
│   │       └── threed.scala.html      # 3D view
│   ├── conf/
│   │   ├── routes                     # URL routing
│   │   └── application.conf
│   └── test/
├── public/                 # Static assets
│   ├── javascripts/
│   │   ├── scalajs/                   # Compiled Scala.js output
│   │   ├── index.js / draw.js         # 2D UI controls
│   │   ├── 3dindex.js / 3ddraw.js     # 3D UI + Three.js rendering
│   │   └── three.js                   # Three.js library
│   └── stylesheets/
└── build.sbt               # Multi-project SBT build
```

## Architecture

The server serves static pages only. All game logic runs in the browser via Scala.js.

### GameClient (Scala.js - `client/`)

The browser game engine with two modes:

**2D Mode:**
- `initOptimized(w, h)` - Initialize 2D grid with Uint8Array
- `startOptimized()` - Run animation with direct canvas rendering
- `stepOptimized()` - Single step advance
- Uses 32-bit pixel writes for maximum performance

**3D Mode:**
- `init3D(layers, w, h)` - Initialize 3D grid
- `start3DAnimation(callback)` - Run with Three.js render callback
- `step3D()` - Single step advance
- `setRule(name)` - Change rule set (4555, 5766, pyroclastic, crystal, original, vonneumann)

**Common:**
- `setFpsLimit(fps)` - Limit frame rate (0 = unlimited)
- `stopAnimation()` - Stop running animation
- `isRunning()` - Check animation state
- `getFps()` - Get current FPS

### Shared Models (`shared/`)

Cross-compiled code used by GameClient:
- `Cell` - Sealed trait: `LiveCell` | `DeadCell`
- `Canvas` - 2D grid trait with neighbor access
- `StagingStrategy` - Rules for cell evolution (B3/S23 for 2D)
- `GameState` - Mutable wrapper with `advance()`

### Type Aliases

```scala
type VC = Vector[Cell]
type VVC = Vector[Vector[Cell]]
```

## Tech Stack

- Scala 2.13.16
- Play Framework 3.0.6
- Scala.js 1.17.0
- SBT 1.10.6
- ScalaTest 3.2.19
- Three.js for 3D visualization
- Bootstrap 5.3.3 via WebJars

## Development Rules

**MANDATORY: Follow these rules when making changes:**

1. **Run tests after every code change**: After modifying any code, always run `sbt test` to ensure all tests pass before proceeding.

2. **Recompile Scala.js after client changes**: After modifying `client/` or `shared/` code, run `sbt client/fastLinkJS` to regenerate the JavaScript.

3. **Commit after each successful phase**: When completing a logical unit of work, commit changes to git with a descriptive message.
