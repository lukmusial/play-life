# Play-Life: Game of Life in Scala

A high-performance implementation of Conway's Game of Life with 2D and 3D visualizations, built with Scala, Play Framework, and Scala.js.

## Features

- **2D Simulation**: Classic Conway's Game of Life running at 60+ FPS
- **3D Simulation**: Three-dimensional cellular automata with WebGL rendering
- **Multiple 3D Rules**: 4555, 5766, Pyroclastic, Crystal, Original, Von Neumann
- **Client-Side Computation**: Game logic runs in browser via Scala.js
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
│                        Play Framework Server                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │
│  │ MainController  │  │ LifeController  │  │ ThreedController    │  │
│  │ ─────────────── │  │ ────────────── │  │ ─────────────────── │  │
│  │ GET /           │  │ GET /life      │  │ GET /3d             │  │
│  │ GET /2d         │  │ POST /reset    │  │ GET /threed/life    │  │
│  │ GET /3d         │  │                │  │ POST /threed/reset  │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────┘  │
│                                                                      │
│  Static Assets: /public/javascripts/scalajs/main.js                 │
└─────────────────────────────────────────────────────────────────────┘
```

### Code Organization

```
┌──────────────────────────────────────────────────────────────────┐
│                        SBT Multi-Project                          │
├──────────────────┬─────────────────────┬─────────────────────────┤
│     shared/      │      client/        │        server/          │
│   (JVM + JS)     │    (Scala.js)       │    (Play Framework)     │
├──────────────────┼─────────────────────┼─────────────────────────┤
│                  │                     │                         │
│  Cell            │  GameClient         │  Controllers            │
│  ├─ LiveCell     │  ├─ init()          │  ├─ MainController      │
│  └─ DeadCell     │  ├─ initOptimized() │  ├─ LifeController      │
│                  │  ├─ init3D()        │  └─ ThreedController    │
│  Canvas          │  ├─ step()          │                         │
│  ├─ Vector2d     │  ├─ startOptimized()│  3D Models              │
│  ├─ Random       │  ├─ start3DAnimation│  ├─ Universe            │
│  └─ String       │  ├─ setRule()       │  ├─ Layers              │
│                  │  └─ setFpsLimit()   │  └─ Vector3dCanvas      │
│  GameState       │                     │                         │
│  └─ advance()    │                     │  Views                  │
│                  │                     │  ├─ landing.html        │
│  StagingStrategy │                     │  ├─ main.html (2D)      │
│  ├─ Life2d       │                     │  └─ threed.html (3D)    │
│  └─ Life3d       │                     │                         │
└──────────────────┴─────────────────────┴─────────────────────────┘
```

### Data Flow

```
2D Mode (Client-Side Only):
┌────────┐    page load     ┌────────┐
│ Server │ ───────────────► │Browser │
└────────┘   HTML + JS      └───┬────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 ▼                 │
              │  ┌──────────────────────────┐    │
              │  │  GameClient.initOptimized│    │
              │  │  ────────────────────────│    │
              │  │  1. Create Uint8Array    │    │
              │  │  2. Random init (10%)    │    │
              │  └────────────┬─────────────┘    │
              │               │                  │
              │               ▼                  │
              │  ┌──────────────────────────┐    │
              │  │  Animation Loop (60fps)  │◄───┼───┐
              │  │  ────────────────────────│    │   │
              │  │  1. advanceFast()        │    │   │
              │  │  2. renderFromTypedArray │    │   │
              │  │  3. requestAnimationFrame│────┼───┘
              │  └──────────────────────────┘    │
              │          Browser (no server)    │
              └─────────────────────────────────┘

3D Mode (Client Compute + WebGL Render):
┌────────────────────────────────────────────────────────┐
│                      Browser                            │
│  ┌─────────────────┐      ┌─────────────────────────┐  │
│  │   GameClient    │      │      Three.js           │  │
│  │   ───────────   │      │      ─────────          │  │
│  │   init3D()      │      │                         │  │
│  │        │        │      │   ┌─────────────────┐   │  │
│  │        ▼        │      │   │ Particle System │   │  │
│  │   advance3D()───┼──────┼──►│ (WebGL Shaders) │   │  │
│  │        │        │ data │   └─────────────────┘   │  │
│  │        ▼        │      │           │             │  │
│  │   get3DState()  │      │           ▼             │  │
│  │                 │      │   ┌─────────────────┐   │  │
│  └─────────────────┘      │   │    Renderer     │   │  │
│                           │   └─────────────────┘   │  │
│                           └─────────────────────────┘  │
└────────────────────────────────────────────────────────┘
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
- **Play Framework 3.0.6** - Web server
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

## Other implementations

http://rosettacode.org/wiki/Conway's_Game_of_Life/Scala
http://www.luigip.com/?p=133
