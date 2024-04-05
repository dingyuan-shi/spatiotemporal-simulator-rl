{

    let max_num = 6;
    let begin_time = new Date(2016,11,6,4);

    // time for simulator time , data is  [available_driver,order,total_driver]
    function updateNum(time, myData) {
        let dataSet = [myData['avail_driver_num'],myData['order_num'],myData['total_driver_num']];
        console.log(dataSet);
        // console.log(option);
        let data = [
            option.series[0].data,
            option.series[1].data,
            option.series[2].data
        ];

        if (data[0].length < max_num) {
            data[0].push({name: time.toString(), value: [getTimeForm(time),dataSet[0]]})
            data[1].push({name: time.toString(), value: [getTimeForm(time),dataSet[1]]})
            data[2].push({name: time.toString(), value: [getTimeForm(time),dataSet[2]]})

        } else {
            data[0].shift();
            data[0].push({name: time.toString(), value: [getTimeForm(time),dataSet[0]]});
            data[1].shift();
            data[1].push({name: time.toString(), value: [getTimeForm(time),dataSet[1]]});
            data[2].shift();
            data[2].push({name: time.toString(), value: [getTimeForm(time),dataSet[2]]});
        }
        num_chart.setOption(option);
        // console.log(option);
    }


    let dom = document.getElementById("num_container");
    let num_chart = echarts.init(dom);
    let app = {};

    let option;

    function getTimeForm(date) {
        return [[date.getFullYear(), date.getMonth(), date.getDay(),].join('/'),[date.getHours(), date.getMinutes(), date.getSeconds()].join(':')].join(' ')
    }


    // function randomData() {
    //     now = new Date(+now + oneDay);
    //     value = value + Math.random() * 21 - 10;
    //     return {
    //         name: now.toString(),
    //         value: [
    //             [now.getFullYear(), now.getMonth() + 1, now.getDate()].join('/'),
    //             Math.round(value)
    //         ]
    //     };
    // }


    // var now = +new Date(1997, 9, 3);
    // var oneDay = 24 * 3600 * 1000;
    // var value = Math.random() * 1000;
    // for (var i = 0; i < 1000; i++) {
    //     let rand = randomData();
    //     data1.push(rand);
    //     rand = randomData();
    //     rand.value[1] = rand.value[1] * 3;
    //     // console.log(rand);
    //     data2.push(rand);
    //     rand = randomData();
    //     rand.value[1] = rand.value[1] * 6;
    //     // console.log(rand);
    //     data3.push(rand);
    // }

    option = {
        // title: {
        //     text: '车辆/订单统计',
        //     textStyle:{
        //         color:'white'
        //     }
        // },
        legend:{
            orient : 'horizontal',
            textStyle : {
                color: 'rgba(255,255,255,0.6)'
            }
        },
        /*tooltip: {
            trigger: 'cross',
            formatter: function (params) {
                params = params[0];
                let date = new Date(params.name);
                return date.getHours() + ':' + (date.getMinutes() + 1) + ':' + date.getSeconds() + ' : ' + params.value[1];
            },
            axisPointer: {
                animation: false
            }
        },*/
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'cross',
                label: {
                    backgroundColor: '#283b56'
                }
            }
        },
        grid: {
            x: 60,
            y: 25,
            x2: 10,
            y2: 30,
        },
        xAxis: {
            type: 'time',
            splitLine: {
                show: false
            },
            show:false,
            axisLine: {
                lineStyle: {
                    color: 'rgba(255,255,255,0.6)'
                }
            }
        },
        yAxis: {
            type: 'value',
            boundaryGap: [0, '100%'],
            splitLine: {
                show: true,
                lineStyle: {
                    color: 'rgba(255,255,255,0.6)'
                }
            },
            axisLine: {
                lineStyle: {
                    color: 'rgba(255,255,255,0.6)'
                }
            }
        },
        series: [{
            name: 'Idle Drivers',
            type: 'line',
            showSymbol: false,
            hoverAnimation: false,
            data: [{name:begin_time.toString(),value:[getTimeForm(begin_time),0]}],
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 1,
                    colorStops: [{
                        offset: 0, color: 'rgba(84,112,198,0.8)' // 0% 处的颜色
                    }, {
                        offset: 1, color: 'rgba(84,112,198,0)' // 100% 处的颜色
                    }],
                    global: false // 缺省为 false
                },
            }
        },
            {
                name: 'Orders',
                type: 'line',
                showSymbol: false,
                hoverAnimation: false,
                data: [{name:begin_time.toString(),value:[getTimeForm(begin_time),0]}],
                areaStyle: {
                    color: {
                        type: 'linear',
                        x: 0,
                        y: 0,
                        x2: 0,
                        y2: 1,
                        colorStops: [{
                            offset: 0, color: 'rgba(145,205,117,0.7)' // 0% 处的颜色
                        }, {
                            offset: 1, color: 'rgba(145,205,117,0)' // 100% 处的颜色
                        }],
                        global: false // 缺省为 false
                    },
                }
            },
            {
                name: 'Total Drivers',
                type: 'line',
                showSymbol: false,
                hoverAnimation: false,
                data: [{name:begin_time.toString(),value:[getTimeForm(begin_time),0]}],
                areaStyle: {
                    color: {
                        type: 'linear',
                        x: 0,
                        y: 0,
                        x2: 0,
                        y2: 1,
                        colorStops: [{
                            offset: 0, color: 'rgba(250,200,88,0.5)' // 0% 处的颜色
                        }, {
                            offset: 1, color: 'rgba(250,200,88,0)' // 100% 处的颜色
                        }],
                        global: false // 缺省为 false
                    },
                },
            }]
    };

    // setInterval(function () {
    //
    //     for (var i = 0; i < 5; i++) {
    //         let data1 = option.series[0].data;
    //         data1.shift();
    //         data1.push(randomData());
    //         let data2 = option.series[1].data;
    //         data2.shift();
    //         let rand = randomData()
    //         rand.value[1] = rand.value[1] * 3
    //         data2.push(rand);
    //         let data3 = option.series[2].data;
    //         data3.shift();
    //         rand = randomData();
    //         rand.value[1] = rand.value[1] * 6;
    //         data3.push(rand);
    //     }
    //     num_chart.setOption(option);
    //     // console.log(option.series[0].data.slice(0, 4));
    //     // console.log(option.series[1].data.slice(0, 4));
    //     // console.log(option.series[2].data.slice(0, 4));
    //     // myChart.setOption({
    //     //     series: [{
    //     //         data: data1
    //     //     },
    //     //         {
    //     //             data: data2
    //     //         },
    //     //         {
    //     //             data: data3
    //     //         }]
    //     // });
    // }, 1000);

    if (option && typeof option === 'object') {
        num_chart.setOption(option);
    }
}

