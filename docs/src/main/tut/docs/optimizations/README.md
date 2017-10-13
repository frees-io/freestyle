---
layout: docs
title: Optimizations
permalink: /docs/optimizations/
---

## Optimizations

Freestyle provides several optimizations that result in more performant runtimes for applications using `Free`.

# Faster ops dispatching

Freestyle optimizes operations dispatched in the generated `FunctionK` handlers to increase ops throughput.
Traditional Scala hand written `FunctionK` handlers are often times implemented simply with pattern matching where each case is considered with constructor based patterns as in the example below:

```scala
val handler = new (Op ~> Option) {
 override def apply[A](fa: Op[A]): Option[A] = fa match {
    case Op1(x) => ...
    case Op2(x) => ...
    case Op3(x) => ...
 }
}
```

Freestyle is able to replace this pattern style with a JVM `@scala.annotation.switch` that operates over a synthetic index in the generated ADTs.
This results in a performance boost of about 5x faster ADT evaluation when using Freestyle Handlers over traditional handcoded `FunctionK`.

This is specially noticeable in apps where each algebras have a long list of operations.

The graph below shows a comparison of evaluating a simple program both with `cats.free.Free + cats.data.EitherK + cats.arrow.FunctionK` using hand rolled pattern matching vs `Freestyle`.
Higher values are higher throughput.

<canvas id="bench-functionk" width="400" height="400"></canvas>

Not only Freestyle optimizes the evaluation of `FunctionK` to interpret algebras but it also covers the case where `cats.data.EitherK` degrades in performance as the number of Algebras increases.
`cats.data.EitherK` is a binary type constructor that when used to combine Algebras as in [Datatypes a la Carte](http://www.cs.ru.nl/~W.Swierstra/Publications/DataTypesALaCarte.pdf) suffers from performance
degradation as it is a general purpose datatype not optimized for composing monads. This is because as algebras are nested in one side of the Coproduct, getting down to finding the right one is not a constant time operation in the resulting Tree/LinkedList type of structure.
Freestyle Coproduct implementation based on [iota](https://github.com/47deg/iota) keeps algebras indexed so when accessed as part of the Coproduct they are found in constant time regardless of the number of algebras your application is composed of.

<canvas id="bench-coproduct" width="400" height="400"></canvas>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.5.0/Chart.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/1.11.3/jquery.min.js">
</script>
<script src="//cdnjs.cloudflare.com/ajax/libs/numeral.js/2.0.6/numeral.min.js"></script>
<script src="http://underscorejs.org/underscore-min.js">
</script>
<script>

    Chart.defaults.global.defaultFontColor = '#fff';
    Chart.defaults.global.defaultFontFamily = 'pragmatapro';
                      
    function renderFunctionKGraph() {
        $.getJSON( 'bench-functionk.json', function( data ) {       
                  var ctx = document.getElementById('bench-functionk');
                  var catsData = _.filter(data, function(d){ return d.benchmark.endsWith('cats'); })
                  var freestyleData = _.filter(data, function(d){ return d.benchmark.endsWith('freestyle'); })
                  var labels = _.map(freestyleData, function(d){ return d.benchmark.split(".")[1].replace(/_/g, ' ').trim(); })
                  var myChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'cats.free.Free ops/sec',
                            data: _.map(catsData, function(d){ return Math.ceil(d.primaryMetric.score); }),
                            borderWidth: 1,
                            borderColor: 'rgba(241, 250, 140, 1)',
                            backgroundColor: 'rgba(241, 250, 140, 0.2)'
                        },{
                            label: 'freestyle ops/sec',
                            data: _.map(freestyleData, function(d){ return Math.ceil(d.primaryMetric.score); }),
                            borderWidth: 1,
                            borderColor: 'rgba(139, 233, 253, 1)',
                            backgroundColor: 'rgba(139, 233, 253, 0.2)'
                        }]
                    },
                    options: {
                        scales: {
                            yAxes: [{
                                ticks: {
                                   beginAtZero: true,
                                   callback: function(value, index, values) {
                                     return numeral(value).format('0a');
                                   }
                                }
                            }]
                        }
                    }
                  });
        });
    }
    
    function renderCoproductGraph() {
        $.getJSON( 'bench-coproduct.json', function( rawData ) {       
                  var ctx = document.getElementById('bench-coproduct');
                  var data = rawData.sort(function(a, b) { return a.n - b.n }); //.filter(function(d) { return d.n <= 40 });
                  var catsData = data.filter(function(d) { return d.impl == "cats" })
                  var iotaData = data.filter(function(d) { return d.impl == "iota" })
                  var labels = _.map(iotaData, function(d){ return d.n; })
                  var myChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'cats.data.Coproduct ops/sec',
                            data: _.map(catsData, function(d){ return Math.ceil(d.score.value); }),
                            borderWidth: 1,
                            borderColor: 'rgba(241, 250, 140, 1)',
                            backgroundColor: 'rgba(241, 250, 140, 0.2)'
                        },{
                            label: 'iota ops/sec',
                            data: _.map(iotaData, function(d){ return Math.ceil(d.score.value); }),
                            borderWidth: 1,
                            borderColor: 'rgba(139, 233, 253, 1)',
                            backgroundColor: 'rgba(139, 233, 253, 0.2)'
                        }]
                    },
                    options: {
                        scales: {
                            yAxes: [{
                                ticks: {
                                   beginAtZero: true,
                                   callback: function(value, index, values) {
                                     return numeral(value).format('0a');
                                   }
                                }
                            }]
                        }
                    }
                  });
        });
    }
    
    $( document ).ready(function() {
        renderFunctionKGraph();
        renderCoproductGraph();
    });
</script>
