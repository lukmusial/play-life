var doRefresh = false;
var frameCount = 0;
var lastFpsUpdate = performance.now();
var currentFps = 0;

// FPS tracking
function updateFps() {
    frameCount++;
    var now = performance.now();
    var elapsed = now - lastFpsUpdate;

    if (elapsed >= 1000) {
        currentFps = Math.round((frameCount * 1000) / elapsed);
        frameCount = 0;
        lastFpsUpdate = now;
        updateFpsDisplay();
    }
}

function updateFpsDisplay() {
    var fpsElement = document.getElementById('fpsCounter');
    if (fpsElement) {
        fpsElement.textContent = 'FPS: ' + currentFps;
        // Color code: green > 30, yellow > 15, red <= 15
        if (currentFps > 30) {
            fpsElement.style.color = '#00ff00';
        } else if (currentFps > 15) {
            fpsElement.style.color = '#ffff00';
        } else {
            fpsElement.style.color = '#ff0000';
        }
    }
}

$(function() {
    $("#autoRefreshButton").click(function(event) {
        if (doRefresh == false) {
             doRefresh = true;
             frameCount = 0;
             lastFpsUpdate = performance.now();
             refresh();
        }
        else {
            doRefresh = false;
        }
    })
})

$(function() {
    $("#singleRefreshButton").click(function(event) {
             doRefresh = false;
             refresh();
    })
})

$(function() {
    $("#resetButton").click(function(event) {
        var width = $(this).data('width');
        var height = $(this).data('height');
        jsRoutes.controllers.LifeController.reset(width, height).ajax({
               success: function(data) {
                 draw(data)
               }
            })
    })
})


function refresh() {
    if (doRefresh == true)
        setTimeout(refresh, 0);  // Changed from 50ms to 0 for max speed
    jsRoutes.controllers.LifeController.getState().ajax({
               success: function(data) {
                   draw(data);
                   updateFps();
               }
    })
}
