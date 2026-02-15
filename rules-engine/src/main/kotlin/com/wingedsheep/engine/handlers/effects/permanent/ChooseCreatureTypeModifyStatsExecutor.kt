package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeModifyStatsEffect
import com.wingedsheep.sdk.scripting.Duration
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseCreatureTypeModifyStatsEffect.
 *
 * "Creatures of the creature type of your choice get +X/+Y until end of turn."
 *
 * This executor:
 * 1. Evaluates dynamic amounts (e.g., X value) at resolution time
 * 2. Presents a ChooseOptionDecision with all creature types
 * 3. Pushes a ChooseCreatureTypeModifyStatsContinuation with resolved integer values
 */
class ChooseCreatureTypeModifyStatsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ChooseCreatureTypeModifyStatsEffect> {

    override val effectType: KClass<ChooseCreatureTypeModifyStatsEffect> =
        ChooseCreatureTypeModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Evaluate dynamic amounts at resolution time
        val resolvedPower = amountEvaluator.evaluate(state, effect.powerModifier, context)
        val resolvedToughness = amountEvaluator.evaluate(state, effect.toughnessModifier, context)

        // If creature type was already chosen during casting, apply directly
        if (context.chosenCreatureType != null) {
            return applyCreatureTypeModifyStats(
                state, context.chosenCreatureType, controllerId, context.sourceId, sourceName,
                resolvedPower, resolvedToughness, effect.duration
            )
        }

        // Otherwise, pause for decision at resolution time (fallback)
        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes
        )

        val continuation = ChooseCreatureTypeModifyStatsContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            creatureTypes = allCreatureTypes,
            powerModifier = resolvedPower,
            toughnessModifier = resolvedToughness,
            duration = effect.duration
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    companion object {
        private val stateProjector = StateProjector()

        fun applyCreatureTypeModifyStats(
            state: GameState,
            chosenType: String,
            controllerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            powerModifier: Int,
            toughnessModifier: Int,
            duration: Duration
        ): ExecutionResult {
            val affectedEntities = mutableSetOf<EntityId>()
            val events = mutableListOf<GameEvent>()

            // Use projected state to check subtypes, so type-changing continuous effects
            // (e.g., Mistform Dreamer becoming a Cleric) are taken into account
            val projected = stateProjector.project(state)

            for (entityId in state.getBattlefield()) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue
                if (!projected.hasSubtype(entityId, chosenType)) continue

                affectedEntities.add(entityId)
                events.add(
                    StatsModifiedEvent(
                        targetId = entityId,
                        targetName = cardComponent.name,
                        powerChange = powerModifier,
                        toughnessChange = toughnessModifier,
                        sourceName = sourceName ?: "Unknown"
                    )
                )
            }

            if (affectedEntities.isEmpty()) {
                return ExecutionResult.success(state, emptyList())
            }

            val floatingEffect = ActiveFloatingEffect(
                id = EntityId.generate(),
                effect = FloatingEffectData(
                    layer = Layer.POWER_TOUGHNESS,
                    sublayer = Sublayer.MODIFICATIONS,
                    modification = SerializableModification.ModifyPowerToughness(
                        powerMod = powerModifier,
                        toughnessMod = toughnessModifier
                    ),
                    affectedEntities = affectedEntities
                ),
                duration = duration,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                timestamp = System.currentTimeMillis()
            )

            val newState = state.copy(
                floatingEffects = state.floatingEffects + floatingEffect
            )

            return ExecutionResult.success(newState, events)
        }
    }
}
