package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * A card entering the battlefield as an Aura it isn't printed as — "It's an Aura enchantment with
 * enchant [enchant] and \"…\", and it loses all other abilities"
 * (`PutOntoBattlefieldAsAuraEffect`). Carried on `ZoneEntryOptions.asAura` so the zone transition
 * makes the object that Aura as it arrives, before it is attached or recorded as an entering
 * permanent, so "a creature entered" observers never see it as a creature.
 */
data class AuraEntry(
    val enchant: GameObjectFilter,
    val grantedStaticAbilities: List<StaticAbility> = emptyList(),
    val grantedAbilities: List<ActivatedAbility> = emptyList(),
) {
    /**
     * The card as the Aura it will be — Enchantment — Aura, keeping its supertypes — for questions
     * asked before it moves (which hosts its protection-relevant characteristics allow).
     */
    fun preview(card: CardComponent): CardComponent = card.copy(
        typeLine = TypeLine(
            supertypes = card.typeLine.supertypes,
            cardTypes = setOf(CardType.ENCHANTMENT),
            subtypes = setOf(Subtype("Aura")),
        )
    )

    /**
     * Make the arrived [entityId] this Aura for as long as it stays on the battlefield: layer 4
     * sets its card types and subtypes (CR 205.1a — the new types replace the old; supertypes, name and
     * colors are untouched), layer
     * 6 removes its abilities, and it has [enchant] as its whole enchant ability plus the granted
     * abilities. The printed statics and replacement effects the entry just registered are dropped
     * outright — this object never has them — so they can't ride the self-sourced ability removal's
     * exemption. The grants are recorded after the removal (a later timestamp), so they survive it.
     */
    fun apply(state: GameState, entityId: EntityId, controllerId: EntityId): GameState {
        val context = EffectContext(sourceId = entityId, controllerId = controllerId)
        var newState = state.updateEntity(entityId) {
            it.without<ContinuousEffectSourceComponent>()
                .without<ReplacementEffectSourceComponent>()
                .with(GainedEnchantRestrictionComponent(filter = enchant))
        }
        val affected = setOf(entityId)
        val conversion = listOf(
            Layer.TYPE to SerializableModification.SetCardTypes(setOf(CardType.ENCHANTMENT.name)),
            Layer.TYPE to SerializableModification.SetAllSubtypes(setOf("Aura")),
            Layer.ABILITY to SerializableModification.RemoveAllAbilities,
        ).map { (layer, modification) ->
            val (id, withId) = newState.newEntity()
            newState = withId
            newState.createFloatingEffect(layer, modification, affected, Duration.Permanent, context, id = id)
        }
        newState = newState.addFloatingEffects(conversion).tick()
        val timestamp = newState.timestamp
        return newState.copy(
            grantedStaticAbilities = newState.grantedStaticAbilities + grantedStaticAbilities.map {
                GrantedStaticAbility(entityId, it, Duration.Permanent, sourceId = entityId,
                    controllerId = controllerId, layerTimestamp = timestamp)
            },
            grantedActivatedAbilities = newState.grantedActivatedAbilities + grantedAbilities.map {
                GrantedActivatedAbility(entityId, it, Duration.Permanent, sourceId = entityId)
            },
        )
    }
}
