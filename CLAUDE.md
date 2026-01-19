# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Play-Life is a Scala implementation of Conway's Game of Life using Play Framework 3.0 with both 2D and 3D visualization capabilities. It uses HTML5 Canvas for 2D rendering and Three.js for 3D visualization.

## Build and Run Commands

```bash
# Run the web application (serves at http://localhost:9000)
sbt run

# Run tests
sbt test

# Run a single test class
sbt "testOnly test.LifeControllerSpec"

# Compile without running
sbt compile

# Interactive SBT shell
sbt
```

## URLs

- 2D visualization: http://localhost:9000
- 3D visualization: http://localhost:9000/threed

## Architecture

### Core Domain Model (`app/models/com/bulba/`)

The game logic follows a functional approach with immutable cells and canvas types:

- **Cell** (`Cell.scala`): Sealed trait with `LiveCell` and `DeadCell` case objects. Cells delegate state transitions to a `StagingStrategy`.

- **Canvas** (`canvas/Canvas.scala`): Trait representing a 2D grid of cells. Implementations include:
  - `Vector2dCanvas`, `RandomCanvas`, `StringCanvas` for 2D
  - `Vector3dCanvas`, `Random3dCanvas`, `Finite3dCanvas` for 3D layers

- **StagingStrategy** (`stagingstrategy/`): Encapsulates the rules for cell evolution:
  - `Life2dStagingStrategy`: Standard Conway rules (B3/S23)
  - `Life3dStagingStrategy`: 3D variant (B9-11/S6-11)

- **GameState** / **Game3DState**: Mutable wrappers that hold a canvas/universe and provide `advance()` to compute the next generation.

- **Universe** / **Layers**: 3D game management. `Universe` contains `Layers`, which is a sequence of `Canvas` objects representing vertical slices. Each 3D canvas knows its layer index and can access neighbors via the `Layers` reference.

### Type Aliases (`package.scala`)

```scala
type VC = Vector[Cell]
type VVC = Vector[Vector[Cell]]
```

### Controllers (`app/controllers/`)

- **LifeController**: Manages 2D game state per session using Guava cache. Endpoints: `GET /life`, `POST /reset/height/:height/width/:width`
- **ThreedController**: Manages 3D game state. Endpoints: `GET /threed/life`, `POST /threed/reset/layers/:layers/height/:height/width/:width`
- **MainController**: Serves the main index page (Java)

State is stored per-session using session cookies as keys into an in-memory Guava cache with 1-hour expiration.

### Frontend (`app/assets/javascripts/`)

- `index.js` / `draw.js`: 2D canvas rendering and animation loop
- `3dindex.js` / `3ddraw.js`: Three.js-based 3D rendering

## Tech Stack

- Scala 2.13.16
- Play Framework 3.0.6
- SBT 1.10.6
- ScalaTest 3.2.19 for testing
- Three.js for 3D visualization
- Guava 33.4.0-jre for caching
- Bootstrap 5.3.3 via WebJars

## Development Rules

**MANDATORY: Follow these rules when making changes:**

1. **Run tests after every code change**: After modifying any code, always run `sbt test` to ensure all tests pass before proceeding.

2. **Commit after each successful phase**: When completing a logical unit of work (e.g., a feature, bugfix, or upgrade phase), commit changes to git with a descriptive message.
