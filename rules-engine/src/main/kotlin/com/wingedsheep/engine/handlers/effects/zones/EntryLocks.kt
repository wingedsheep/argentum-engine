package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.CantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.model.EntityId

/**
 * "<Cards> can't enter the battlefield" ([CantEnterTheBattlefield] — Worms of the Earth, Soulless
 * Jailer).
 *
 * Its own object rather than a private helper because the lock is global: it is owned by whichever
 * permanent prints it, not by the card being moved, so every path that puts a card onto the
 * battlefield has to ask the same question. `ZoneTransitionService.moveToZone` asks it for every
 * move; `MoveToZoneEffectExecutor` asks it early too, so a locked card never prompts for an Aura
 * host or an "as this enters" choice it will not use.
 */
object EntryLocks {

    /**
     * True if a permanent on the battlefield forbids [entityId] from entering the battlefield out
     * of [fromZone]. The card is matched where it is — before it moves — so its own
     * characteristics decide (a permanent card in a graveyard, a land in a library).
     */
    fun cantEnter(
        state: GameState,
        entityId: EntityId,
        fromZone: Zone,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        // A move that starts on the battlefield (a same-zone instruction) isn't an entry.
        if (fromZone == Zone.BATTLEFIELD) return false
        for (sourceId in state.getBattlefield()) {
            val container = state.getEntity(sourceId) ?: continue
            if (container.has<FaceDownComponent>()) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                val lock = when (ability) {
                    is CantEnterTheBattlefield -> ability
                    is ConditionalStaticAbility -> {
                        val inner = ability.ability as? CantEnterTheBattlefield ?: continue
                        val controller = state.projectedState.getController(sourceId) ?: continue
                        val context = EffectContext(sourceId = sourceId, controllerId = controller)
                        if (!predicateEvaluator.conditions.evaluate(state, ability.condition, context)) continue
                        inner
                    }
                    else -> continue
                }
                if (lock.fromZones?.contains(fromZone) == false) continue
                val controller = state.projectedState.getController(sourceId) ?: continue
                if (predicateEvaluator.matches(
                        state, state.projectedState, entityId, lock.filter,
                        PredicateContext(controllerId = controller, sourceId = sourceId)
                    )
                ) return true
            }
        }
        return false
    }
}
