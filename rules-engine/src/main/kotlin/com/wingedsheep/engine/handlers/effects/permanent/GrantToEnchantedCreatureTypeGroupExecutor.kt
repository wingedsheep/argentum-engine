package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantToEnchantedCreatureTypeGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantToEnchantedCreatureTypeGroupEffect.
 *
 * Resolves the enchanted creature from the source aura's AttachedToComponent,
 * determines its creature subtypes, and applies stat/keyword effects to all
 * creatures on the battlefield that share at least one creature type.
 *
 * Used by the Crown cycle from Onslaught.
 */
class GrantToEnchantedCreatureTypeGroupExecutor : EffectExecutor<GrantToEnchantedCreatureTypeGroupEffect> {

    override val effectType: KClass<GrantToEnchantedCreatureTypeGroupEffect> =
        GrantToEnchantedCreatureTypeGroupEffect::class

    override fun execute(
        state: GameState,
        effect: GrantToEnchantedCreatureTypeGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        // Find the enchanted creature via AttachedToComponent on the source (aura)
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state)
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.success(state)
        val enchantedCreatureId = sourceContainer.get<AttachedToComponent>()?.targetId
            ?: return ExecutionResult.success(state)

        // Get the enchanted creature's subtypes
        val enchantedContainer = state.getEntity(enchantedCreatureId)
            ?: return ExecutionResult.success(state)
        val enchantedCard = enchantedContainer.get<CardComponent>()
            ?: return ExecutionResult.success(state)
        val enchantedSubtypes = enchantedCard.typeLine.subtypes.map { it.value }.toSet()

        if (enchantedSubtypes.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Find all creatures on battlefield sharing at least one creature type
        val events = mutableListOf<EngineGameEvent>()
        val affectedEntities = mutableSetOf<EntityId>()
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        val keyword = effect.keyword

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be a creature (or face-down permanent)
            if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue

            // Check if this creature shares at least one subtype with the enchanted creature
            val creatureSubtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet()
            if (creatureSubtypes.intersect(enchantedSubtypes).isEmpty()) continue

            affectedEntities.add(entityId)

            if (effect.powerModifier != 0 || effect.toughnessModifier != 0) {
                events.add(
                    StatsModifiedEvent(
                        targetId = entityId,
                        targetName = cardComponent.name,
                        powerChange = effect.powerModifier,
                        toughnessChange = effect.toughnessModifier,
                        sourceName = sourceName
                    )
                )
            }

            if (keyword != null) {
                events.add(
                    KeywordGrantedEvent(
                        targetId = entityId,
                        targetName = cardComponent.name,
                        keyword = keyword.displayName,
                        sourceName = sourceName
                    )
                )
            }
        }

        if (affectedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Create floating effects
        val floatingEffects = mutableListOf<ActiveFloatingEffect>()
        val timestamp = System.currentTimeMillis()

        if (effect.powerModifier != 0 || effect.toughnessModifier != 0) {
            floatingEffects.add(
                ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.POWER_TOUGHNESS,
                        sublayer = Sublayer.MODIFICATIONS,
                        modification = SerializableModification.ModifyPowerToughness(
                            powerMod = effect.powerModifier,
                            toughnessMod = effect.toughnessModifier
                        ),
                        affectedEntities = affectedEntities
                    ),
                    duration = effect.duration,
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    controllerId = context.controllerId,
                    timestamp = timestamp
                )
            )
        }

        if (keyword != null) {
            floatingEffects.add(
                ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.ABILITY,
                        sublayer = null,
                        modification = SerializableModification.GrantKeyword(keyword.name),
                        affectedEntities = affectedEntities
                    ),
                    duration = effect.duration,
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    controllerId = context.controllerId,
                    timestamp = timestamp
                )
            )
        }

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffects
        )

        return ExecutionResult.success(newState, events)
    }
}
