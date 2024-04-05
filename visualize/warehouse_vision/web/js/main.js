window.onload = main;

var process;
var t = 0;
function main() {
    // get meta data
    $.getJSON("meta.json", function(data) {
        //data 代表读取到的json中的数据
        var warehouse_height = data.height;
        var warehouse_width = data.width;
        var stat_freq = data.stat_freq;
        bar_init();
        area_init();
        var gauge_div_h = parseInt(document.getElementById("divGauge").style.height.slice(0, -2));
        gauge_init(gauge_div_h);
        var heat_div_h = parseInt(document.getElementById("divHeat").style.height.slice(0, -2))
        heat_init(heat_div_h, warehouse_height, warehouse_width);
        var warehouse_div_h = parseInt(document.getElementById("divCanvas").style.height.slice(0, -2)) - 40;
        warehouse_init(warehouse_div_h, warehouse_height, warehouse_width).then((values) => {
            process = setInterval(refresh, 100);
        });
    });
}

function refresh() {
    $.getJSON("frame", {"t": t}, function(data) {
        // parse data and update gauge
        snapshot = data;
        if (snapshot.hasOwnProperty("frame")) {
            draw(snapshot.frame);
        }
        if (snapshot.hasOwnProperty("ppr") && snapshot.hasOwnProperty("rwr")) {
            update_gauge(parseFloat(snapshot.ppr), parseFloat(snapshot.rwr));
        }
        if (snapshot.hasOwnProperty("heat")) {
            update_heat(snapshot.heat);
        }
        if (snapshot.hasOwnProperty("area")) {
            update_area(snapshot.area);
        }
        if (snapshot.hasOwnProperty("bar")) {
            update_bar(snapshot.bar);
        }
        if (snapshot.hasOwnProperty("end")) {
            clearInterval(process);
        }
    });
    ++t;
}