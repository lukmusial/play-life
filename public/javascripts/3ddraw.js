
function padString(binString, targetWidth) {
    var len = targetWidth-binString.length;
    if (len>0)
        return Array(targetWidth-binString.length+1).join("0")+binString;
    return binString;
}

function createParticles(particleSystem, particles, planes, height, width, scene) {
        for (var plane = 0; plane<planes.length; plane++) {
                for (var x = 0; x < height; x++) {
                      for (var y = 0; y < width*UNIT_WIDTH; y++) {
                        var particle = new THREE.Vector3((x-height/2)*3, (y-width*UNIT_WIDTH/2)*3, planes[plane].position.z );
                        particles.vertices.push(particle);
                     }
                }
            }
        scene.add(particleSystem);
}

function draw(arrHex) {

      var layers = arrHex.length
      var height = arrHex[0].length;
      var width = arrHex[0][0].length;

      if (particles.vertices.length==0) {
         createParticles(particleSystem, particles, planes, height, width, scene)
      }

    var alphas = [];
    for (var plane = 0; plane<layers; plane++) {
          for (var i=0; i < height; i++) {
                for (var j = 0; j<width;j++) {
                    var paddedString = padString(arrHex[plane][i][j].toString(2), UNIT_WIDTH);
                    var rangeAlphas = Array.prototype.map.call(paddedString, function(x) { if (x==1) return 0.8; return 0; });
                    alphas.push.apply(alphas, rangeAlphas)
                }
          }
     }
     attributes.alpha.value = alphas;
     attributes.alpha.needsUpdate = true;

}

// Raw data version for Scala.js client (no hex encoding)
// arrRaw[layer][row][col] = 0 or 1
function drawRaw(arrRaw) {
    var layers = arrRaw.length;
    var height = arrRaw[0].length;
    var width = arrRaw[0][0].length;

    // Create particles if not yet initialized
    if (particles.vertices.length == 0) {
        // For raw data, we don't use UNIT_WIDTH packing
        createParticlesRaw(particleSystem, particles, planes, height, width, scene);
    }

    var alphas = [];
    for (var plane = 0; plane < layers; plane++) {
        for (var i = 0; i < height; i++) {
            for (var j = 0; j < width; j++) {
                alphas.push(arrRaw[plane][i][j] == 1 ? 0.8 : 0);
            }
        }
    }
    attributes.alpha.value = alphas;
    attributes.alpha.needsUpdate = true;
}

function createParticlesRaw(particleSystem, particles, planes, height, width, scene) {
    for (var plane = 0; plane < planes.length; plane++) {
        for (var x = 0; x < height; x++) {
            for (var y = 0; y < width; y++) {
                var particle = new THREE.Vector3(
                    (x - height / 2) * 3,
                    (y - width / 2) * 3,
                    planes[plane].position.z
                );
                particles.vertices.push(particle);
            }
        }
    }
    scene.add(particleSystem);
}



// Mouse interaction state
var isDragging = false;
var previousMousePosition = { x: 0, y: 0 };
var lastInteractionTime = 0;
var autoRotatePauseMs = 10000; // Resume auto-rotation after 10 seconds
var userRotation = { x: 0, y: 0 };
var targetZoom = 1;
var currentZoom = 1;

function animate(lastTime, particleSystem){
        var time = (new Date()).getTime();
        var timeDiff = time - lastTime;

        // Check if we should auto-rotate (10 seconds since last interaction)
        var timeSinceInteraction = time - lastInteractionTime;
        var shouldAutoRotate = timeSinceInteraction > autoRotatePauseMs;

        // Update rotation status indicator
        var statusEl = document.getElementById('rotationStatus');
        if (statusEl) {
            if (shouldAutoRotate) {
                statusEl.textContent = '🔄 Auto-rotating';
                statusEl.style.color = '#888';
            } else {
                var secondsLeft = Math.ceil((autoRotatePauseMs - timeSinceInteraction) / 1000);
                statusEl.textContent = '✋ Manual control (' + secondsLeft + 's)';
                statusEl.style.color = '#aaa';
            }
        }

        if (shouldAutoRotate) {
            var angleChange = angularSpeed * timeDiff * 2 * Math.PI / 1000;
            for (i = 0; i<planes.length; i++) {
                 planes[i].rotation.z += angleChange;
            }
            particleSystem.rotation.z += angleChange;
        }

        // Smooth zoom interpolation
        currentZoom += (targetZoom - currentZoom) * 0.1;
        camera.position.y = -1200 * currentZoom;
        camera.position.z = 800 * currentZoom;
        camera.lookAt(new THREE.Vector3(0, 0, 50));

        lastTime = time;
        renderer.render(scene, camera);
        requestAnimationFrame(function() {
            animate(lastTime, particleSystem);
        });
}

// Mouse event handlers for rotation
function onMouseDown(event) {
    // Ignore clicks on the control panel
    if (event.target.closest('#controls')) return;

    isDragging = true;
    previousMousePosition = {
        x: event.clientX,
        y: event.clientY
    };
    lastInteractionTime = (new Date()).getTime();
}

function onMouseUp(event) {
    isDragging = false;
}

function onMouseMove(event) {
    if (!isDragging) return;

    lastInteractionTime = (new Date()).getTime();

    var deltaX = event.clientX - previousMousePosition.x;
    var deltaY = event.clientY - previousMousePosition.y;

    // Rotate the scene based on mouse movement
    var rotationSpeed = 0.005;

    // Horizontal drag: rotate around Z axis
    for (var i = 0; i < planes.length; i++) {
        planes[i].rotation.z += deltaX * rotationSpeed;
    }
    particleSystem.rotation.z += deltaX * rotationSpeed;

    // Vertical drag: rotate around X axis
    for (var i = 0; i < planes.length; i++) {
        planes[i].rotation.x += deltaY * rotationSpeed;
    }
    particleSystem.rotation.x += deltaY * rotationSpeed;

    previousMousePosition = {
        x: event.clientX,
        y: event.clientY
    };
}

function onMouseWheel(event) {
    // Ignore scroll on the control panel
    if (event.target.closest('#controls')) return;

    lastInteractionTime = (new Date()).getTime();
    event.preventDefault();

    // Zoom in/out
    var zoomSpeed = 0.1;
    if (event.deltaY > 0) {
        targetZoom = Math.min(3, targetZoom + zoomSpeed); // Zoom out (max 3x distance)
    } else {
        targetZoom = Math.max(0.3, targetZoom - zoomSpeed); // Zoom in (min 0.3x distance)
    }
}

// Touch event handlers for mobile
function onTouchStart(event) {
    if (event.target.closest('#controls')) return;
    if (event.touches.length === 1) {
        isDragging = true;
        previousMousePosition = {
            x: event.touches[0].clientX,
            y: event.touches[0].clientY
        };
        lastInteractionTime = (new Date()).getTime();
    }
}

function onTouchEnd(event) {
    isDragging = false;
}

function onTouchMove(event) {
    if (!isDragging || event.touches.length !== 1) return;

    lastInteractionTime = (new Date()).getTime();

    var deltaX = event.touches[0].clientX - previousMousePosition.x;
    var deltaY = event.touches[0].clientY - previousMousePosition.y;

    var rotationSpeed = 0.005;

    // Horizontal drag: rotate around Z axis
    for (var i = 0; i < planes.length; i++) {
        planes[i].rotation.z += deltaX * rotationSpeed;
    }
    particleSystem.rotation.z += deltaX * rotationSpeed;

    // Vertical drag: rotate around X axis
    for (var i = 0; i < planes.length; i++) {
        planes[i].rotation.x += deltaY * rotationSpeed;
    }
    particleSystem.rotation.x += deltaY * rotationSpeed;

    previousMousePosition = {
        x: event.touches[0].clientX,
        y: event.touches[0].clientY
    };
}

// Register event listeners
document.addEventListener('mousedown', onMouseDown, false);
document.addEventListener('mouseup', onMouseUp, false);
document.addEventListener('mousemove', onMouseMove, false);
document.addEventListener('wheel', onMouseWheel, { passive: false });
document.addEventListener('touchstart', onTouchStart, false);
document.addEventListener('touchend', onTouchEnd, false);
document.addEventListener('touchmove', onTouchMove, false);


var uniforms, attributes, particles;
var angularSpeed = 0.03;

var PLANE_WIDTH=106
var PLANE_HEIGHT=106
var UNIT_WIDTH=53;

      // renderer
      var renderer = new THREE.WebGLRenderer();
      renderer.setSize(window.innerWidth, window.innerHeight);
      renderer.domElement.style.position = 'fixed';
      renderer.domElement.style.top = '0';
      renderer.domElement.style.left = '0';
      renderer.domElement.style.zIndex = '1';
      document.body.appendChild(renderer.domElement);

      //
      var particles = new THREE.Geometry()


         // attributes
    attributes = {

        alpha: { type: 'f', value: [] }

    };

    // uniforms
    uniforms = {

        color: { type: "c", value: new THREE.Color( 0x77cae6 ) }

    };


    var shaderMaterial = new THREE.ShaderMaterial( {

        uniforms:       uniforms,
        attributes:     attributes,
        vertexShader:   document.getElementById( 'vertexshader' ).textContent,
        fragmentShader: document.getElementById( 'fragmentshader' ).textContent,
        transparent:    true

    });

      // camera - positioned to center the 3D scene vertically
      // Account for control panel at top by shifting view down
      var camera = new THREE.PerspectiveCamera(16, window.innerWidth / window.innerHeight, 1, 3000);
      camera.position.x = 0;
      camera.position.y = -1200;
      camera.position.z = 800;
      // Look at a point slightly above center to shift scene down on screen
      camera.lookAt(new THREE.Vector3(0, 0, 50));

var scene = new THREE.Scene();
var planes = new Array();
for (i = 0; i < 20; i++) {
    planes[i] = new THREE.Mesh(new THREE.PlaneGeometry(PLANE_HEIGHT*3, PLANE_WIDTH*3), new THREE.MeshNormalMaterial({color: 0x77cae6, transparent: true, opacity: 0.02}));
    planes[i].position.z = (i-10)*10;
    planes[i].overdraw = true;
    scene.add(planes[i]);
}


var particleSystem = new THREE.ParticleSystem(particles, shaderMaterial);


animate(0, particleSystem, planes);