# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Play-Life is a Scala implementation of Conway's Game of Life using Play Framework 3.0 with both 2D and 3D visualization capabilities. The 2D simulation runs entirely in the browser via Scala.js for 60+ FPS performance. The 3D simulation uses Three.js with WebGL particle rendering.

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
- Legacy 3D URL: http://localhost:9000/threed (redirects to /3d)

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
│           └── GameClient.scala     # Browser game engine
├── server/                 # Play Framework server
│   ├── app/
│   │   ├── controllers/
│   │   │   ├── MainController.scala   # Landing + 2D pages
│   │   │   ├── LifeController.scala   # 2D API endpoints
│   │   │   └── ThreedController.scala # 3D page + API
│   │   ├── models/com/bulba/          # 3D-specific models
│   │   │   ├── Universe.scala         # 3D world container
│   │   │   ├── Layers.scala           # Z-layer management
│   │   │   └── canvas/                # 3D canvas types
│   │   └── views/
│   │       ├── landing.scala.html     # Home page
│   │       ├── main.scala.html        # 2D view (index)
│   │       └── threed.scala.html      # 3D view
│   ├── conf/
│   │   ├── routes                     # URL routing
│   │   └── application.conf
│   └── test/
├── public/                 # Static assets
│   ├── javascripts/
│   │   ├── scalajs/                   # Compiled Scala.js output
│   │   ├── index.js / draw.js         # 2D rendering
│   │   ├── 3dindex.js / 3ddraw.js     # 3D rendering
│   │   └── three.js                   # Three.js library
│   └── stylesheets/
└── build.sbt               # Multi-project SBT build
```

## Architecture

### Rendering Modes

**2D Mode**: Pure client-side Scala.js
- GameClient runs simulation in browser using typed arrays (Uint8Array)
- Direct canvas rendering with 32-bit pixel writes
- No server communication after initial page load
- Achieves 60+ FPS

**3D Mode**: Client-side Scala.js + Three.js
- GameClient computes 3D cellular automata
- Three.js renders particles via WebGL shaders
- Multiple rule sets: 4555, 5766, Pyroclastic, Crystal, Original, Von Neumann
- Mouse/touch controls for rotation and zoom

### Key Components

**GameClient** (Scala.js - `client/`):
- `initOptimized(w, h)` - Initialize 2D with typed arrays
- `startOptimized()` - Run 2D animation at max FPS
- `init3D(layers, w, h)` - Initialize 3D grid
- `start3DAnimation(callback)` - Run 3D with render callback
- `setRule(name)` - Change 3D rule set
- `setFpsLimit(fps)` - Limit frame rate

**Shared Models** (`shared/`):
- `Cell` - Sealed trait: `LiveCell` | `DeadCell`
- `Canvas` - 2D grid trait with neighbor access
- `StagingStrategy` - Rules for cell evolution
- `GameState` - Mutable wrapper with `advance()`

**3D Models** (`server/app/models/`):
- `Universe` - Contains `Layers` of 3D canvases
- `Vector3dCanvas` - 3D grid with z-neighbor access
- `Life3dStagingStrategy` - 3D rules (26 or 6 neighbors)

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
- Guava 33.4.0-jre for session caching
- Bootstrap 5.3.3 via WebJars

## Development Rules

**MANDATORY: Follow these rules when making changes:**

1. **Run tests after every code change**: After modifying any code, always run `sbt test` to ensure all tests pass before proceeding.

2. **Recompile Scala.js after client changes**: After modifying `client/` or `shared/` code, run `sbt client/fastLinkJS` to regenerate the JavaScript.

3. **Commit after each successful phase**: When completing a logical unit of work, commit changes to git with a descriptive message.
