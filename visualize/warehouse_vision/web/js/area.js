var areaChart;
var areaOption;
var rawdata = [
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
];

function area_init() {
    chartDom = document.getElementById("divArea");
    areaChart = echarts.init(chartDom);
    areaOption = {
        color: ['#80FFA5', '#37A2FF', '#FF0087', '#FFBF00'],
        legend: {
          data: ['Idling', 'Moving', 'Carrying', 'Queuing']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            boundaryGap: false,
            show: false,
          }
        ],
        yAxis: [
          {
            type: 'value'
          }
        ],
        series: [
          {
            name: 'Idling',
            z: 7, 
            type: 'line',
            // stack: 'Total',
            smooth: true,
            lineStyle: {
              width: 0
            },
            showSymbol: false,
            areaStyle: {
              opacity: 0.8,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgb(128, 255, 165)'
                },
                {
                  offset: 1,
                  color: 'rgb(1, 191, 236)'
                }
              ])
            },
            emphasis: {
              focus: 'series'
            },
          },
          {
            name: 'Moving',
            z: 6, 
            type: 'line',
            // stack: 'Total',
            smooth: true,
            lineStyle: {
              width: 0
            },
            showSymbol: false,
            areaStyle: {
              opacity: 0.8,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgb(55, 162, 255)'
                },
                {
                  offset: 1,
                  color: 'rgb(116, 21, 219)'
                }
              ])
            },
            emphasis: {
              focus: 'series'
            },
          },
          {
            name: 'Carrying',
            z: 5, 
            type: 'line',
            // stack: 'Total',
            smooth: true,
            lineStyle: {
              width: 0
            },
            showSymbol: false,
            areaStyle: {
              opacity: 0.8,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgb(255, 0, 135)'
                },
                {
                  offset: 1,
                  color: 'rgb(135, 0, 157)'
                }
              ])
            },
            emphasis: {
              focus: 'series'
            },
          },
          {
            name: 'Queuing',
            z: 4, 
            type: 'line',
            // stack: 'Total',
            smooth: true,
            lineStyle: {
              width: 0
            },
            showSymbol: false,
            label: {
              show: true,
              position: 'top'
            },
            areaStyle: {
              opacity: 0.8,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgb(255, 191, 0)'
                },
                {
                  offset: 1,
                  color: 'rgb(224, 62, 76)'
                }
              ])
            },
            emphasis: {
              focus: 'series'
            },
          }]};
    update_area([1, 1, 1, 1]);
}


function update_area(data) {
    for (let i = 0; i < data.length; ++i) {
        for (let j = 0; j < 9; ++j) {
            rawdata[i][j] = rawdata[i][j + 1];
        }
        rawdata[i][9] = data[i];
        areaOption.series[i].data = JSON.parse(JSON.stringify(rawdata[i]));
        if (i > 0) {
          for (let j = 0; j < 10; ++j) {
            areaOption.series[i].data[j] += areaOption.series[i - 1].data[j]
          }
        }
    }
    areaChart.setOption(areaOption);
}
