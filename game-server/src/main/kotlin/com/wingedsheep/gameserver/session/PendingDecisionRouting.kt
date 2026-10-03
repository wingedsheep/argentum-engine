package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.*

/** Change only the browser-facing correlation handle; the engine decision remains untouched. */
internal fun PendingDecision.withClientRoutingId(id: String): PendingDecision = when (this) {
    is PlayCardDecision -> copy(id = id)
    is ChooseTargetsDecision -> copy(id = id)
    is SelectCardsDecision -> copy(id = id)
    is YesNoDecision -> copy(id = id)
    is BatchYesNoDecision -> copy(id = id)
    is ChooseModeDecision -> copy(id = id)
    is ChooseColorDecision -> copy(id = id)
    is ChooseNumberDecision -> copy(id = id)
    is DistributeDecision -> copy(id = id)
    is OrderObjectsDecision -> copy(id = id)
    is SplitPilesDecision -> copy(id = id)
    is ChooseOptionDecision -> copy(id = id)
    is ChooseReplacementDecision -> copy(id = id)
    is AssignDamageDecision -> copy(id = id)
    is SearchLibraryDecision -> copy(id = id)
    is ReorderLibraryDecision -> copy(id = id)
    is SelectManaSourcesDecision -> copy(id = id)
    is BudgetModalDecision -> copy(id = id)
    is CombatResolutionDecision -> copy(id = id)
}
