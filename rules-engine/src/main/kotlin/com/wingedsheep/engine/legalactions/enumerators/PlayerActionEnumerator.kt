package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.TakePlayerAction
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming

class PlayerActionEnumerator(private val manaOnly: Boolean = false) : ActionEnumerator {
    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val window = ManaPaymentWindow.openFor(state, state.actorFor(context.playerId))
        if (state.pendingDecision != null && (window == null || window.playerId != context.playerId)) return emptyList()
        if (window == null && !com.wingedsheep.engine.event.canTakePlayerActionAtPriority(state, context.playerId)) return emptyList()
        return state.playerActionPermissions.mapNotNull { permission ->
            if (permission.playerId != context.playerId) return@mapNotNull null
            val spec = permission.action
            if ((manaOnly || window != null) && spec.timing != PlayerActionTiming.ManaAbility) return@mapNotNull null
            if (spec.timing == PlayerActionTiming.Sorcery && !context.canPlaySorcerySpeed) return@mapNotNull null
            val affordable = CostPaymentService.canAfford(
                state, context.playerId, spec.cost, permission.context.sourceId ?: permission.playerId,
                context.manaSolver, context.predicateEvaluator,
            )
            LegalAction(
                action = TakePlayerAction(context.playerId, permission.id), actionType = "TakePlayerAction",
                description = spec.actionDescription, affordable = affordable,
                manaCostString = ((spec.cost as? PayCost.Atom)?.atom as? CostAtom.Mana)?.cost?.toString(),
            )
        }
    }
}
