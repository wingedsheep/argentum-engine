package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLeftGameEvent
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerLeftGameComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

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
 * - **CR 800.4f–h (a choice the leaver was mid-way through making)** — a pending decision
 *   addressed to the leaver is abandoned: the decision is cleared, the continuation frames
 *   waiting on it are dropped, and priority goes to the active player (CR 117.3b, redirected
 *   if they too have left). Almost every such decision belongs to the leaver's own spell or
 *   ability, which has just left the game with them, so the resolution simply ends; a "may
 *   pay" prompt is answered by not paying (800.4f). The one divergence is a choice another
 *   player's object asked the leaver to make (800.4g — the controller should pick a
 *   substitute chooser): that resolution ends early too. Without this the table deadlocks —
 *   only the leaver could ever answer, and every other seat's pass is refused while a
 *   decision is pending.
 *
 * Deliberately not modelled here (documented simplifications of the deeper CR 800.4
 * sub-rules, none of which the current corpus exercises): firing the remaining players'
 * "leaves the battlefield" triggers off these mass removals (CR 800.4a vs 800.4d — the
 * leaver's own triggers must never fire), and exiling an object the leaver controlled via a
 * *static* ability on a permanent owned by another player (CR 800.4c). Floating control
 * effects — the common case (theft like Control Magic, "gain control until end of turn") —
 * are handled.
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

        // 3b. CR 800.4e — combat damage that would be assigned to a player who has left isn't.
        //     Creatures attacking the leaver (or a planeswalker / battle of theirs, which has just
        //     left too) are removed from combat: nothing is left for them to hit, and leaving
        //     them "attacking" a seat that is gone would still fire lifelink and
        //     "deals combat damage to a player" triggers off a dead life total in the damage step.
        s = removeAttackersAimedAt(s, leaver, toRemove)

        // 4. Remove the objects from the game entirely.
        for (id in toRemove) {
            s = s.removeEntity(id)
        }

        // 5. End the continuous effects that depended on a departed object. Only those: the
        //    effect of a spell or ability the leaver already *resolved* persists (CR 611.2c) — a
        //    Giant Growth on someone else's creature outlives its caster's concession — so nothing
        //    is dropped for having the leaver as its controller. What ends is an effect whose
        //    duration is tied to its source staying on the battlefield (611.2b/611.3) when that
        //    source just left, or one that lasts while the leaver controls the affected object,
        //    which they no longer do. The leaver's "until your next turn" effects are neither —
        //    CR 800.4m has them last until that turn would have begun (TurnManager.endTurn).
        s = s.copy(
            floatingEffects = s.floatingEffects.filterNot { fe ->
                val sourceLeft = fe.sourceId != null && fe.sourceId in toRemove
                when (fe.duration) {
                    is Duration.WhileSourceOnBattlefield,
                    is Duration.WhileYouControlSource,
                    is Duration.WhileSourceTapped,
                    is Duration.WhileSourceTappedAndAffectedPowerAtMostSource,
                    is Duration.WhileYouControlSourceAndSourceTapped,
                    Duration.WhileSourceAttachedToAffected -> sourceLeft
                    Duration.WhileControlledByController -> fe.controllerId == leaver
                    else -> false
                }
            }
        )

        // 6. A decision only the leaver could answer can never be answered now — abandon it
        //    (CR 800.4f–h; see the class KDoc for what that means for the paused resolution).
        s = abandonLeaversDecision(s, leaver)

        // 7. If the leaver (or any departed player) holds priority, hand it to the next
        //    player still in the game. withPriority performs the redirect (CR 800.4a).
        val priorityHolder = s.priorityPlayerId
        if (priorityHolder != null &&
            s.getEntity(priorityHolder)?.has<PlayerLostComponent>() == true
        ) {
            s = s.withPriority(priorityHolder)
        }

        // 8. Mark the leave processing done so the SBA loop never re-applies it.
        s = s.updateEntity(leaver) { it.with(PlayerLeftGameComponent) }

        return ExecutionResult.success(
            s,
            listOf(PlayerLeftGameEvent(leaver, reason, toRemove.size))
        )
    }

    /**
     * Abandon a pending decision addressed to [leaver]. The decision is cleared, every
     * continuation frame is dropped (the frames beneath the one waiting on this decision
     * belong to the same paused resolution — or to triggers deferred behind it — and none of
     * them can be resumed without the answer), and priority returns to the active player as
     * it would after a completed resolution (CR 117.3b). A decision addressed to anyone else
     * is left alone: the game is still waiting on a player who is still here.
     */
    private fun abandonLeaversDecision(state: GameState, leaver: EntityId): GameState {
        val pending = state.pendingDecision ?: return state
        if (pending.playerId != leaver) return state
        return state
            .clearPendingDecision()
            .copy(continuationStack = emptyList())
            .withPriority(state.activePlayerId)
    }

    /**
     * Remove from combat every remaining attacker whose declared defender is [leaver] or one of
     * the [removed] objects (their planeswalkers and battles). Their own attackers are already
     * gone, so this only ever touches other players' creatures.
     */
    private fun removeAttackersAimedAt(state: GameState, leaver: EntityId, removed: Set<EntityId>): GameState {
        var s = state
        for (id in s.getBattlefield()) {
            val defender = s.getEntity(id)?.get<AttackingComponent>()?.defenderId ?: continue
            if (defender == leaver || defender in removed) {
                s = CombatRemovalHelper.removeFromCombat(s, id)
            }
        }
        return s
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
