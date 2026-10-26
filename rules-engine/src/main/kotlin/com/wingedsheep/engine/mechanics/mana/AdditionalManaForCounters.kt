package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaForEntryCounters
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

/**
 * Finds the [AdditionalManaForEntryCounters] static (Chorus of the Conclave) that lets [casterId]
 * pay "any amount of mana" as an additional cost while casting a given card.
 *
 * Shared by the cast enumerator (to offer the choice), the cast handler (to validate and charge
 * it), so the two can't disagree about when the payment is legal. The ability works only while its
 * source is on the battlefield under the caster's control with its abilities intact (projected
 * controller, no ability loss), and only for a spell its filter matches — evaluated against the
 * card being cast.
 */
object AdditionalManaForCounters {

    /** The grant that applies to [cardId] cast by [casterId], or null when none does. */
    fun applicableGrant(
        state: GameState,
        casterId: EntityId,
        cardId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): AdditionalManaForEntryCounters? {
        val projected = state.projectedState
        for (permanentId in projected.getBattlefieldControlledBy(casterId)) {
            if (projected.hasLostAllAbilities(permanentId)) continue
            val def = state.getEntity(permanentId)?.faceUpCardDefinition(cardRegistry) ?: continue
            for (ability in def.script.staticAbilities) {
                if (ability !is AdditionalManaForEntryCounters) continue
                val matches = predicateEvaluator.matches(
                    state, projected, cardId, ability.spellFilter,
                    PredicateContext(controllerId = casterId, sourceId = permanentId)
                )
                if (matches) return ability
            }
        }
        return null
    }
}
