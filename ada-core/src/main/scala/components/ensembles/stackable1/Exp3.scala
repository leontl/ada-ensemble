package ada.ensembles

import breeze.stats.mode

import ada._
import ada.interface._
import ada.components.selectors._
import ada.components.distributions._

class Exp3Ensemble[ModelID, ModelData, ModelAction, AggregateReward <: Exp3Reward]
    (models: Map[ModelID, StackableModel[ModelID, ModelData, ModelAction]],
    modelRewards: Map[ModelID, AggregateReward],
    protected[ada] var gamma: Double)
    extends StackableEnsemble1[ModelID, ModelData, ModelAction, AggregateReward](models, modelRewards)
    with Exp3[ModelID, ModelData, ModelAction]{
    
    var k: Int = models.keys.toList.length

    def actWithID(data: ModelData, selectedIds: List[ModelID]): (ModelAction, List[ModelID]) = {
        _actImpl[AggregateReward](models, modelRewards , 1.0, data, selectedIds, gamma, k)
    }

    override def update(modelIds: List[ModelID], data: ModelData, reward: Reward): Unit = {
                                            //this variable comes from the Exp3 Actor Trait
        val probability = (1.0-gamma)*reward.value/totalReward.value + gamma/k
        modelRewards(modelIds(0)).update(new Reward(reward.value/probability))
        models(modelIds.head).update(modelIds.tail, data, reward)
    }
}

