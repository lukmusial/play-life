function padString(binString, targetLength) {
    var len = targetLength-binString.length;
    if (len>0)
        return Array(targetLength-binString.length+1).join("0")+binString;
    return binString;
}

function putPixel(data, coord, value){
if (value == 1) {
    data[coord] =     0x77;
    data[coord + 1] = 0xCA;
    data[coord + 2] = 0xE6;
    data[coord + 3] = 255;
 } else {
    var coord0 = data[coord]
    var coord3 = data[coord +3]
    if (coord0 == 0x77 && coord3 == 255) data[coord +3] = 180;
    else if (coord0 == 0x77 && coord3 == 180) data[coord +3] = 140;
    else if (coord0 == 0x77 && coord3 == 140) data[coord +3] = 100;
    else if (coord0 == 0x77 && coord3 == 100) data[coord +3] = 50;
    else {
        data[coord] =     0;
        data[coord + 1] = 0;
        data[coord + 2] = 0;
        data[coord + 3] = 255;
    }
 }
}

function draw(arrHex) {
 var canvas = document.getElementById("canvas");
if (canvas.getContext) {
      var ctx = canvas.getContext("2d");
      var height = arrHex[0]
      var width = arrHex[1]

      var h = ctx.canvas.height;
      var w = ctx.canvas.width;

      var imgData = ctx.getImageData(0, 0, w, h);
      var data = imgData.data;
      for (var i=0; i < height; i++) {
            var map = Array.prototype.map
            var mappedString = map.call(arrHex[i+2], function(x) {return padString(x.charCodeAt(0).toString(2),8);})
            for (var j=0; j < mappedString.length; j++) {
                 for (var k=0; k<8; k++) {
                   putPixel(data, 4*(i*w + k +j*8), mappedString[j][k])
                 }
            }
      }

      ctx.putImageData(imgData, 0, 0);
  }
}
