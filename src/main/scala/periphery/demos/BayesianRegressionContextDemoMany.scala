package epsilon.demos

import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.{ListBuffer}

import epsilon.core.components.distributions.BayesianRegressionSampleContext
import epsilon.core.ensembles._
import epsilon.core.models.DummyModel


import plotting.Chart

object DemoBayesianRegressionContextMany{
    val nIter = 1000 * 5    
    val nFeatures = 3
    val nModels = 100
    val nGoodModels = 10


    val rnd = scala.util.Random

    val models = (0 until nModels).map(x => new DummyModel(x.toDouble))
    val contexts = (0 until nModels).map(x => new BayesianRegressionSampleContext(nFeatures, 0.3, 1.0))
    val ensemble = new EpsilonEnsembleGreedySoftmaxLocalWithContext[Int, Array[Double], Unit, Double,  BayesianRegressionSampleContext](
        (0 until nModels).zip(models).toMap,
        MutableMap((0 until nModels).zip(contexts):_*),
        (context, aggregateReward) => aggregateReward.draw(context),
        0.0,
        (action1, action2) => math.exp(action1 - action2),
        (context,aggregateReward, reward) => {aggregateReward.update(context, reward); aggregateReward}
    )



    def getAverages(): List[Double] = {
        val context = Array.fill(nFeatures)(rnd.nextGaussian())
        context(1) += 4
        val selected = ListBuffer.fill(nModels)(ListBuffer.empty[Double])
        var j = 0
        while( j < 100) {
            val (action, id) = ensemble.actWithID(context, ())
            selected.zipWithIndex.map{case(a, i) => selected(i) += (if(i == id) 1.0 else 0.0)}
            j += 1
        }
        selected.map(s => s.sum/s.length).toList
    }



    def run(): Unit = {
        println("started run")
        val shares = ListBuffer.fill(nModels)(ListBuffer.empty[Double])

        var i = 0
        while(i < nIter){
            if(i % scala.math.max(1, (nIter / 100).toInt)  == 0){
                var selections = getAverages()
                shares.zipWithIndex.map{case(s,j) => s += selections(j)}
            }

            //(0 until nModels).map{id =>
            val context = Array.fill(nFeatures)(rnd.nextGaussian())
            val (action, id) = ensemble.actWithID(context, ())
            val k = if(id < nFeatures * nGoodModels) id % nFeatures else 0
            ensemble.update(id, context,  context(k))
            //}

            if(i % 10000 == 0) println(i) 

            i += 1
        }
        println("-----finished loop------")

        var selections= getAverages()
        if( selections.length > 10){
            selections = selections.zipWithIndex
                                    .filter{
                                        case(x, i)if( (i% nFeatures == 1) && i < nGoodModels*nFeatures) => true
                                        case _ => false
                                    }
                                    .map(_._1)

        }
        println("last values")
        selections.zipWithIndex.map{
            case(v, i) => println(f"action$i: $v")
        }


        val characters = "-+abcdefghijklmnopqr"
        var chart = Chart(1.1, -0.1, 0, nIter)
        (0 until nModels).map{i =>
            chart = chart.plotLine(shares(i).toList, Some(i.toString), characters(i%characters.length).toString())
        }
        println(chart.render())


    }
}