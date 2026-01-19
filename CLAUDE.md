# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Play-Life is a Scala implementation of Conway's Game of Life using Play Framework 3.0 with both 2D and 3D visualization capabilities. It uses HTML5 Canvas for 2D rendering and Three.js for 3D visualization.

**Key Feature**: The 2D game logic runs entirely in the browser via Scala.js, eliminating network latency for 60 FPS performance.

## Project Structure

```
play-life/
├── shared/                 # Cross-compiled code (JVM + JS)
│   └── src/main/scala/
│       └── models/com/bulba/
│           ├── Cell.scala
│           ├── GameState.scala
│           ├── canvas/         # 2D canvas implementations
│           └── stagingstrategy/
├── client/                 # Scala.js browser client
│   └── src/main/scala/
│       └── client/
│           └── GameClient.scala
├── server/                 # Play Framework server
│   ├── app/
│   │   ├── controllers/
│   │   ├── models/com/bulba/   # 3D-specific code only
│   │   └── views/
│   ├── conf/
│   └── test/
├── public/                 # Static assets
│   ├── javascripts/
│   │   ├── scalajs/       # Compiled Scala.js output
│   │   ├── draw.js
│   │   └── index.js
│   └── stylesheets/
└── build.sbt              # Multi-project SBT build
```

## Build and Run Commands

```bash
# Compile Scala.js client (required before running server)
sbt client/fastLinkJS

# Run the Play server (serves at http://localhost:9000)
sbt server/run

# Run all tests
sbt server/test

# Run a single test class
sbt "server/testOnly test.UISpec"

# Compile everything
sbt compile

# Full optimized Scala.js build (for production)
sbt client/fullLinkJS

# Interactive SBT shell
sbt
```

## URLs

- 2D visualization: http://localhost:9000
- 3D visualization: http://localhost:9000/threed

## Architecture

### Shared Module (`shared/`)

Cross-compiled code that runs on both JVM and browser:

- **Cell** (`Cell.scala`): Sealed trait with `LiveCell` and `DeadCell` case objects
- **Canvas** (`canvas/Canvas.scala`): Trait representing a 2D grid of cells
  - `Vector2dCanvas`, `RandomCanvas`, `StringCanvas` for 2D
- **StagingStrategy**: Rules for cell evolution
  - `Life2dStagingStrategy`: Standard Conway rules (B3/S23)
  - `Life3dStagingStrategy`: 3D variant (B9-11/S6-11)
- **GameState**: Mutable wrapper for canvas with `advance()` method

### Client Module (`client/`)

Scala.js browser client that runs game logic locally:

- **GameClient**: Exported to JavaScript with methods:
  - `init(width, height)`: Initialize random grid
  - `step()`: Advance one generation
  - `startAnimation(callback)`: Run at 60fps
  - `stopAnimation()`: Stop animation
  - `getFps()`: Get current FPS

### Server Module (`server/`)

Play Framework server with 3D-specific code:

- **Controllers**: HTTP endpoints for reset/state
- **3D Models**: `Vector3dCanvas`, `Universe`, `Layers` (JVM-only, uses Futures)
- **Views**: HTML templates

### Type Aliases (`package.scala`)

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
- scalajs-dom 2.8.0
- Three.js for 3D visualization
- Guava 33.4.0-jre for caching
- Bootstrap 5.3.3 via WebJars

## Development Rules

**MANDATORY: Follow these rules when making changes:**

1. **Run tests after every code change**: After modifying any code, always run `sbt server/test` to ensure all tests pass before proceeding.

2. **Recompile Scala.js after client changes**: After modifying `client/` or `shared/` code, run `sbt client/fastLinkJS` to regenerate the JavaScript.

3. **Commit after each successful phase**: When completing a logical unit of work, commit changes to git with a descriptive message.
