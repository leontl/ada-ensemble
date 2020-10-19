package epsilon.demos

import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.sql.{Dataset, Row}
import org.apache.spark.ml.feature.VectorAssembler

import epsilon.models.{LinearRegressionEE, LinearRegressionModelEE}
import epsilon.ensembles.{EpsilonEnsembleThompsonSamplingLocal, BetaDistribution}
import epsilon.generators.AutoregressionGenerator
import plotting.Chart

import org.apache.spark.sql.SparkSession

object ThompsonSamlingOnSpark{
  def tuplify(list: List[Double]): List[(Double, Double, Double, Double)] = {
  {
    for{
      a <- 3 until list.length
    } yield((list(a), list(a-1), list(a-2), list(a-3)))
  }.toList
  }

  val spark: SparkSession = SparkSession.builder.master("local").appName("TemperatureViewer").getOrCreate()
  import spark.implicits._

  val assembler =  new VectorAssembler().setInputCols(Array("-3", "-2", "-1"))
                                              .setOutputCol("features")

  val lr1 = new LinearRegressionEE()
  lr1.setFeaturesCol("features")
    .setLabelCol("target")
    .setElasticNetParam(0.8)
    .setFitIntercept(true)
  
  val lr2 = new LinearRegressionEE()
  lr2.setFeaturesCol("features")
    .setLabelCol("target")
    .setElasticNetParam(0.8)
    .setFitIntercept(false)

  val lr3 = new LinearRegressionEE()
  lr3.setFeaturesCol("features")
    .setLabelCol("target")
    .setElasticNetParam(0.3)

  var dataRun: List[Double] = List.fill(15)(1000.0)
  var dataTuples = tuplify(dataRun)
  var data1 = assembler.transform(Seq(dataTuples.takeRight(20):_*).toDF("-3", "-2", "-1", "target"))

  val models: List[LinearRegressionModelEE] = List(lr1.fitEE(data1),
                                              lr2.fitEE(data1),
                                              lr3.fitEE(data1))



  val evaluationFn = (action: Double, correctAction: Double) => math.max(0.0, (20.0-math.pow(action-correctAction, 2))/20)
  val ensemble = EpsilonEnsembleThompsonSamplingLocal[Int, Vector, Double](
                                                               models.zipWithIndex.toMap.map{case(k,v) => (v, k)},
                                                               (prior, rew) => {prior.update(rew); prior},
                                                               evaluationFn,
                                                               100, 100)

  val generator = new AutoregressionGenerator(3, 0.2)

  def run(): Unit = {
      var next = 0.0
      var i = 0
      val incr = math.max(1, math.ceil(1000.0/150.0).toInt*2)
      var rewards: List[List[String]] = List.fill(models.length)(List())
      var selectedModels: List[String] = List()
      while(i < 1000){
          print(i)

          data1 = assembler.transform(Seq(dataTuples.takeRight(20+i):_*).toDF("-3", "-2", "-1", "target"))
    
          next = generator.next
          dataTuples = tuplify(dataRun)

          val (action, selectedModel) = ensemble.actWithID(Vectors.dense(dataRun.takeRight(3).toArray))
          if (i % incr == 0) {
              rewards = rewards.zipWithIndex.map{case(r, i) => {
                  val rewardString = (ensemble.getModelRewardsMap(i).draw *100).toInt.toString
                  if (rewardString.length == 1) " " + rewardString :: r
                  else rewardString :: r
                  }
              }
              selectedModels = selectedModel.toString :: selectedModels            
          }

          dataRun = dataRun :+ next
          ensemble.update(selectedModel, action, next)
          //ensemble.learn(-999, next, aw => true)

          i = i + 1
      }
      println("\n" + Chart(dataRun.max.toInt, dataRun.min.toInt, 0, dataRun.length).plotLine(dataRun, "M").render())
      println("rewards")
      println(rewards.map(r => r.reverse.mkString("")).mkString("\n"))
      println("selected model")
      println(" " + selectedModels.reverse.mkString(" "))
      println("model frequencies")
      println(selectedModels.reverse.groupBy(identity).view.map{case(k,v) => (k, v.size)}.toMap.toList.sortWith(_._2 > _._2).map{case(a,b) => s"Model $a -> $b"}.mkString("\n"))
      spark.close()

  }

}

