// Game of Life - Scala.js Client
// Uses client-side computation with optimized rendering for maximum performance

// Rendering modes:
// 1. "optimized" - Direct Scala.js rendering with Uint32Array (fastest)
// 2. "scalajs" - Scala.js with hex encoding + JS draw callback
// 3. "server" - Server-side computation with AJAX (legacy)
var renderMode = "optimized";

// Debug: Check if GameClient is available
if (typeof GameClient === 'undefined') {
    console.error("GameClient not loaded! Falling back to server mode.");
    renderMode = "server";
} else {
    console.log("GameClient loaded, using " + renderMode + " mode");
}

// Get canvas dimensions based on selector or browser size
function getCanvasDimensions() {
    var sizeValue = $("#canvasSize").val();
    if (sizeValue === "browser") {
        // Use browser window size, accounting for control panel
        var controlHeight = $("#controls").outerHeight() || 60;
        return {
            width: window.innerWidth,
            height: window.innerHeight - controlHeight
        };
    } else {
        var parts = sizeValue.split("x");
        return {
            width: parseInt(parts[0], 10),
            height: parseInt(parts[1], 10)
        };
    }
}

// Resize canvas element
function resizeCanvas(width, height) {
    var canvas = document.getElementById("canvas");
    if (canvas) {
        canvas.width = width;
        canvas.height = height;
    }
}

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
        var dims = getCanvasDimensions();
        var width = dims.width;
        var height = dims.height;

        console.log("Reset clicked: " + width + "x" + height + ", mode: " + renderMode);

        // Resize the canvas element
        resizeCanvas(width, height);

        if (renderMode === "optimized") {
            GameClient.stopAnimation();
            GameClient.initOptimized(width, height);
            console.log("Initialized with optimized rendering");
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

    // Canvas size selector
    $("#canvasSize").change(function() {
        $("#resetButton").click();
    });

    // FPS limit selector
    $("#fpsLimit").change(function() {
        var limit = parseInt($(this).val(), 10);
        if (renderMode === "scalajs" || renderMode === "optimized") {
            GameClient.setFpsLimit(limit);
            console.log("FPS limit set to: " + (limit > 0 ? limit : "unlimited"));
        }
    });

    // Handle window resize when in browser size mode
    $(window).resize(function() {
        if ($("#canvasSize").val() === "browser") {
            // Debounce resize
            clearTimeout(window.resizeTimer);
            window.resizeTimer = setTimeout(function() {
                $("#resetButton").click();
            }, 250);
        }
    });

    // Initialize on page load
    // Set initial FPS limit
    if (renderMode === "scalajs" || renderMode === "optimized") {
        var initialLimit = parseInt($("#fpsLimit").val(), 10);
        GameClient.setFpsLimit(initialLimit);
    }

    $("#resetButton").click();
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
