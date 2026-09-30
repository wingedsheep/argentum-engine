package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DredgeComponent
import com.wingedsheep.engine.state.components.identity.GrantsDredgeToGraveyardCardsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith

/**
 * Supplies graveyard dredge abilities to the ordinary replacement-choice pipeline: printed dredge
 * ([DredgeComponent]) and dredge granted by a permanent the drawing player controls
 * ([GrantsDredgeToGraveyardCardsComponent], "land cards in your graveyard have dredge 2").
 *
 * Each dredge ability a card has is a separate option (CR 616.1), identified by its index: printed
 * abilities first, then grants in battlefield order.
 */
internal object DredgeReplacements {
    private val drawPattern = EventPattern.DrawEvent()

    fun gather(
        state: GameState,
        event: PendingGameEvent,
        context: EffectContext?,
        predicateEvaluator: PredicateEvaluator
    ): List<GatheredReplacement> {
        val player = event.affectedPlayerId
        // Match the individual draw, never the multi-card draw announcement. Other event
        // families return before walking a graveyard, keeping damage/token hot paths cheap.
        if (!event.matches(drawPattern, player, state, context)) return emptyList()
        val graveyard = state.getGraveyard(player)
        if (graveyard.isEmpty()) return emptyList()
        val librarySize = state.getLibrary(player).size
        // "Your graveyard" — only grants controlled by the graveyard's owner reach it, and a
        // granter that has lost all its abilities grants nothing.
        val projected = state.projectedState
        val grants = state.controlledBattlefield(player)
            .filterNot { projected.hasLostAllAbilities(it) }
            .mapNotNull { state.getEntity(it)?.get<GrantsDredgeToGraveyardCardsComponent>() }
        val predicateContext = PredicateContext(controllerId = player)

        val result = mutableListOf<GatheredReplacement>()
        for (id in graveyard) {
            val card = state.getEntity(id) ?: continue
            var index = 0
            fun offer(amount: Int, replacement: ReplaceDrawWith) {
                val abilityIndex = index++
                if (librarySize < amount) return
                result.add(dredgeOption(id, card.get<CardComponent>()?.name, abilityIndex, amount, replacement, player))
            }
            card.get<DredgeComponent>()?.let { dredge ->
                dredge.amounts.forEachIndexed { i, amount -> offer(amount, dredge.replacements[i]) }
            }
            for (grant in grants) {
                grant.grants.forEachIndexed { i, ability ->
                    if (predicateEvaluator.matches(state, projected, id, ability.filter, predicateContext)) {
                        offer(ability.amount, grant.replacements[i])
                    }
                }
            }
        }
        return result
    }

    private fun dredgeOption(
        cardId: EntityId,
        cardName: String?,
        abilityIndex: Int,
        amount: Int,
        replacement: ReplaceDrawWith,
        player: EntityId
    ): GatheredReplacement {
        val name = cardName ?: "this card"
        val cardsToMill = if (amount == 1) "a card" else "$amount cards"
        return GatheredReplacement(
            identity = ReplacementEffectIdentity.CardZoneIdentity(cardId, Zone.GRAVEYARD, abilityIndex),
            effect = replacement,
            sourceControllerId = player,
            description = "$name: Dredge $amount",
            optionalPrompt = "Dredge $amount — Mill $cardsToMill and return $name from your graveyard to your hand instead of drawing?"
        )
    }
}
