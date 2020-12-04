package ada.core.ensembles

import scala.collection.mutable.{Map => MutableMap}

import ada._
import ada.core.interface._
import ada.core.components.selectors._
import ada.core.components.distributions._
import breeze.stats.distributions.Beta


class ThompsonSampling
    [ModelID, ModelData, ModelAction, Distr <: SimpleDistribution]
    (models: ModelID  => StackableModel[ModelID, ModelData, ModelAction],
     modelKeys: () => List[ModelID],
     modelRewards: MutableMap[ModelID, Distr])
    extends GreedySoftmaxEnsemble[ModelID, ModelData, ModelAction, Distr](
        models,
        modelKeys,
        modelRewards,
        0.0
    )

class ThompsonSamplingLocalBeta[ModelID, ModelData, ModelAction]
    (models: Map[ModelID, StackableModel[ModelID, ModelData, ModelAction]],
         alpha: Double,
         beta: Double)
    extends ThompsonSampling[ModelID, ModelData, ModelAction, BetaDistribution](
        key => models(key), () => models.keys.toList, MutableMap(models.keys.map(k => (k, new BetaDistribution(alpha, beta))).toSeq:_*))

