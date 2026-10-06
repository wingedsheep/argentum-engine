package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.TurnsFaceUpInsteadComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * "Instead it's turned face up and assigns or deals damage, is dealt damage, or becomes tapped" —
 * the replacement a [TurnsFaceUpInsteadComponent] puts on a face-down permanent (Illusionary Mask).
 *
 * It is a replacement effect (the card's ruling: it doesn't use the stack), and it modifies the
 * event rather than cancelling it: the permanent is turned face up and *then* the tap or the
 * damage happens to — or is assigned and dealt by — the face-up permanent. So the face-up
 * permanent's own characteristics are the ones the rest of the event sees: its real power when it
 * assigns combat damage, its deathtouch or lifelink when it deals damage, its toughness and
 * protection when it is dealt damage.
 *
 * Three read sites, one per event the card names:
 *  - **becomes tapped** — the [com.wingedsheep.engine.core.tap] atom, the single chokepoint for a
 *    permanent becoming tapped. An already-tapped permanent can't become tapped (CR 701.26a), so it
 *    doesn't turn face up either; nor does a permanent *entering* tapped, which never becomes
 *    tapped.
 *  - **assigns combat damage / is dealt combat damage** — the combat-damage step. A creature
 *    turns face up as it would assign combat damage (CR 510.1), before assignment, so it assigns
 *    its real power; one that assigns none — 0 or less power (CR 510.1a), or blocked with no
 *    blockers left (CR 510.1c) — doesn't. A recipient turns face up once the assignment is fixed
 *    and before the damage is dealt (CR 510.2).
 *  - **deals or is dealt non-combat damage** — `DamageUtils.dealDamageToTarget`. The amount is
 *    already fixed by the time damage would be dealt (a fight reads its power first), so turning
 *    face up never changes how much is dealt.
 *
 * Ordering against other replacement and prevention effects (CR 616.1): the affected object's
 * controller chooses the order, and every choice is legal (CR 616.1e). The engine always applies
 * this one after redirection and before every prevention effect, and then re-evaluates the rest
 * against the face-up permanent (CR 616.1f) — so a face-up creature's own protection or damage
 * replacement applies to the damage that turned it face up.
 */
object FaceUpInstead {

    /** Whether [entityId] is a face-down permanent that hasn't been turned face up since the rider. */
    fun applies(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        return container.has<TurnsFaceUpInsteadComponent>() && container.has<FaceDownComponent>()
    }

    /**
     * Turn [entityId] face up as the replacement, returning the [TurnFaceUpEvent] it emits — or
     * the unchanged state and null when the rider doesn't apply. "Whenever a permanent is turned
     * face up" abilities trigger off that event as usual; abilities relating to the permanent
     * entering the battlefield don't (CR 708.8).
     */
    fun turnFaceUp(state: GameState, entityId: EntityId): Pair<GameState, TurnFaceUpEvent?> {
        if (!applies(state, entityId)) return state to null
        val container = state.getEntity(entityId)!!
        val rider = container.get<TurnsFaceUpInsteadComponent>()!!
        val controllerId = state.projectedState.getController(entityId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: return state to null
        val cardName = container.get<CardComponent>()?.name ?: "Unknown"
        val newState = state.updateEntity(entityId) { c ->
            var updated = removeRider(c.without<FaceDownComponent>().without<FaceDownModeComponent>())
            rider.faceUpStatics?.let { updated = updated.with(it) }
            rider.faceUpReplacements?.let { updated = updated.with(it) }
            updated
        }
        return newState to TurnFaceUpEvent(entityId, cardName, controllerId)
    }

    /** [turnFaceUp] over several permanents at once — the simultaneous combat-damage batch. */
    fun turnFaceUpAll(state: GameState, entityIds: Iterable<EntityId>): Pair<GameState, List<GameEvent>> {
        var current = state
        val events = mutableListOf<GameEvent>()
        for (id in entityIds) {
            val (next, event) = turnFaceUp(current, id)
            current = next
            event?.let(events::add)
        }
        return current to events
    }

    /**
     * Put the rider on a face-down permanent, baking the face-up static components the replacement
     * installs later (see [TurnsFaceUpInsteadComponent]).
     */
    fun stamp(container: ComponentContainer, staticAbilityHandler: StaticAbilityHandler): ComponentContainer {
        val bare = container.without<ContinuousEffectSourceComponent>().without<ReplacementEffectSourceComponent>()
        val faceUp = staticAbilityHandler.addReplacementEffectComponent(
            staticAbilityHandler.addContinuousEffectComponent(bare)
        )
        return container.with(
            TurnsFaceUpInsteadComponent(
                faceUpStatics = faceUp.get<ContinuousEffectSourceComponent>(),
                faceUpReplacements = faceUp.get<ReplacementEffectSourceComponent>(),
            )
        )
    }

    /**
     * Drop the rider from a permanent being turned face up some other way — "has not been turned
     * face up" ends it for good, so a permanent later turned face down again doesn't regain it.
     */
    fun removeRider(container: ComponentContainer): ComponentContainer =
        container.without<TurnsFaceUpInsteadComponent>()
}
