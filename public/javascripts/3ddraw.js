
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



function animate(lastTime, particleSystem){
        var time = (new Date()).getTime();
        var timeDiff = time - lastTime;
        var angleChange = angularSpeed * timeDiff * 2 * Math.PI / 1000;
        for (i = 0; i<planes.length; i++) {
             planes[i].rotation.z += angleChange;
        }
        particleSystem.rotation.z += angleChange;
        lastTime = time;
        renderer.render(scene, camera);
        requestAnimationFrame(function() {
            animate(lastTime, particleSystem);
        });
}


var uniforms, attributes, particles;
var angularSpeed = 0.03;

var PLANE_WIDTH=106
var PLANE_HEIGHT=106
var UNIT_WIDTH=53;

      // renderer
      var renderer = new THREE.WebGLRenderer();
      renderer.setSize(window.innerWidth, window.innerHeight);
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

      // camera
      var camera = new THREE.PerspectiveCamera(16, window.innerWidth / window.innerHeight, 1, 3000);
      camera.position.y = -950;
      camera.position.z = 1250;
      camera.rotation.x = 35 * (Math.PI / 180);

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