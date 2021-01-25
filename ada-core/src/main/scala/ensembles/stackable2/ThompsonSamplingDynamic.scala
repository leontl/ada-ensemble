package ada.core.ensembles

import breeze.stats.mode
import io.circe.Json

import ada._
import ada.core.interface._
import ada.core.components.selectors._
import ada.core.components.distributions._




class ThompsonSamplingDynamicLocal
    [ModelID, ModelData, ModelAction, ContextualDistr <: ContextualDistribution[ModelData]]
    (models: Map[ModelID, StackableModel[ModelID, ModelData, ModelAction]],
     modelRewards: Map[ModelID, ContextualDistr])
    extends GreedyDynamicEnsemble[ModelID, ModelData, ModelAction, ContextualDistr](
        key => models(key),
        () => models.keys.toList,
        modelRewards,
        0.0
    )