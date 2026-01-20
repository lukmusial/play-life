// 3D Game of Life - Scala.js Client
// Uses client-side computation with Three.js rendering for maximum performance

// Rendering modes:
// 1. "scalajs" - Scala.js with raw data + drawRaw callback (fastest)
// 2. "server" - Server-side computation with AJAX (legacy)
var renderMode = "scalajs";

// Debug: Check if GameClient is available
if (typeof GameClient === 'undefined') {
    console.error("GameClient not loaded! Falling back to server mode.");
    renderMode = "server";
} else {
    console.log("GameClient loaded, using " + renderMode + " mode for 3D");
}

// Rule descriptions for display
var ruleDescriptions = {
    "original": "B9-11/S6-11 (26 neighbors) - Original 3D Life",
    "4555": "B5/S4,5 (26 neighbors) - Most life-like, stable",
    "5766": "B6,7/S5,6 (26 neighbors) - Slow growth, interesting",
    "pyroclastic": "B4-5/S5 (26 neighbors) - Explosive bursts",
    "crystal": "B6/S5-7 (26 neighbors) - Crystalline growth",
    "vonneumann": "B1-2/S2-4 (6 neighbors) - Sparse patterns"
};

function updateRuleDisplay(ruleName) {
    // Reset all buttons to outline style (blue)
    $(".rule-btn")
        .removeClass("btn-primary btn-info btn-light btn-outline-primary btn-outline-secondary btn-outline-light")
        .addClass("btn-outline-info");

    // Highlight selected button (solid blue)
    $(".rule-btn[data-rule='" + ruleName + "']")
        .removeClass("btn-outline-info")
        .addClass("btn-info");

    // Update info text
    var infoText = ruleDescriptions[ruleName] || ruleName;
    $("#ruleInfo").text(infoText);
}

// Get FPS limit from selector
function getFpsLimit() {
    var limit = parseInt($("#fpsLimit").val(), 10);
    return limit > 0 ? limit : 0; // 0 means unlimited
}

$(function() {
    // Rule selection buttons - changes rule and resets simulation
    $(".rule-btn").click(function(event) {
        // Use attr() instead of data() to avoid jQuery auto-converting numbers
        var ruleName = $(this).attr("data-rule");
        console.log("Rule button clicked: " + ruleName);
        event.preventDefault();
        event.stopPropagation();
        if (renderMode === "scalajs") {
            try {
                var wasRunning = GameClient.isRunning();
                GameClient.stopAnimation();
                GameClient.setRule(ruleName);
                updateRuleDisplay(ruleName);

                // Reset the simulation to see the rule effect from scratch
                var layers = $("#resetButton").data('layers');
                var width = $("#resetButton").data('width');
                var height = $("#resetButton").data('height');
                GameClient.init3D(layers, width, height);
                var data = GameClient.get3DState();
                drawRaw(data);

                // Resume animation if it was running
                if (wasRunning) {
                    GameClient.start3DAnimation(function(data) {
                        drawRaw(data);
                    });
                }
                console.log("Rule changed to: " + ruleName + ", simulation reset");
            } catch (e) {
                console.error("Error changing rule to " + ruleName + ": ", e);
            }
        }
    });

    $("#autoRefreshButton").click(function(event) {
        if (renderMode === "scalajs") {
            if (!GameClient.isRunning()) {
                GameClient.start3DAnimation(function(data) {
                    drawRaw(data);
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
        if (renderMode === "scalajs") {
            GameClient.stopAnimation();
            var data = GameClient.step3D();
            drawRaw(data);
        } else {
            window.doRefresh = false;
            legacyRefresh();
        }
    });

    $("#resetButton").click(function(event) {
        var layers = $(this).data('layers');
        var width = $(this).data('width');
        var height = $(this).data('height');
        console.log("Reset 3D clicked: " + layers + " layers, " + width + "x" + height + ", mode: " + renderMode);

        if (renderMode === "scalajs") {
            GameClient.stopAnimation();
            GameClient.init3D(layers, width, height);
            var data = GameClient.get3DState();
            drawRaw(data);
            // Update rule display
            var currentRule = GameClient.getCurrentRule();
            updateRuleDisplay(currentRule);
            console.log("Initialized 3D with client-side rendering, rule: " + currentRule);
        } else {
            jsRoutes.controllers.ThreedController.reset(layers, width, height).ajax({
                success: function(data) {
                    draw(data);
                }
            });
        }
    });

    // FPS limit selector
    $("#fpsLimit").change(function() {
        var limit = parseInt($(this).val(), 10);
        if (renderMode === "scalajs") {
            GameClient.setFpsLimit(limit);
            console.log("FPS limit set to: " + (limit > 0 ? limit : "unlimited"));
        }
    });

    // Initialize on page load
    $("#resetButton").click();

    // Set initial FPS limit
    if (renderMode === "scalajs") {
        var initialLimit = parseInt($("#fpsLimit").val(), 10);
        GameClient.setFpsLimit(initialLimit);
    }
});

// Legacy server-side refresh for comparison
function legacyRefresh() {
    if (window.doRefresh)
        setTimeout(legacyRefresh, 500);
    jsRoutes.controllers.ThreedController.getState().ajax({
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
