package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.copy.EffectCopyEntry
import com.wingedsheep.engine.handlers.effects.copy.EntryCopyChoice
import com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * An object's characteristics as it would exist on the battlefield — what a replacement effect on
 * its entry judges it by (CR 614.12). Containment Priest's "if a nontoken creature would enter"
 * exiles an artifact that March of the Machines would make a creature, but not a Heliod whose
 * devotion is too low for it to be one, nor a Phyrexian Metamorph entering as a copy of a
 * noncreature artifact.
 *
 * Read off a scratch state with the object placed on the battlefield under [controllerId], its own
 * static abilities installed and any entry copy applied, then projected — so the layer system answers
 * it exactly as it will once the object is there. Callers pass it lazily: only a battlefield-entry
 * replacement that filters on card type ever asks.
 */
object EntryCharacteristics {

    fun wouldBeCreature(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId?,
        entryCopy: EntryCopyChoice?,
        faceDown: Boolean,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        entersWith: com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics? = null,
    ): Boolean {
        // A face-down permanent is a 2/2 creature with no other characteristics (CR 708.2a).
        if (faceDown) return true
        val choice = GrantedEntryCopy.find(state, entityId, predicateEvaluator)?.chosen(entryCopy) ?: entryCopy
        var scratch = choice?.takeIf { it.copiedCard != null }
            ?.let { EffectCopyEntry.apply(state, entityId, it) } ?: state
        val container = scratch.getEntity(entityId) ?: return false
        val controller = controllerId
            ?: container.get<ControllerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
            ?: return false
        scratch.logicalZone(entityId)?.let { scratch = scratch.removeFromZone(it, entityId) }
        val statics = StaticAbilityHandler(cardRegistry)
        scratch = scratch.addToZone(ZoneKey(controller, Zone.BATTLEFIELD), entityId)
            .updateEntity(entityId) { statics.addContinuousEffectComponent(it.with(ControllerComponent(controller))) }
        // The effect's own "enters as a … creature" rider (Dance of the Manse) is part of how it enters.
        entersWith?.let {
            scratch = com.wingedsheep.engine.handlers.effects.zones.CharacteristicsEntry.apply(scratch, entityId, controller, it)
        }
        return scratch.projectedState.isCreature(entityId)
    }
}
