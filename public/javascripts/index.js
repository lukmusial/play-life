// Game of Life - Scala.js Client
// Uses client-side computation with optimized rendering for maximum performance

// Rendering modes:
// 1. "optimized" - Direct Scala.js rendering with Uint32Array (fastest)
// 2. "scalajs" - Scala.js with hex encoding + JS draw callback
// 3. "server" - Server-side computation with AJAX (legacy)
var renderMode = "optimized";

$(function() {
    $("#autoRefreshButton").click(function(event) {
        if (renderMode === "optimized") {
            if (!GameClient.isRunning()) {
                GameClient.startOptimized();
            } else {
                GameClient.stopAnimation();
            }
        } else if (renderMode === "scalajs") {
            if (!GameClient.isRunning()) {
                GameClient.startAnimation(function(data) {
                    draw(data);
                });
            } else {
                GameClient.stopAnimation();
            }
        } else {
            // Legacy server-side mode
            if (!window.doRefresh) {
                window.doRefresh = true;
                window.frameCount = 0;
                window.lastFpsUpdate = performance.now();
                legacyRefresh();
            } else {
                window.doRefresh = false;
            }
        }
    });

    $("#singleRefreshButton").click(function(event) {
        if (renderMode === "optimized") {
            GameClient.stopAnimation();
            GameClient.stepOptimized();
        } else if (renderMode === "scalajs") {
            GameClient.stopAnimation();
            var data = GameClient.step();
            draw(data);
        } else {
            window.doRefresh = false;
            legacyRefresh();
        }
    });

    $("#resetButton").click(function(event) {
        var width = $(this).data('width');
        var height = $(this).data('height');

        if (renderMode === "optimized") {
            GameClient.stopAnimation();
            GameClient.initOptimized(width, height);
        } else if (renderMode === "scalajs") {
            GameClient.stopAnimation();
            var data = GameClient.init(width, height);
            draw(data);
        } else {
            jsRoutes.controllers.LifeController.reset(width, height).ajax({
                success: function(data) {
                    draw(data);
                }
            });
        }
    });
});

// Legacy server-side refresh for comparison
function legacyRefresh() {
    if (window.doRefresh)
        setTimeout(legacyRefresh, 0);
    jsRoutes.controllers.LifeController.getState().ajax({
        success: function(data) {
            draw(data);
            updateLegacyFps();
        }
    });
}

function updateLegacyFps() {
    window.frameCount = (window.frameCount || 0) + 1;
    var now = performance.now();
    var elapsed = now - (window.lastFpsUpdate || now);

    if (elapsed >= 1000) {
        var fps = Math.round((window.frameCount * 1000) / elapsed);
        window.frameCount = 0;
        window.lastFpsUpdate = now;
        var fpsElement = document.getElementById('fpsCounter');
        if (fpsElement) {
            fpsElement.textContent = 'FPS: ' + fps + ' (Server)';
            fpsElement.style.color = fps > 30 ? '#00ff00' : fps > 15 ? '#ffff00' : '#ff0000';
        }
    }
}
