// 3D Game of Life - Scala.js Client
// Uses client-side computation with Three.js rendering for maximum performance

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

$(function() {
    // Rule selection buttons - changes rule and resets simulation
    $(".rule-btn").click(function(event) {
        // Use attr() instead of data() to avoid jQuery auto-converting numbers
        var ruleName = $(this).attr("data-rule");
        console.log("Rule button clicked: " + ruleName);
        event.preventDefault();
        event.stopPropagation();

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
    });

    $("#autoRefreshButton").click(function(event) {
        if (!GameClient.isRunning()) {
            GameClient.start3DAnimation(function(data) {
                drawRaw(data);
            });
        } else {
            GameClient.stopAnimation();
        }
    });

    $("#singleRefreshButton").click(function(event) {
        GameClient.stopAnimation();
        var data = GameClient.step3D();
        drawRaw(data);
    });

    $("#resetButton").click(function(event) {
        var layers = $(this).data('layers');
        var width = $(this).data('width');
        var height = $(this).data('height');
        console.log("Reset 3D clicked: " + layers + " layers, " + width + "x" + height);

        GameClient.stopAnimation();
        GameClient.init3D(layers, width, height);
        var data = GameClient.get3DState();
        drawRaw(data);
        // Update rule display
        var currentRule = GameClient.getCurrentRule();
        updateRuleDisplay(currentRule);
        console.log("Initialized 3D with client-side rendering, rule: " + currentRule);
    });

    // FPS limit selector
    $("#fpsLimit").change(function() {
        var limit = parseInt($(this).val(), 10);
        GameClient.setFpsLimit(limit);
        console.log("FPS limit set to: " + (limit > 0 ? limit : "unlimited"));
    });

    // Initialize on page load
    $("#resetButton").click();

    // Set initial FPS limit
    var initialLimit = parseInt($("#fpsLimit").val(), 10);
    GameClient.setFpsLimit(initialLimit);
});
