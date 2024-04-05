/**
 * ?@param container 无#的容器ID
 * ?@param config.data = lineData
 */
let AreaRange = function (container, config) {
    let data = config.data;
    let supply = data.supply;
    let demand = data.demand;

    //create a function to find where lines intersect, to color them correctly
    function intersect(x1, x2, y1, y2, y3, y4) {
        return (
            (x2 * y1 - x1 * y2 - (x2 * y3 - x1 * y4)) / (y4 - y3 - (y2 - y1))
        );
    }

    let ranges = []; //stores all the data for the graph like so [x, y1, y2]
    let demandZones = []; //stores the different zones based on where the lines intersect
    let demandBiggerBool = true; //used for keeping track of what current color is

    //loop through all values in supply and demand array (assumes they are same length). Fill the ranges array and create color zones.
    //Zones color up to a given point, therefore we need to push a color at the end, before it intersects
    for (let i = 0; i < demand.length; i++) {
        ranges.push([i, demand[i], supply[i]]); //push to range array

        if (demand[i] < supply[i] && demandBiggerBool) {
            demandZones.push({
                value: intersect(
                    i - 1,
                    i,
                    demand[i - 1],
                    demand[i],
                    supply[i - 1],
                    supply[i]
                ),
                fillColor: "rgba(255, 125, 65, 0.5)", // blue
            }); //push to zone array
            demandBiggerBool = false;
        } else if (demand[i] > supply[i] && !demandBiggerBool) {
            demandZones.push({
                value: intersect(
                    i - 1,
                    i,
                    demand[i - 1],
                    demand[i],
                    supply[i - 1],
                    supply[i]
                ),
                fillColor: "rgba(29, 153, 255, 0.5)", // orange
            }); //push to zone array
            demandBiggerBool = true;
        }
    }

    //zones color up to a given point, therefore we need to push a color at the end as well:
    if (demandBiggerBool) {
        demandZones.push({
            value: demand.length,
            fillColor: "rgba(255, 125, 65, 0.5)", // blue
        });
    } else {
        demandZones.push({
            value: demand.length,
            fillColor: "rgba(29, 153, 255, 0.5)", // orange
        });
    }

    let myHighChart = Highcharts.stockChart(container, {
        chart: {
            backgroundColor: "transparent",
        },
        credits: {
            enabled: false,
        },
        exporting: {
            enabled: false,
        },
        rangeSelector: {
            enabled: false,
        },
        scrollbar: {
            enabled: false,
        },
        navigator: {
            enabled: false,
        },
        xAxis: {
            visible: false,
            crosshair: false,
        },
        yAxis: {
            visible: false,
        },
        tooltip: {
            enabled: false,
        },
        plotOptions: {
            series: {
                states: {
                    inactive: {
                        opacity: 1,
                    },
                    hover: {
                        enabled: false,
                    },
                },
            },
        },
        series: [
            {
                name: "Range",
                type: "arearange",
                data: ranges,
                zoneAxis: "x",
                zones: demandZones,
            },
            {
                name: "demand",
                type: "line",
                data: demand,
                color: "rgba(255, 125, 65, 0.8)",
            },
            {
                name: "supply",
                type: "line",
                data: supply,
                color: "rgba(29, 153, 255, 0.8)",
            },
        ],
    });
};
