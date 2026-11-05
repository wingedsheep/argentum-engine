package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.sdk.model.EntityId

/** Shared bookkeeping for declared blocks and blocks created by resolving effects. */
internal object BlockingRelationships {
    /**
     * @param enteredBlockers blockers put onto the battlefield blocking (CR 509.4): they are
     *   blocking, but for triggers and effects they never "blocked", so none of the blocker-side
     *   "blocked this combat / turn" history is stamped on them. The attacker side is unchanged —
     *   it did become blocked by them.
     */
    fun establish(
        state: GameState,
        pairs: Map<EntityId, List<EntityId>>,
        enteredBlockers: Set<EntityId> = emptySet(),
    ): GameState {
        var result = state
        val projected = state.projectedState
        for ((blocker, attackers) in pairs) {
            val existing = result.getEntity(blocker)?.get<BlockingComponent>()?.blockedAttackerIds.orEmpty()
            val entered = blocker in enteredBlockers
            result = result.updateEntity(blocker) { c ->
                val blocking = c.with(BlockingComponent((existing + attackers).distinct()))
                if (entered) return@updateEntity blocking
                val partners = c.get<CombatPartnersThisTurnComponent>() ?: CombatPartnersThisTurnComponent()
                blocking.with(BlockedThisCombatComponent).with(BlockedThisTurnComponent)
                    .with(
                        partners.copy(
                            blockedIds = partners.blockedIds + attackers,
                            partnerSnapshots = partners.partnerSnapshots + partnerSnapshots(state, attackers)
                        )
                    )
            }
            val blockerRef = state.objectRef(blocker)
            for (attacker in attackers) {
                result = result.updateEntity(attacker) { c ->
                    val blockers = c.get<BlockedComponent>()?.blockerIds.orEmpty()
                    val history = c.get<BlockersThisCombatComponent>()?.blockers.orEmpty()
                    val partners = c.get<CombatPartnersThisTurnComponent>() ?: CombatPartnersThisTurnComponent()
                    val controller = projected.getController(attacker)
                    var updated = c.with(BlockedComponent((blockers + blocker).distinct()))
                        .with(
                            partners.copy(
                                blockedByIds = partners.blockedByIds + blocker,
                                controllerWhenBlockedBy = if (controller == null) partners.controllerWhenBlockedBy
                                else partners.controllerWhenBlockedBy + (blocker to controller),
                                partnerSnapshots = partners.partnerSnapshots + partnerSnapshots(state, listOf(blocker))
                            )
                        )
                    if (blockerRef != null) updated = updated.with(BlockersThisCombatComponent(history + blockerRef))
                    updated
                }
            }
        }
        return result
    }

    /**
     * Each partner's characteristics as they stand at the pairing — the frozen record
     * `BlockPairedWithMatchingThisTurn` reads, so "blocked or was blocked by a Zombie" still
     * answers after an embalmed Zombie token has ceased to exist.
     */
    private fun partnerSnapshots(state: GameState, partners: List<EntityId>) =
        com.wingedsheep.engine.state.components.stack.captureEntitySnapshots(partners, state.projectedState, state)
            .map { snapshot ->
                snapshot.copy(
                    wasToken = state.getEntity(snapshot.entityId)
                        ?.has<com.wingedsheep.engine.state.components.identity.TokenComponent>() == true
                )
            }
}
