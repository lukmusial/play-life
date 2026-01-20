// Game of Life - Scala.js Client
// Uses client-side computation with optimized rendering for maximum performance

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
        if (!GameClient.isRunning()) {
            GameClient.startOptimized();
        } else {
            GameClient.stopAnimation();
        }
    });

    $("#singleRefreshButton").click(function(event) {
        GameClient.stopAnimation();
        GameClient.stepOptimized();
    });

    $("#resetButton").click(function(event) {
        var dims = getCanvasDimensions();
        var width = dims.width;
        var height = dims.height;

        console.log("Reset clicked: " + width + "x" + height);

        // Resize the canvas element
        resizeCanvas(width, height);

        GameClient.stopAnimation();
        GameClient.initOptimized(width, height);
        console.log("Initialized with optimized rendering");
    });

    // Canvas size selector
    $("#canvasSize").change(function() {
        $("#resetButton").click();
    });

    // FPS limit selector
    $("#fpsLimit").change(function() {
        var limit = parseInt($(this).val(), 10);
        GameClient.setFpsLimit(limit);
        console.log("FPS limit set to: " + (limit > 0 ? limit : "unlimited"));
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
    var initialLimit = parseInt($("#fpsLimit").val(), 10);
    GameClient.setFpsLimit(initialLimit);

    $("#resetButton").click();
});
