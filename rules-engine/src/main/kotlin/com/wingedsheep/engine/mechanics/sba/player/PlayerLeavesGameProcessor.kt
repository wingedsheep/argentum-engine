package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLeftGameEvent
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerLeftGameComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Applies the "leaving the game" processing for a player who has lost (CR 800.4a–c).
 *
 * Unlike a two-player game — where one player losing simply ends the game — a multiplayer
 * pod must continue for the remaining players, with everything the leaver brought to the
 * table removed. This object performs that removal in one shot:
 *
 * - **CR 800.4a** — every object the leaver *owns* leaves the game, in every zone and on
 *   the stack. Any effect giving the leaver control of an object ends (so a creature they
 *   stole reverts to its owner).
 * - **CR 800.4a (stack)** — the leaver's stack objects not represented by cards (their
 *   triggered/activated abilities) cease to exist.
 * - **CR 800.4a (priority)** — if the leaver held priority, it passes to the next player
 *   still in the game (the redirect lives in [GameState.withPriority]).
 *
 * The leaver's *player entity* itself is kept (marked [PlayerLostComponent] +
 * [PlayerLeftGameComponent]) so it remains in [GameState.turnOrder] for history and the
 * game-end SBA. The turn-order iteration helpers already skip players who have left.
 *
 * Deliberately not modelled here (documented simplifications of the deeper CR 800.4
 * sub-rules, none of which the current corpus exercises): firing the remaining players'
 * "leaves the battlefield" triggers off these mass removals (CR 800.4a vs 800.4d — the
 * leaver's own triggers must never fire), delegating a choice a leaver was mid-way through
 * making (CR 800.4g–h), and exiling an object the leaver controlled via a *static* ability
 * on a permanent owned by another player (CR 800.4c). Floating control effects — the common
 * case (theft like Control Magic, "gain control until end of turn") — are handled.
 */
object PlayerLeavesGameProcessor {

    fun process(state: GameState, leaver: EntityId, reason: GameEndReason): ExecutionResult {
        var s = state

        // 1. End any effect granting the leaver control of an object (CR 800.4a). Removing
        //    the floating effect lets the object revert to its owner on the next projection.
        s = s.copy(
            floatingEffects = s.floatingEffects.filterNot { fe ->
                val m = fe.effect.modification
                m is SerializableModification.ChangeController && m.newControllerId == leaver
            }
        )

        // 2. Collect every object the leaver owns (all zones), plus the leaver's stack
        //    abilities that aren't represented by a card — all of which leave the game.
        val ownedObjects = s.entities.keys.filter { id ->
            id != leaver && s.getEntity(id)?.get<CardComponent>()?.ownerId == leaver
        }
        val leaverStackAbilities = s.stack.filter { id ->
            val c = s.getEntity(id) ?: return@filter false
            c.get<CardComponent>() == null && (
                c.get<TriggeredAbilityOnStackComponent>()?.controllerId == leaver ||
                    c.get<ActivatedAbilityOnStackComponent>()?.controllerId == leaver
                )
        }
        val toRemove = (ownedObjects + leaverStackAbilities).toSet()

        // 3. Drop combat references to the departing objects from permanents that remain so
        //    combat can continue without them (CR 800.4: the leaver's attackers and blockers
        //    vanish, the rest of combat proceeds).
        s = clearCombatReferences(s, toRemove)

        // 4. Remove the objects from the game entirely.
        for (id in toRemove) {
            s = s.removeEntity(id)
        }

        // 5. End floating effects the leaver controlled or that were sourced by an object
        //    that just left (their continuous effects end with them — CR 800.4a).
        s = s.copy(
            floatingEffects = s.floatingEffects.filterNot { fe ->
                fe.controllerId == leaver || (fe.sourceId != null && fe.sourceId in toRemove)
            }
        )

        // 6. If the leaver (or any departed player) holds priority, hand it to the next
        //    player still in the game. withPriority performs the redirect (CR 800.4a).
        val priorityHolder = s.priorityPlayerId
        if (priorityHolder != null &&
            s.getEntity(priorityHolder)?.has<PlayerLostComponent>() == true
        ) {
            s = s.withPriority(priorityHolder)
        }

        // 7. Mark the leave processing done so the SBA loop never re-applies it.
        s = s.updateEntity(leaver) { it.with(PlayerLeftGameComponent) }

        return ExecutionResult.success(
            s,
            listOf(PlayerLeftGameEvent(leaver, reason, toRemove.size))
        )
    }

    /**
     * Remove references to [removed] entities from the combat components of permanents that
     * are staying. A blocker that was blocking a departed attacker stops blocking it; an
     * attacker blocked only by departed blockers becomes unblocked.
     */
    private fun clearCombatReferences(state: GameState, removed: Set<EntityId>): GameState {
        if (removed.isEmpty()) return state
        var s = state
        for (id in s.getBattlefield()) {
            if (id in removed) continue
            val container = s.getEntity(id) ?: continue

            container.get<BlockingComponent>()?.let { blocking ->
                val kept = blocking.blockedAttackerIds.filter { it !in removed }
                if (kept.size != blocking.blockedAttackerIds.size) {
                    s = s.updateEntity(id) { c ->
                        if (kept.isEmpty()) c.without<BlockingComponent>()
                        else c.with(BlockingComponent(kept))
                    }
                }
            }

            container.get<BlockedComponent>()?.let { blocked ->
                val kept = blocked.blockerIds.filter { it !in removed }
                if (kept.size != blocked.blockerIds.size) {
                    s = s.updateEntity(id) { c ->
                        if (kept.isEmpty()) c.without<BlockedComponent>()
                        else c.with(BlockedComponent(kept))
                    }
                }
            }
        }
        return s
    }
}
