package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * An Aura card entering the battlefield as a non-Aura enchantment — "return this card to the
 * battlefield as a non-Aura enchantment. It loses \"enchant creature\" and gains \"…\""
 * (`PutOntoBattlefieldAsNonAuraEffect`, Takklemaggot). The mirror of [AuraEntry]: carried on
 * `ZoneEntryOptions.asNonAura` so the object is no Aura from the moment it arrives, and the
 * unattached-Aura state-based action (CR 704.5m) never sees it unattached.
 */
data class NonAuraEntry(
    val grantedTriggeredAbilities: List<TriggeredAbility> = emptyList(),
) {
    /**
     * Make the arrived [entityId] a non-Aura for as long as it stays on the battlefield: layer 4
     * drops the Aura subtype (its card types and any other subtypes stay), and with it the enchant
     * ability only an Aura uses. Its printed abilities stay; the granted triggered abilities are
     * added on top.
     */
    fun apply(state: GameState, entityId: EntityId, controllerId: EntityId): GameState {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return state
        val context = EffectContext(sourceId = entityId, controllerId = controllerId)
        val subtypes = card.typeLine.subtypes.map { it.value }.filterNot { it == "Aura" }.toSet()
        val (id, withId) = state.newEntity()
        val retype = withId.createFloatingEffect(
            Layer.TYPE, SerializableModification.SetAllSubtypes(subtypes), setOf(entityId),
            Duration.Permanent, context, id = id
        )
        val newState = withId.addFloatingEffects(listOf(retype)).tick()
        return newState.copy(
            grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grantedTriggeredAbilities.map {
                GrantedTriggeredAbility(entityId, it, Duration.Permanent, sourceId = entityId)
            }
        )
    }
}
