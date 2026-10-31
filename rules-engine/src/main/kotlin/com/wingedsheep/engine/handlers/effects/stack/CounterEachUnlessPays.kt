package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterEachUnlessPaysContinuation
import com.wingedsheep.engine.core.CounterUnlessPaysContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterTarget

/**
 * Counter one stack object: a spell into [destination], or an activated/triggered ability off the
 * stack. [CounterTarget.SpellOrAbility] dispatches on what [entityId] actually is. Shared by the
 * single counter and the stack-wide batch.
 */
internal fun counterStackObject(
    state: GameState,
    counterer: SpellCounterer,
    target: CounterTarget,
    destination: CounterDestination,
    entityId: EntityId,
    countererId: EntityId,
    sourceId: EntityId?
): ExecutionResult {
    val asSpell = when (target) {
        CounterTarget.Spell -> true
        CounterTarget.Ability -> false
        CounterTarget.SpellOrAbility -> state.isSpellOnStack(entityId)
    }
    if (!asSpell) return counterer.counterAbility(state, entityId)
    return when (destination) {
        CounterDestination.Graveyard -> counterer.counterSpell(state, entityId, countererId)
        is CounterDestination.Exile -> counterer.counterSpellToExile(state, entityId, destination.grantFreeCast, countererId)
        CounterDestination.Hand -> counterer.counterSpellToHand(state, entityId, countererId)
        is CounterDestination.Library -> counterSpellToLibrary(state, counterer, entityId, destination, countererId, sourceId)
    }
}

/**
 * Drives a [CounterEachUnlessPaysContinuation]: asks each remaining object's controller whether to
 * pay (skipping, as unpaid, any object whose controller can't), and once every object has been
 * decided counters all the unpaid ones in one step — Whirlwind Denial's "then all spells and
 * abilities that weren't paid for are countered at the same time".
 */
internal class CounterEachUnlessPays(
    private val counterer: SpellCounterer,
    private val manaSolver: ManaSolver
) {
    private val decisionHandler = DecisionHandler()

    fun step(state: GameState, frame: CounterEachUnlessPaysContinuation): ExecutionResult {
        val unpaid = frame.unpaid.toMutableList()
        var remaining = frame.remaining
        while (remaining.isNotEmpty()) {
            val objectId = remaining.first()
            remaining = remaining.drop(1)
            if (objectId !in state.stack) continue
            val payer = TargetResolutionUtils.stackObjectController(state, objectId) ?: continue
            if (!manaSolver.canPay(state, payer, frame.manaCost)) {
                unpaid += objectId
                continue
            }
            val withBatch = state.pushContinuation(frame.copy(remaining = remaining, unpaid = unpaid.toList()))
            val kind = if (state.isSpellOnStack(objectId)) "spell" else "ability"
            val objectName = stackObjectName(state, objectId)
            return decisionHandler.createYesNoDecision(
                state = withBatch,
                playerId = payer,
                sourceId = frame.sourceId,
                sourceName = frame.sourceName,
                prompt = "Pay ${frame.manaCost} to prevent your $kind${objectName?.let { " ($it)" } ?: ""} from being countered?",
                yesText = "Pay ${frame.manaCost}",
                noText = "Don't pay",
                answer = CounterUnlessPaysContinuation(
                    payingPlayerId = payer,
                    spellEntityId = objectId,
                    manaCost = frame.manaCost,
                    sourceId = frame.sourceId,
                    sourceName = frame.sourceName,
                    counterDestination = frame.counterDestination,
                    controllerId = frame.controllerId,
                    deferCounter = true
                )
            )
        }
        return counterAll(state, unpaid, frame)
    }

    private fun stackObjectName(state: GameState, objectId: EntityId): String? {
        val container = state.getEntity(objectId) ?: return null
        return container.get<ActivatedAbilityOnStackComponent>()?.sourceName
            ?: container.get<TriggeredAbilityOnStackComponent>()?.sourceName
            ?: container.get<CardComponent>()?.name
    }

    private fun counterAll(
        state: GameState,
        unpaid: List<EntityId>,
        frame: CounterEachUnlessPaysContinuation
    ): ExecutionResult {
        var current = state
        val events = mutableListOf<GameEvent>()
        for (objectId in unpaid) {
            if (objectId !in current.stack) continue
            val result = counterStackObject(
                current, counterer, frame.target, frame.counterDestination,
                objectId, frame.controllerId, frame.sourceId
            )
            if (result.error != null) return result
            events += result.events
            current = result.state
            // EachOnStack forbids a choice-bearing destination, so nothing here can pause.
            check(result.outcome !is Outcome.Paused) { "Batch counter paused on $objectId" }
        }
        return ExecutionResult.success(current, events)
    }
}
