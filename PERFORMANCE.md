# Performance Analysis and Upgrade Plan

## Current Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           BROWSER                                    │
├─────────────────────────────────────────────────────────────────────┤
│  index.js                    draw.js                                │
│  ┌─────────────┐            ┌─────────────────────────────────────┐ │
│  │ refresh()   │───AJAX────▶│ draw(arrHex)                       │ │
│  │ setTimeout  │            │ - getImageData()                   │ │
│  │ (50ms loop) │◀───JSON────│ - decode hex → binary per pixel    │ │
│  └─────────────┘            │ - putImageData()                   │ │
│                             └─────────────────────────────────────┘ │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ HTTP Request/Response
                                   │ (~10-50ms latency)
┌──────────────────────────────────▼──────────────────────────────────┐
│                           SERVER (Play)                              │
├─────────────────────────────────────────────────────────────────────┤
│  LifeController              GameState            Canvas             │
│  ┌─────────────┐            ┌──────────┐        ┌──────────────────┐│
│  │ getState()  │───────────▶│ advance()│───────▶│ stage()          ││
│  │             │            │          │        │ - compute cells  ││
│  │             │◀───────────│ toHex()  │◀───────│ - new immutable  ││
│  └─────────────┘            └──────────┘        │   data structure ││
│                                                 └──────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

## Identified Bottlenecks

### 1. Network Round-Trip (CRITICAL - ~70% of frame time)
- **Impact**: Every frame requires full HTTP request/response cycle
- **Latency**: 10-50ms minimum per frame
- **Result**: Maximum theoretical FPS = 1000ms / 50ms = **~20 FPS**

### 2. Data Serialization (HIGH - ~15% of frame time)
- **Server**: `toHex()` converts cell grid to hex strings using `.par` parallelism
- **Transfer**: JSON encoding of hex string array
- **Client**: Decode hex strings back to binary with nested string operations

### 3. Canvas Rendering (MEDIUM - ~10% of frame time)
- `getImageData()` / `putImageData()` are relatively slow operations
- Pixel-by-pixel iteration with function calls
- Fade effect requires reading existing pixel values

### 4. Game Logic Computation (LOW - ~5% of frame time)
- `stage()` creates new immutable Vector structures
- Already optimized with parallel collections for large grids
- `changedCells` optimization reduces unnecessary recalculation

## Performance Metrics (Current)

| Grid Size | Expected FPS | Bottleneck |
|-----------|--------------|------------|
| 100x100   | 15-20        | Network    |
| 200x200   | 12-18        | Network    |
| 400x400   | 8-15         | Network + Serialization |
| 800x800   | 3-8          | All factors |

---

## Upgrade Plan

### Phase 1: Client-Side Computation (HIGHEST IMPACT)
**Expected improvement: 5-10x FPS increase**

Move the Game of Life algorithm to JavaScript. Server only provides initial state.

```javascript
// New architecture
class GameOfLife {
    constructor(width, height) {
        this.width = width;
        this.height = height;
        this.grid = new Uint8Array(width * height);
        this.nextGrid = new Uint8Array(width * height);
    }

    step() {
        // Compute next generation entirely in browser
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                const neighbors = this.countNeighbors(x, y);
                const idx = y * this.width + x;
                const alive = this.grid[idx];
                this.nextGrid[idx] = (alive && (neighbors === 2 || neighbors === 3))
                                   || (!alive && neighbors === 3) ? 1 : 0;
            }
        }
        [this.grid, this.nextGrid] = [this.nextGrid, this.grid];
    }
}
```

**Benefits:**
- Eliminates network latency per frame
- No serialization overhead
- Can use `requestAnimationFrame` for 60 FPS sync

### Phase 2: WebWorker for Computation
**Expected improvement: +20-30% FPS, smoother animation**

Move simulation to background thread to prevent UI blocking.

```javascript
// Main thread
const worker = new Worker('life-worker.js');
worker.postMessage({ type: 'init', width: 400, height: 400 });
worker.onmessage = (e) => render(e.data.grid);

// Worker thread (life-worker.js)
self.onmessage = function(e) {
    if (e.data.type === 'step') {
        game.step();
        self.postMessage({ grid: game.grid });
    }
};
```

### Phase 3: Rendering Optimizations
**Expected improvement: +10-20% FPS**

1. **Use requestAnimationFrame**
```javascript
function animate() {
    game.step();
    render();
    requestAnimationFrame(animate);
}
```

2. **Direct pixel buffer manipulation**
```javascript
const imageData = ctx.createImageData(width, height);
const data = new Uint32Array(imageData.data.buffer);
// Write 4 bytes at once instead of 1
data[idx] = alive ? 0xFF77CAE6 : 0xFF000000;
```

3. **Delta rendering** - Only update changed pixels
```javascript
for (let i = 0; i < grid.length; i++) {
    if (grid[i] !== prevGrid[i]) {
        // Only update this pixel
    }
}
```

### Phase 4: WebGL Acceleration (Optional)
**Expected improvement: 10-100x for very large grids**

Use GPU shaders for both computation and rendering.

```glsl
// Fragment shader for Game of Life computation
uniform sampler2D uState;
void main() {
    int neighbors = countNeighbors(gl_FragCoord.xy);
    bool alive = texture2D(uState, uv).r > 0.5;
    gl_FragColor = vec4((alive && (neighbors == 2 || neighbors == 3))
                      || (!alive && neighbors == 3) ? 1.0 : 0.0);
}
```

---

## Implementation Priority

| Phase | Effort | Impact | Priority |
|-------|--------|--------|----------|
| 1. Client-side computation | Medium | Very High | **P0** |
| 2. WebWorker | Low | Medium | P1 |
| 3. Rendering optimizations | Low | Medium | P1 |
| 4. WebGL | High | High (large grids) | P2 |

## Expected Results

| Phase | Grid 400x400 FPS | Notes |
|-------|------------------|-------|
| Current | 8-15 | Network bottleneck |
| After Phase 1 | 50-60 | Browser-limited |
| After Phase 2 | 60+ | Consistent 60 |
| After Phase 3 | 60+ | Smoother, lower CPU |
| After Phase 4 | 60+ | Supports 1000x1000+ |

## Migration Path

1. **Keep server endpoints** for initial state and potential multiplayer
2. **Add client-side mode** with feature flag
3. **Gradually deprecate** per-frame server calls
4. **Server becomes** initial state provider + optional state sync

## Files to Modify

### Phase 1 (Client-side computation)
- `public/javascripts/game-of-life.js` (NEW) - Game logic
- `public/javascripts/index.js` - Use local computation
- `app/controllers/LifeController.scala` - Add initial state endpoint
- `app/views/index.scala.html` - Include new script

### Phase 2 (WebWorker)
- `public/javascripts/life-worker.js` (NEW) - Worker script
- `public/javascripts/index.js` - Worker communication

### Phase 3 (Rendering)
- `public/javascripts/draw.js` - Optimized rendering
