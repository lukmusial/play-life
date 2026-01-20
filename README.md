# Play-Life: Game of Life in Scala

A high-performance implementation of Conway's Game of Life with 2D and 3D visualizations. All game logic runs client-side via Scala.js for 60+ FPS performance.

## Features

- **2D Simulation**: Classic Conway's Game of Life at 60+ FPS
- **3D Simulation**: Three-dimensional cellular automata with WebGL rendering
- **Multiple 3D Rules**: 4555, 5766, Pyroclastic, Crystal, Original, Von Neumann
- **Pure Client-Side**: All computation in browser - server only serves static pages
- **Interactive Controls**: Mouse/touch rotation, zoom, FPS limiting

## Quick Start

```bash
# Build Scala.js client
sbt client/fastLinkJS

# Run server
sbt server/run

# Open http://localhost:9000
```

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                            Browser                                   │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                     Scala.js (GameClient)                       │ │
│  │  ┌─────────────────┐    ┌─────────────────┐                    │ │
│  │  │  2D Simulation  │    │  3D Simulation  │                    │ │
│  │  │  ─────────────  │    │  ─────────────  │                    │ │
│  │  │  Uint8Array     │    │  Uint8Array     │                    │ │
│  │  │  grid storage   │    │  3D grid        │                    │ │
│  │  │                 │    │                 │                    │ │
│  │  │  Conway B3/S23  │    │  Multiple rules │                    │ │
│  │  └────────┬────────┘    └────────┬────────┘                    │ │
│  └───────────┼──────────────────────┼─────────────────────────────┘ │
│              │                      │                                │
│              ▼                      ▼                                │
│  ┌─────────────────┐    ┌─────────────────────┐                     │
│  │  HTML5 Canvas   │    │  Three.js / WebGL   │                     │
│  │  32-bit pixels  │    │  Particle system    │                     │
│  └─────────────────┘    └─────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   Play Framework Server (Static Only)                │
│  ┌─────────────────────────┐  ┌─────────────────────────────────┐  │
│  │    MainController       │  │    ThreedController             │  │
│  │    ──────────────       │  │    ────────────────             │  │
│  │    GET /  (landing)     │  │    GET /3d                      │  │
│  │    GET /2d              │  │    GET /threed                  │  │
│  └─────────────────────────┘  └─────────────────────────────────┘  │
│                                                                      │
│  Static Assets: HTML, CSS, JavaScript, Scala.js output              │
└─────────────────────────────────────────────────────────────────────┘
```

### Code Organization

```
┌──────────────────────────────────────────────────────────────────┐
│                        SBT Multi-Project                          │
├──────────────────┬─────────────────────┬─────────────────────────┤
│     shared/      │      client/        │        server/          │
│   (JVM + JS)     │    (Scala.js)       │   (Static pages only)   │
├──────────────────┼─────────────────────┼─────────────────────────┤
│                  │                     │                         │
│  Cell            │  GameClient         │  Controllers            │
│  ├─ LiveCell     │  ├─ initOptimized() │  ├─ MainController      │
│  └─ DeadCell     │  ├─ startOptimized()│  └─ ThreedController    │
│                  │  ├─ init3D()        │                         │
│  Canvas          │  ├─ start3DAnimation│  Views                  │
│  ├─ Vector2d     │  ├─ setRule()       │  ├─ landing.html        │
│  ├─ Random       │  ├─ setFpsLimit()   │  ├─ main.html (2D)      │
│  └─ String       │  └─ stopAnimation() │  └─ threed.html (3D)    │
│                  │                     │                         │
│  GameState       │                     │                         │
│  └─ advance()    │                     │                         │
│                  │                     │                         │
│  StagingStrategy │                     │                         │
│  └─ Life2d       │                     │                         │
└──────────────────┴─────────────────────┴─────────────────────────┘
```

### Data Flow

```
Page Load:
┌────────┐    HTML + JS      ┌────────┐
│ Server │ ─────────────────►│Browser │
└────────┘                   └───┬────┘
     │                           │
     │  No further communication │
     ▼                           ▼
┌────────┐               ┌──────────────────────────────┐
│ Done   │               │  GameClient runs locally     │
└────────┘               │  ──────────────────────────  │
                         │  • Initialize grid           │
                         │  • Run simulation loop       │
                         │  • Render to canvas/WebGL    │
                         │  • Handle user input         │
                         └──────────────────────────────┘

2D Animation Loop:
┌──────────────────────────┐
│  requestAnimationFrame   │◄──────┐
│  ────────────────────────│       │
│  1. advanceFast()        │       │
│  2. renderFromTypedArray │       │
│  3. updateFpsDisplay     │───────┘
└──────────────────────────┘

3D Animation Loop:
┌──────────────────────────┐
│  requestAnimationFrame   │◄──────┐
│  ────────────────────────│       │
│  1. advance3D()          │       │
│  2. get3DState()         │       │
│  3. drawRaw() → Three.js │───────┘
└──────────────────────────┘
```

### 3D Rules

| Rule | Birth | Survive | Neighbors | Characteristics |
|------|-------|---------|-----------|-----------------|
| 4555 | 5 | 4-5 | 26 (Moore) | Life-like, stable structures |
| 5766 | 6-7 | 5-6 | 26 (Moore) | Slow growth, interesting patterns |
| Pyroclastic | 4-5 | 5 | 26 (Moore) | Explosive bursts, unstable |
| Crystal | 6 | 5-7 | 26 (Moore) | Slow crystalline growth |
| Original | 9-11 | 6-11 | 26 (Moore) | Original 3D Life |
| Von Neumann | 1-2 | 2-4 | 6 (faces) | Sparse patterns |

## Tech Stack

- **Scala 2.13.16** - Core language
- **Play Framework 3.0.6** - Static page server
- **Scala.js 1.17.0** - Browser compilation
- **Three.js** - 3D WebGL rendering
- **Bootstrap 5.3.3** - UI styling
- **SBT 1.10.6** - Build tool

## Development

```bash
# Run tests
sbt test

# Compile everything
sbt compile

# Production Scala.js build
sbt client/fullLinkJS
```

## Other Implementations

- http://rosettacode.org/wiki/Conway's_Game_of_Life/Scala
- http://www.luigip.com/?p=133
