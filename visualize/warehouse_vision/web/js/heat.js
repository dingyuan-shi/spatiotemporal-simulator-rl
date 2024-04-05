var heatChart;
var heatOption;

function heat_init(heat_div_h, warehouse_height, warehouse_width) {
    var chartDom = document.querySelector("#heat");
    var unit_len = heat_div_h / warehouse_height * 1.1;
    // heatChart = echarts.init(chartDom, null, {width:warehouse_width * unit_len, height: warehouse_height * unit_len});
    heatChart = echarts.init(chartDom);
    heatOption = {
      grid:{
        left: unit_len * warehouse_width * 0.3,
        top: 0,
      }, 
      xAxis: {
        type: 'category',
        show: false
      },
      yAxis: {
        type: 'category',
        show: false
      },
      visualMap: {
        top: 0,
        min: 0,
        max: 100,
        calculable: true,
        realtime: true,
        inRange: {
          color: [
            '#313695',
            '#4575b4',
            '#74add1',
            '#abd9e9',
            '#e0f3f8',
            '#ffffbf',
            '#fee090',
            '#fdae61',
            '#f46d43',
            '#d73027',
            '#a50026'
          ]
        }
      },
      series: [
        {
          name: 'Gaussian',
          type: 'heatmap',
          emphasis: {
            itemStyle: {
              borderColor: '#333',
              borderWidth: 1
            }
          },
          progressive: 1000,
          animation: false
        }
      ]
    };
    update_heat([[0,0,0]]);
}


function update_heat(data) {
  heatOption.series[0].data = data;
  heatChart.setOption(heatOption);
}
