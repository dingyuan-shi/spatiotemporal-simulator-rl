{
    let max_num = 5;
    let begin_time = new Date(2016, 11, 6, 4)

    // ans_rate, comp_rate
    function updateRate(time, myData) {
        let dataSet = [myData['ans_rate'],myData['comp_rate']];
        let ans_rate = (dataSet[0] * 100).toFixed(0);
        let rej_rate = ((dataSet[0] - dataSet[1]) * 100).toFixed(0);
        let wait_rate = 100 - ans_rate;
        let source = option.dataset.source;
        let timeAxis = option.dataset['source'][0];
        source[0] = insert(timeAxis,time.toLocaleTimeString().replace(/^\D*/, ''))
        source[1] = insert(source[1],ans_rate);
        source[2] = insert(source[2],rej_rate);
        source[3] = insert(source[3],wait_rate);
        // console.log(source);
        myChart.setOption(option);
    }

    let dom = document.getElementById("rate_container");
    let myChart = echarts.init(dom);
    let app = {};

    let option;


    function insert(l, data) {
        let tmp = l.slice(2)
        tmp.unshift(l[0]);
        tmp.push(data);
        return tmp;
    }

    setTimeout(function () {

        option = {
            legend: {
                textStyle: {
                    color: 'rgba(255,255,255,0.6)'
                }
            },
            tooltip: {
                trigger: 'axis',
                showContent: true
            },

            dataset: {
                source: [
                    ['product', '9:59:50', '9:59:52', '9:59:54', '9:59:56', '9:59:58', '10:00:00'],
                    ['Response', 100, 100, 100, 100, 100, 100],
                    ['Reject', 0, 0, 0, 0, 0, 0],
                    ['Waiting', 0, 0, 0, 0, 0, 0],
                ]
            },
            xAxis: {type: 'category'},
            yAxis: {gridIndex: 0},
            grid: {
                top: '55%',
                bottom: '30px'

            },
            series: [
                {type: 'line', seriesLayoutBy: 'row', emphasis: {focus: 'series'}},
                {type: 'line', seriesLayoutBy: 'row', emphasis: {focus: 'series'}},
                {type: 'line', seriesLayoutBy: 'row', emphasis: {focus: 'series'}},

                {
                    type: 'pie',
                    id: 'pie',
                    // roseType: 'radius',
                    radius: '30%',
                    center: ['50%', '30%'],
                    emphasis: {focus: 'data'},
                    label: {
                        // formatter: '{b}: {@2012} ({d}%)'
                        show: false
                    },
                    encode: {
                        itemName: 'product',
                        value: 6,
                        tooltip: 6
                    }
                }
            ]
        };

        myChart.on('updateAxisPointer', function (event) {
            // console.log(event);
            let xAxisInfo = event.axesInfo[0];
            if (xAxisInfo) {
                let dimension = xAxisInfo.value + 1;
                myChart.setOption({
                    series: {
                        id: 'pie',
                        label: {
                            formatter: '{b}: {@[' + dimension + ']} ({d}%)'
                        },
                        encode: {
                            value: dimension,
                            tooltip: dimension
                        }
                    }
                });
            }
        });

        myChart.setOption(option);

    });

    if (option && typeof option === 'object') {
        myChart.setOption(option);
    }

}