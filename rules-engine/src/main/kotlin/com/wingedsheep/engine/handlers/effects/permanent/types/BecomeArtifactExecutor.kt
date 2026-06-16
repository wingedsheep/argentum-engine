package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeArtifactEffect].
 *
 * Turns a permanent into an artifact (e.g. a Treasure) by stacking floating continuous effects
 * keyed to that entity, mirroring [BecomeCreatureExecutor]:
 *  - Layer 4 (TYPE): SetCardTypes (replace all card types) + SetAllSubtypes (replace all subtypes)
 *  - Layer 5 (COLOR): ChangeColor when [BecomeArtifactEffect.colors] is non-null
 *  - Layer 6 (ABILITY): RemoveAllAbilities when [BecomeArtifactEffect.loseAllAbilities]
 *
 * The optional [BecomeArtifactEffect.grantedAbility] is recorded in
 * [GameState.grantedActivatedAbilities] (NOT as a floating projection effect), so it survives the
 * Layer 6 "lose all abilities" wipe — the legal-action enumerator and mana-ability enumerator read
 * granted activated abilities after the projected `lostAllAbilities` check. That's how Vraska, the
 * Silencer's returned card is a bare Treasure whose only functional ability is the granted sac-for-
 * mana ability.
 */
class BecomeArtifactExecutor : EffectExecutor<BecomeArtifactEffect> {

    override val effectType: KClass<BecomeArtifactEffect> = BecomeArtifactEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeArtifactEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // The permanent must still be on the battlefield to be transformed.
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val affectedEntities = setOf(targetId)
        var newState = state

        // Layer 4 (TYPE): set card types, replacing all existing ones ("loses all other card types").
        newState = newState.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.SetCardTypes(effect.cardTypes),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        // Layer 4 (TYPE): set subtypes, replacing all existing ones (e.g. "Treasure").
        newState = newState.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.SetAllSubtypes(effect.subtypes),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        // Layer 5 (COLOR): set colors (emptySet = colorless).
        effect.colors?.let { colors ->
            newState = newState.addFloatingEffect(
                layer = Layer.COLOR,
                modification = SerializableModification.ChangeColor(colors.map { it.name }.toSet()),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 6 (ABILITY): lose all other abilities.
        if (effect.loseAllAbilities) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.RemoveAllAbilities,
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Grant the single activated ability (survives RemoveAllAbilities — stored separately).
        effect.grantedAbility?.let { ability ->
            newState = newState.copy(
                grantedActivatedAbilities = newState.grantedActivatedAbilities +
                    GrantedActivatedAbility(
                        entityId = targetId,
                        ability = ability,
                        duration = effect.duration
                    )
            )
        }

        return EffectResult.success(newState)
    }
}
