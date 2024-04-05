{
    let max_num = 10;
    let begin_time = new Date(2016, 11, 6, 4)

    // acc_rewards,seg_rewards
    function updateReward(time, myData) {
        let dataSet = [myData['accu_rewards'],myData['seg_rewards']];
        let axisData = (time).toLocaleTimeString().replace(/^\D*/, '');
        let data0 = option.series[0].data;
        let data1 = option.series[1].data;
        data0.shift();
        data0.push(dataSet[0].toFixed(1));
        data1.shift();
        data1.push(dataSet[1].toFixed(1));

        option.xAxis[0].data.shift();
        option.xAxis[0].data.push(axisData);
        myChart.setOption(option);
    }


    let dom = document.getElementById("rewards_container");
    let myChart = echarts.init(dom);
    let app = {};

    let option;


    option = {
        // title: {
        //     text: '动态数据',
        //     subtext: '纯属虚构'
        // },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'cross',
                label: {
                    backgroundColor: '#283b56'
                }
            }
        },
        legend: {
            data: ['Accu.', 'Seg'],
            textStyle: {
                color: 'rgba(255,255,255,0.6)'
            }
        },
        // toolbox: {
        //     show: true,
        //     feature: {
        //         dataView: {readOnly: false},
        //         restore: {},
        //         saveAsImage: {}
        //     }
        // },
        dataZoom: {
            show: false,
            start: 0,
            end: 100
        },
        grid: {
            left: 40,
            bottom : 35,
        },
        xAxis: [
            {
                type: 'category',
                boundaryGap: true,
                axisLine: {
                    lineStyle: {
                        color: 'rgba(255,255,255,0.6)'
                    }
                },
                data:
                    (function () {
                        let now = begin_time;
                        let res = [];
                        let len = max_num;
                        while (len--) {
                            res.unshift(begin_time.toLocaleTimeString().replace(/^\D*/, ''));
                            now = new Date(now - 2000);
                        }
                        return res;
                    })()
            },
            /*{
                type: 'category',
                boundaryGap: true,
                data: (function () {
                    var res = [];
                    var len = 10;
                    while (len--) {
                        res.push(10 - len - 1);
                    }
                    return res;
                })()
            }*/
        ],
        yAxis: [
            {
                type: 'value',
                scale: true,
                name: '总收益',
                axisLabel: {
                    rotate: 40
                },
                axisLine: {
                    lineStyle: {
                        color: 'rgba(255,255,255,0.6)'
                    }
                },
                // max: 30,
                min: 0,
                boundaryGap: [0.2, 0.2]
            },
            {
                type: 'value',
                scale: true,
                axisLabel: {
                    rotate: 40
                },
                axisLine: {
                    lineStyle: {
                        color: 'rgba(255,255,255,0.6)'
                    }
                },
                name: '段内收益',
                // max: 1200,
                min: 0,
                boundaryGap: [0.2, 0.2]
            }
        ],
        series: [
            {
                name: '总收益',
                type: 'bar',
                xAxisIndex: 0,
                yAxisIndex: 1,
                data:
                    (function () {
                        var res = [];
                        var len = 10;
                        while (len--) {
                            res.push(0);
                        }
                        return res;
                    })()
            },
            {
                name: '段内收益',
                type: 'line',
                data: (function () {
                    var res = [];
                    var len = 0;
                    while (len < 10) {
                        res.push(0);
                        len++;
                    }
                    return res;
                })()
            }
        ]
    };

    // app.count = 11;
    // setInterval(function () {
    //     var axisData = (new Date()).toLocaleTimeString().replace(/^\D*/, '');
    //
    //     var data0 = option.series[0].data;
    //     var data1 = option.series[1].data;
    //     data0.shift();
    //     data0.push(Math.round(Math.random() * 1000));
    //     data1.shift();
    //     data1.push((Math.random() * 10 + 5).toFixed(1) - 0);
    //
    //     option.xAxis[0].data.shift();
    //     option.xAxis[0].data.push(axisData);
    //     // option.xAxis[1].data.shift();
    //     // option.xAxis[1].data.push(app.count++);
    //
    //     myChart.setOption(option);
    // }, 2100);

    if (option && typeof option === 'object') {
        myChart.setOption(option);
    }
}