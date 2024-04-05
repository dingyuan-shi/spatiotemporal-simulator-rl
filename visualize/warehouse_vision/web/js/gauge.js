
var gaugeLeft;
var gaugeRight;
var optionLeft;
var optionRight;

function gauge_init(height) {
    var chartDomLeft = document.querySelector("#gauge_left");
    var chartDomRight = document.querySelector("#gauge_right");
    // gaugeLeft = echarts.init(chartDomLeft, null, {height: height});
    // gaugeRight = echarts.init(chartDomRight, null, {height: height});
    gaugeLeft = echarts.init(chartDomLeft);
    gaugeRight = echarts.init(chartDomRight);
    var inner_left = {
        type: 'gauge',
        min: 0,
        max: 100,
        z: 1,
        startAngle: 210,
        endAngle: -30,
        splitNumber: 5,
        radius: '70%',
        center: ['50%', '50%'],
        axisLine: {
            show: true,
            lineStyle: {
            width: 0,
            color: [
                [0.825, '#fff'],
                [1, '#f00']
            ]
            }
        },
        splitLine: {
            distance: 20,
            length: 15,
            lineStyle: {
            color: 'auto',
            width: 2,
            shadowColor: 'rgba(255, 255, 255, 0.5)',
            shadowBlur: 15,
            shadowOffsetY: -10
            }
        },
        axisTick: {
            distance: 20,
            length: 8,
            lineStyle: {
            color: 'auto',
            width: 2,
            shadowColor: 'rgba(255, 255, 255)',
            shadowBlur: 10,
            shadowOffsetY: -10
            }
        },
        axisLabel: {
            distance: 0,
            fontSize: 16,
            fontWeight: 200,
            fontFamily: 'Arial',
            color: '#fff'
        },
        anchor: {},
        pointer: {
            icon: 'path://M-36.5,23.9L-41,4.4c-0.1-0.4-0.4-0.7-0.7-0.7c-0.5-0.1-1.1,0.2-1.2,0.7l-4.5,19.5c0,0.1,0,0.1,0,0.2v92.3c0,0.6,0.4,1,1,1h9c0.6,0,1-0.4,1-1V24.1C-36.5,24-36.5,23.9-36.5,23.9z M-39.5,114.6h-5v-85h5V114.6z',
            width: 5,
            offsetCenter: [0, '-10%'],
            length: '70%',
            itemStyle: {
            color: '#f00',
            shadowColor: 'rgba(255, 0, 0)',
            shadowBlur: 5,
            shadowOffsetY: 3
            }
        },
        title: {
            color: '#fff',
            fontSize: 16,
            fontWeight: 200,
            fontFamily: 'Arial',
            offsetCenter: [0, '-30%']
        },
        data: [
            {
            value: 32,
            name: 'PPR/%'
            }
        ],
        detail: {
            offsetCenter: ['0%', '40%'],
            formatter: '{a|{value}}',
            rich: {
            a: {
                fontSize: 16,
                fontWeight: 200,
                fontFamily: 'Arial',
                color: '#fff',
                align: 'center',
                padding: [0, 5, 0, 0]
            }
            }
        }
    };
    var outer_left = {
        type: 'gauge',
        z: 5,
        radius: '75%',
        center: ['50%', '50%'],
        axisLine: {
            lineStyle: {
            width: 5,
            color: [[1, '#FFF']]
            }
        },
        splitLine: {
            show: false
        },
        axisTick: {
            show: false
        },
        axisLabel: {
            show: false
        },
        pointer: {
            show: false
        },
        title: {
            show: false
        },
        detail: { show: false }
    };
    var outer_right = JSON.parse(JSON.stringify(outer_left));
    outer_right.center = ['50%', '50%'];
    var inner_right = JSON.parse(JSON.stringify(inner_left));
    inner_right.center = ['50%', '50%'];
    inner_right.data[0].name = 'RWR/%';
    optionLeft = {
        background: false,
        series: [
            inner_left,
            outer_left,
        ]
    };
    optionRight = {
        background: false,
        series: [
            inner_right,
            outer_right
        ]
    }
    update_gauge(0, 0);
}

function update_gauge(ppr, rwr) {
    optionLeft.series[0].data[0].value = ppr;
    optionRight.series[0].data[0].value = rwr;
    gaugeLeft.setOption(optionLeft);
    gaugeRight.setOption(optionRight);
}