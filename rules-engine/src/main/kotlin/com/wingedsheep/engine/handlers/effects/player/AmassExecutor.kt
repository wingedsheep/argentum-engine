package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.AmassContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AmassEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Resolves [AmassEffect] — "amass [subtype] N" (CR 701.47).
 *
 * 1. If the controller controls no Army creature, create a 0/0 black [subtype] Army token first.
 * 2. The controller chooses an Army they control (no prompt when they have exactly one — including
 *    the token just created); [AmassContinuation] handles the choice when several Armies exist.
 * 3. Put N +1/+1 counters on the chosen Army and make it the subtype if it isn't already.
 *
 * Token creation and counter placement compose the existing [CreateTokenEffect] / AddCounters
 * executors via the injected [executeEffect]; the counter/subtype back half lives in
 * [AmassResolution] so the single-army path and the continuation share it.
 */
class AmassExecutor(
    private val executeEffect: (GameState, Effect, EffectContext) -> EffectResult,
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AmassEffect> {

    override val effectType: KClass<AmassEffect> = AmassEffect::class

    override fun execute(
        state: GameState,
        effect: AmassEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val subtype = effect.subtype
        val amount = amountEvaluator.evaluate(state, effect.amount, context)

        val armies = controlledArmies(state, controllerId)

        // No Army → create a 0/0 black [subtype] Army token, then amass onto it (now the sole Army).
        if (armies.isEmpty()) {
            val createResult = executeEffect(
                state,
                CreateTokenEffect(
                    power = 0,
                    toughness = 0,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf(subtype, ARMY),
                    name = "$subtype Army"
                ),
                context
            )
            if (createResult.error != null) return createResult

            val tokenId = controlledArmies(createResult.state, controllerId).firstOrNull()
                ?: return EffectResult.success(createResult.state, createResult.events)

            val applied = AmassResolution.applyToArmy(
                createResult.state, tokenId, controllerId, subtype, amount, context.sourceId, executeEffect
            )
            // Preserve the AmassedArmy pipeline entry produced by AmassResolution so a
            // composite "Amass X. Then [effect using amassed Army's power]" still threads
            // the just-amassed token into the follow-up effect. Merge both halves' pipeline
            // state (the token-creation step doesn't stash any today, but if it ever does we
            // mustn't drop it); `applied` wins on key conflict so the amass slot is authoritative.
            return applied.copy(
                events = createResult.events + applied.events,
                updatedCollections = createResult.updatedCollections + applied.updatedCollections,
                updatedSubtypeGroups = createResult.updatedSubtypeGroups + applied.updatedSubtypeGroups
            )
        }

        // Exactly one Army → no choice to make.
        if (armies.size == 1) {
            return AmassResolution.applyToArmy(
                state, armies.single(), controllerId, subtype, amount, context.sourceId, executeEffect
            )
        }

        // Multiple Armies → the controller chooses which one is amassed (CR 701.47a).
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Amass"
        val decisionId = UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Amass ${subtype}s — choose an Army you control to put the counters on",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = armies,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )
        val continuation = AmassContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            subtype = subtype,
            amount = amount,
            sourceId = context.sourceId,
            candidates = armies
        )
        val newState = state.withPendingDecision(decision).pushContinuation(continuation)
        return EffectResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun controlledArmies(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(controllerId)
            .filter { projected.isCreature(it) && projected.hasSubtype(it, ARMY) }
    }

    private companion object {
        const val ARMY = "Army"
    }
}
