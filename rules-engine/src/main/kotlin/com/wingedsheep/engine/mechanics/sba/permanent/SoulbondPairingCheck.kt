package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.CreaturesUnpairedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PairedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * CR 702.95e — "A paired creature becomes unpaired if any of the following occur: another player
 * gains control of it or the creature it's paired with; it or the creature it's paired with stops
 * being a creature; or it or the creature it's paired with leaves the battlefield."
 *
 * Soulbond's "for as long as both remain creatures on the battlefield under your control" is the
 * pairing state's own lifetime rather than a continuous effect's duration, so it is checked here
 * instead of by `EndedDurationExpiryCheck`. Breaking a pair is one-way (CR 611.2b in spirit): a
 * creature that stops being a creature and then becomes one again is not re-paired.
 *
 * This check **tidies up and announces**; it is not what makes a pair stop counting. CR 702.95e is a
 * continuous fact, and an SBA only runs before a player gets priority — too late for a creature that
 * leaves and re-enters inside a single resolution. So [SoulbondPairing] re-checks the link on every
 * read (see its class note) and this pass follows behind to drop the components and emit
 * [CreaturesUnpairedEvent]. Both halves are dropped together, keeping `PairedComponent` symmetric.
 *
 * The *leaving* half of a zone change is already stripped by `ZoneMovementUtils`; what this check
 * adds is the half left behind, plus the two CR 702.95e cases that involve no zone change at all and
 * therefore need projected state to see — a controller change, and a permanent that stops being a
 * creature (a de-animated Deadeye Navigator, or a partner that lost its creature types).
 */
class SoulbondPairingCheck : StateBasedActionCheck {
    override val name = "702.95e Soulbond Unpairing"
    override val order = SbaOrder.SOULBOND_UNPAIRING

    override fun check(state: GameState): ExecutionResult {
        val battlefield = state.getBattlefield()
        // Collect first, mutate after: unpairing walks to the partner, and both halves must be
        // judged against the same pre-mutation state.
        val toUnpair = mutableSetOf<EntityId>()
        val events = mutableListOf<GameEvent>()

        for (entityId in battlefield) {
            val partnerId = state.getEntity(entityId)?.get<PairedComponent>()?.partnerId ?: continue
            if (entityId in toUnpair) continue
            if (pairStillHolds(state, entityId, partnerId)) continue

            toUnpair.add(entityId)
            toUnpair.add(partnerId)
            events.add(
                CreaturesUnpairedEvent(
                    entityId = entityId,
                    entityName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "Creature",
                    formerPartnerId = partnerId
                )
            )
        }

        if (toUnpair.isEmpty()) return ExecutionResult.success(state)

        var newState = state
        for (entityId in toUnpair) {
            if (newState.getEntity(entityId) == null) continue
            newState = newState.updateEntity(entityId) { it.without<PairedComponent>() }
        }
        return ExecutionResult.success(newState, events)
    }

    /**
     * Does the pair between [entityId] and [partnerId] still satisfy CR 702.95e? Both halves must be
     * creatures on the battlefield under the *same* controller — "another player gains control of
     * either" is exactly the two projected controllers ceasing to agree, so one comparison covers a
     * gained-control effect on either half.
     *
     * The battlefield-presence and symmetry half is [SoulbondPairing.partnerOf], the same read every
     * consumer of the pairing uses, so this check can never disagree with what the rest of the engine
     * already believes. What it adds is the projection-dependent half: creature-ness comes from
     * projected types (CR 613), so a de-animated or type-stripped half breaks the pair while a
     * non-creature permanent that *became* a creature keeps it.
     */
    private fun pairStillHolds(state: GameState, entityId: EntityId, partnerId: EntityId): Boolean {
        if (SoulbondPairing.partnerOf(state, entityId) != partnerId) return false
        if (!state.projectedState.isCreature(entityId)) return false
        if (!state.projectedState.isCreature(partnerId)) return false
        val controller = state.projectedState.getController(entityId) ?: return false
        return state.projectedState.getController(partnerId) == controller
    }
}
