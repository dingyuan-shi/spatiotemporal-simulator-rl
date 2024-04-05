var bar;
var barOption;

function bar_init() {
    var chartDom = document.getElementById("divBar");
    bar = echarts.init(chartDom);
    barOption = {
    legend: {},
    grid: {
        left: '3%',
        right: '4%',
        top: '30%',
        bottom: '-3%',
        containLabel: true
    },
    xAxis: {
        type: 'value'
    },
    yAxis: {
        type: 'category',
        data: ['10', '9', '8', '7', '6', '5', '4', '3', '2', '1']
    },
    series: [
        {
            name: 'Transmission',
            type: 'bar',
            stack: 'total',
            color: "#b2da96", 
            label: {
                show: false
            },
            emphasis: {
                focus: 'series'
            },
            data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
            },
            {
            name: 'Queueing',
            type: 'bar',
            stack: 'total',
            label: {
                show: false
            },
            color: "#f5c74e", 
            emphasis: {
                focus: 'series'
            },
            data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
            },
            {
            name: 'Processing',
            type: 'bar',
            stack: 'total',
            color: "#e56465", 
            label: {
                show: false
            },
            emphasis: {
                focus: 'series'
            },
            data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
            }
        ]
    };
    bar.setOption(barOption);
}

function update_bar(data) {
    for (let i = 0; i < data.length; ++i) {
        for (let j = 0; j < barOption.series.length; ++j) {
            barOption.series[j].data[i] = data[i][j];
        }
    }
    bar.setOption(barOption);
}
