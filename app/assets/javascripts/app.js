import Vue from 'vue'
import VueResource from 'vue-resource'
import Highcharts from 'highcharts'

Vue.use(VueResource)

new Vue({
  el: '#container',

  data: {
    chart: '',
    counters: [0, 0]
  },

  methods: {
    onNewEvent: function(event) {
      var values = JSON.parse(event)
      console.log(values)
      this.incrementSerieCounter(values.idx)
      this.$data.chart.series[values.idx].addPoint([values.timestamp, values.value], true, this.shouldAnimate(values.idx))
    },
    incrementSerieCounter: function(idx) {
      this.counters[idx]++
    },
    shouldAnimate: function(idx) {
      return this.counters[idx] > 10
    }
  },

  ready: function() {
    Highcharts.setOptions({
      global: {
        useUTC: false
      }
    })
    this.chart = new Highcharts["Chart"]({
      chart: {
        type: 'spline',
        animation: Highcharts.svg,
        renderTo: this.$el
      },
      title: {
        text: ''
      },
      xAxis: {
        type: 'datetime',
      },
      yAxis: {
        plotLines: [{
          value: 0,
          width: 1,
          color: '#808080'
        }]
      },
      tooltip: {
        formatter: function() {
          return '<b>' + this.series.name + '</b><br/>' +
            Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) + '.' + new Date(this.x).getMilliseconds() + '<br/>' + Highcharts.numberFormat(this.y, 2)
        }
      },
      legend: {
        enabled: false
      },
      exporting: {
        enabled: false
      },
      series: [{
        name: 'Temperature'
      }, {
        name: 'Humidity'
      }]
    })

    var comp = this
    var feed = new EventSource("http://localhost:9000/sse")
    feed.addEventListener('message', function(event) {
      comp.onNewEvent(event.data)
    })
    feed.addEventListener('error', function(e) {
      feed.close()
    }, false)

  }
})
