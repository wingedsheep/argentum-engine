package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
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
 *
 * [BecomeArtifactEffect.grantedStaticAbilities] take a different route, because a static ability
 * has to *project* to do anything (an attached-creature `ModifyStats` must reach Layer 7c of the
 * equipped creature). They are lowered to [ContinuousEffectData] and appended to the permanent's
 * own [ContinuousEffectSourceComponent] — the same channel a printed static ability uses. That
 * placement is what makes them survive the ability wipe: [StateProjector] exempts a source from
 * `lostAllAbilities` suppression when that source is itself the origin of the RemoveAllAbilities
 * effect, which the permanent being transformed here always is. The Irencrag gaining
 * "Equipped creature gets +3/+3" while losing its printed mana ability rides on exactly that.
 */
class BecomeArtifactExecutor(
    private val staticAbilityHandler: StaticAbilityHandler
) : EffectExecutor<BecomeArtifactEffect> {

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

        // Layer 3 (TEXT): overwrite the name (CR 612.8 — "loses any names it had and has only the
        // specified name"). Applied before the type/color layers, matching TransformPermanent.
        effect.name?.let { newName ->
            newState = newState.addFloatingEffect(
                layer = Layer.TEXT,
                modification = SerializableModification.SetName(newName),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 4 (TYPE): set card types, replacing all existing ones ("loses all other card types").
        // Skipped when cardTypes is null — the permanent keeps its existing card types (Ultima's
        // blighted land stays a land and keeps any other card types; only its subtypes are stripped).
        effect.cardTypes?.let { cardTypes ->
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.SetCardTypes(cardTypes),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

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
                        duration = effect.duration,
                        sourceId = context.sourceId
                    )
            )
        }

        // Grant static abilities by appending to the permanent's own continuous-effect source list,
        // so they project through the layer system like printed statics do. Group ids are namespaced
        // per grant: `toGroupedEffectData` numbers them by list index, which would otherwise collide
        // with the ids already baked from the card definition (CR 613.6 locks an affected set per
        // group id, so a collision would silently fuse two unrelated multi-layer abilities).
        if (effect.grantedStaticAbilities.isNotEmpty()) {
            val container = newState.getEntity(targetId)
            if (container != null) {
                val existing = container.get<ContinuousEffectSourceComponent>()?.effects ?: emptyList()
                val namespace = "granted${existing.size}"
                val granted = staticAbilityHandler
                    .lowerToContinuousEffectData(effect.grantedStaticAbilities)
                    .map { data -> data.groupId?.let { data.copy(groupId = "$namespace-$it") } ?: data }
                newState = newState.withEntity(
                    targetId,
                    container.with(ContinuousEffectSourceComponent(existing + granted))
                )
            }
        }

        return EffectResult.success(newState)
    }
}
