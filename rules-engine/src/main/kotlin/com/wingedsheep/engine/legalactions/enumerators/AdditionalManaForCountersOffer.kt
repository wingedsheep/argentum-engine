package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.mana.AdditionalManaForCounters
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AdditionalManaForEntryCounters
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

/**
 * Marks cast offers that may carry the optional "pay any amount of mana" additional cost of an
 * [AdditionalManaForEntryCounters] static the caster controls (Chorus of the Conclave).
 *
 * Applied once over every enumerated action rather than inside each cast enumerator, because the
 * grant applies to a spell however it is being cast — from hand, from a graveyard or exile
 * permission, with an alternative cost — and every one of those offers is a `CastSpell`. Face-down
 * casts are left alone (the handler refuses the payment there; the grant's filter can't be read off
 * a hidden card).
 */
internal object AdditionalManaForCountersOffer {

    fun annotate(context: EnumerationContext, actions: List<LegalAction>, predicateEvaluator: PredicateEvaluator): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId
        // Cheap gate: nothing to do unless the player controls a permanent printing the static.
        val anyGrant = state.projectedState.getBattlefieldControlledBy(playerId).any { id ->
            state.faceUpCardDefinition(id, context.cardRegistry)?.script?.staticAbilities
                ?.any { it is AdditionalManaForEntryCounters } == true
        }
        if (!anyGrant) return actions

        val availableMana by lazy {
            context.manaSolver.getAvailableManaCount(state, playerId, context.availableManaSources)
        }
        return actions.map { legal ->
            val cast = legal.action as? CastSpell ?: return@map legal
            if (cast.castFaceDown) return@map legal
            AdditionalManaForCounters.applicableGrant(state, playerId, cast.cardId, context.cardRegistry, predicateEvaluator = predicateEvaluator)
                ?: return@map legal
            val baseCost = legal.manaCostString?.let { runCatching { ManaCost.parse(it).cmc }.getOrNull() } ?: 0
            legal.copy(maxAdditionalManaForCounters = (availableMana - baseCost).coerceAtLeast(0))
        }
    }
}
