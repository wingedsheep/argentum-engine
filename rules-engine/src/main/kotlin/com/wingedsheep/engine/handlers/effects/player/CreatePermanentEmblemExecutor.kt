package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect
import kotlin.reflect.KClass

/**
 * Executor for [CreatePermanentEmblemEffect].
 *
 * Static-ability emblems are modeled as a set of [Duration.Permanent] floating effects whose
 * source is a synthetic "emblem" entity carrying the controller and any chosen creature type.
 * The synthetic source is placed in [GameState.entities] but never registered in a zone, so it
 * stays inert except as a target for filter resolution. Each modification (P/T bonus, granted
 * keywords) becomes its own floating effect with a `dynamicGroupFilter` so the projector
 * re-evaluates the affected entities every projection — letting later-entering creatures pick
 * up the bonus automatically.
 */
class CreatePermanentEmblemExecutor : EffectExecutor<CreatePermanentEmblemEffect> {

    override val effectType: KClass<CreatePermanentEmblemEffect> = CreatePermanentEmblemEffect::class

    override fun execute(
        state: GameState,
        effect: CreatePermanentEmblemEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId

        // Build the synthetic emblem source entity. It carries the controller (so "you control"
        // filters resolve correctly) and the chosen creature type (so chosenSubtypeKey filters
        // find a ChosenCreatureTypeComponent).
        val emblemId = EntityId.generate()
        var emblemContainer: ComponentContainer = ComponentContainer.EMPTY
            .with(ControllerComponent(controllerId))
        if (effect.groupFilter.chosenSubtypeKey != null) {
            val chosenType = context.chosenCreatureType
                ?: return EffectResult.error(
                    state,
                    "Emblem references chosenSubtypeKey but no creature type was chosen"
                )
            emblemContainer = emblemContainer.with(ChosenCreatureTypeComponent(chosenType))
        }

        var newState = state.withEntity(emblemId, emblemContainer)
        val emblemContext = context.copy(sourceId = emblemId)

        // Power/toughness modification (Layer 7c).
        if (effect.powerBonus != 0 || effect.toughnessBonus != 0) {
            newState = newState.addFloatingEffect(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(
                    powerMod = effect.powerBonus,
                    toughnessMod = effect.toughnessBonus
                ),
                affectedEntities = emptySet(),
                duration = Duration.Permanent,
                context = emblemContext,
                sublayer = Sublayer.MODIFICATIONS,
                dynamicGroupFilter = effect.groupFilter
            )
        }

        // One floating effect per granted keyword (Layer 6 — abilities).
        for (keyword in effect.grantedKeywords) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.GrantKeyword(keyword),
                affectedEntities = emptySet(),
                duration = Duration.Permanent,
                context = emblemContext,
                dynamicGroupFilter = effect.groupFilter
            )
        }

        return EffectResult.success(newState)
    }
}
