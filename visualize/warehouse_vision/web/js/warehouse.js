var cx;
var warehouse_height;
var warehouse_width;
var unit_len;
var aCanvas;
var robot_free;
var robot_carry;
var rack;
var picker;

function warehouse_init(h, height, width) {
    warehouse_height = height;
    warehouse_width = width;
    unit_len = h / warehouse_height;
    aCanvas = document.querySelector("#aCanvas");
    aCanvas.width = warehouse_width * unit_len;
    aCanvas.height = warehouse_height * unit_len;
    cx = aCanvas.getContext('2d');
    // load image
    return Promise.all(
        [
            new Promise(resolve => {
                robot_free = document.createElement("img");
                robot_free.onload = () => { resolve("done"); };
                robot_free.src = "./images/robot_free.png";
            }),
            new Promise(resolve => {
                robot_carry = document.createElement("img");
                robot_carry.onload = () => { resolve("done"); };
                robot_carry.src = "./images/robot_carry.png";
            }),
            new Promise(resolve => {
                rack = document.createElement("img");
                rack.onload = () => { resolve("done"); };
                rack.src = "./images/rack.png";
            }),
            new Promise(resolve => {
                picker = document.createElement("img");
                picker.onload = () => { resolve("done"); };
                picker.src = "./images/picker.png";
            })
        ]
    );
}

function draw(frame) {
    cx.clearRect(0, 0, aCanvas.width, aCanvas.height);
    for (let i = 0; i < frame.length; i++) {
        var cur = frame[i];
        var r = parseInt(i / warehouse_width);
        var c = i % warehouse_width;
        var real_r = r * unit_len;
        var real_c = c * unit_len;
        if (cur == 'R') {
            cx.drawImage(rack, real_c, real_r, unit_len, unit_len);
        } else if (cur == 'P') {
            cx.drawImage(picker, real_c, real_r, unit_len, unit_len);
        }else if (cur == 'A') {
            cx.drawImage(robot_free, real_c, real_r, unit_len, unit_len);
        } else if (cur == 'C') {
            cx.drawImage(robot_carry, real_c, real_r, unit_len, unit_len);
        } 
    }
}