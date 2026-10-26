package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for BecomeCreatureTypeEffect.
 *
 * "Target creature becomes the creature type of your choice until end of turn."
 *
 * This executor:
 * 1. Resolves the target creature
 * 2. Presents a ChooseOptionDecision with all creature types
 * 3. Pushes a BecomeCreatureTypeContinuation for the next step
 */
class BecomeCreatureTypeExecutor : EffectExecutor<BecomeCreatureTypeEffect> {

    override val effectType: KClass<BecomeCreatureTypeEffect> = BecomeCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeCreatureTypeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state.tick())

        // Target must still be on the battlefield
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state.tick())
        }

        // Read the target's current characteristics, not its card: a face-down permanent is a
        // 2/2 creature with no subtypes (CR 708.2), whatever card it is.
        val projected = state.projectedState
        if (!projected.isCreature(targetId)) {
            return EffectResult.success(state.tick())
        }

        val allCreatureTypes = if (effect.excludedTypes.isNotEmpty()) {
            Subtype.ALL_CREATURE_TYPES.filter { it !in effect.excludedTypes }
        } else {
            Subtype.ALL_CREATURE_TYPES
        }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Prefill search with the target's first creature subtype
        val defaultSearch = projected.getSubtypes(targetId)
            .firstOrNull { it in allCreatureTypes }

        val decision = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes,
            defaultSearch = defaultSearch
        ) }

        val continuation = BecomeCreatureTypeContinuation(
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            targetId = targetId,
            creatureTypes = allCreatureTypes,
            duration = effect.duration
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }
}
