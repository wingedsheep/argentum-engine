package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone

/**
 * "Exile this card instead of putting it anywhere else any time it would leave the stack" — the
 * stack-side clause shared by flashback (CR 702.34a), aftermath (CR 702.127a) and jump-start
 * (CR 702.133a). Every stack exit that isn't a resolution or a counter (those already honour the
 * graveyard-bound [AfterResolveDestinationComponent]) asks here before moving a spell: a bounce to
 * its owner's hand, a put-into-library, a generic zone move off the stack.
 */
object StackExitRider {

    /**
     * The zone [container]'s spell must go to instead of wherever it was headed as it leaves the
     * stack, or null when nothing overrides the move. Either an [AfterResolveDestinationComponent]
     * armed for [AfterResolveDestinationComponent.everyStackExit] (aftermath, a granted jump-start),
     * or a spell cast from the graveyard by paying its flashback cost.
     */
    fun overrideZone(container: ComponentContainer): Zone? {
        container.get<AfterResolveDestinationComponent>()?.takeIf { it.everyStackExit }?.let { return it.zone }
        val spell = container.get<SpellOnStackComponent>() ?: return null
        return Zone.EXILE.takeIf {
            spell.castFromZone == Zone.GRAVEYARD && spell.alternativeCost == AlternativeCostType.FLASHBACK
        }
    }

    /**
     * Move [spellId] off the stack into its owner's [zone] as the rider directs — the shared move
     * for exits that would otherwise put the spell somewhere positional (a chosen library slot), so
     * they skip that placement entirely. Returns null when [spellId] is no longer on the stack.
     */
    fun moveOffStack(state: GameState, spellId: EntityId, ownerId: EntityId, zone: Zone): Pair<GameState, ZoneChangeEvent>? {
        if (spellId !in state.stack) return null
        val name = state.getEntity(spellId)?.get<CardComponent>()?.name ?: "Unknown"
        var newState = com.wingedsheep.engine.mechanics.CastCharacteristics.end(state.removeFromStack(spellId), spellId)
        newState = newState.updateEntity(spellId) { c ->
            // The rider is spent once the card is off the stack, so a later cast from exile starts clean.
            c.without<SpellOnStackComponent>()
                .without<TextReplacementComponent>()
                .without<TargetsComponent>()
                .without<AfterResolveDestinationComponent>()
        }
        newState = newState.addToZone(ZoneKey(ownerId, zone), spellId)
        return newState to ZoneChangeEvent(
            spellId, name, Zone.STACK, zone, ownerId,
            oldObject = state.objectRef(spellId),
            newObject = newState.objectRef(spellId)
        )
    }
}
