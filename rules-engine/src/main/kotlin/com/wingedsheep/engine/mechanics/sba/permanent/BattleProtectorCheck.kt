package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.BattleProtectorChoiceContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.battle.Battles
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * CR 704.5w / 704.5x — keep every battle's protector legal (CR 310.8).
 *
 * A battle's protector is chosen *as it enters* (CR 310.8a), but a battle can reach the
 * battlefield by paths that never ask: reanimated, blinked, put onto the battlefield by an
 * effect. Rather than bolt an entry choice onto each of those pipelines, the engine relies on the
 * two state-based actions the rules provide for exactly this gap, which also cover the cases where
 * a legal protector stops being one mid-game (the protector leaves the game, a Siege changes
 * controller onto its own protector):
 *
 *  - **704.5w** — a battle with no protector designated, and no creatures currently attacking it,
 *    gets one chosen by its controller from the players its battle type allows.
 *  - **704.5x** — a Siege whose controller is also its protector gets a new protector chosen from
 *    its controller's opponents (CR 310.11a: only an opponent may protect a Siege).
 *
 * In both cases, if no player can be chosen the battle is put into its owner's graveyard.
 *
 * When exactly one player is eligible — every two-player game — the choice is forced, so it is
 * applied silently rather than surfaced as a decision the player cannot influence. With two or more
 * candidates the SBA loop pauses on a [ChooseOptionDecision], the same way
 * [CommanderZoneChoiceCheck] does, and only one battle is resolved per pass.
 */
class BattleProtectorCheck : StateBasedActionCheck {
    override val name = "704.5w/x Battle Protector"
    override val order = SbaOrder.BATTLE_PROTECTOR

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            if (!Battles.isBattle(newState, entityId)) continue

            val protector = Battles.protectorOf(newState, entityId)
            val eligible = Battles.eligibleProtectors(newState, entityId)
            val needsChoice = when {
                // 704.5w — no protector at all. Skipped while the battle is being attacked, so a
                // protector who leaves mid-combat doesn't hand the battle to someone new.
                protector == null -> !isBeingAttacked(newState, entityId)
                // 704.5x — a Siege may not be protected by its own controller, and more generally
                // 704.5w requires the protector to still be a legal one for the battle's type.
                else -> protector !in eligible
            }
            if (!needsChoice) continue

            if (eligible.isEmpty()) {
                val result = SbaZoneMovementHelper.putPermanentInGraveyard(newState, entityId, cardComponent)
                newState = result.newState
                events.addAll(result.events)
                continue
            }

            if (eligible.size == 1) {
                newState = ProtectorAssignment.assign(newState, entityId, eligible.single())
                continue
            }

            val chooserId = newState.projectedState.getController(entityId) ?: continue
            val decisionId = "battle-protector-${entityId.value}"
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = chooserId,
                prompt = "Choose a player to protect ${cardComponent.name}",
                context = DecisionContext(
                    sourceId = entityId,
                    sourceName = cardComponent.name,
                    phase = DecisionPhase.STATE_BASED
                ),
                options = eligible.map { playerNameOf(newState, it) }
            )
            val continuation = BattleProtectorChoiceContinuation(
                decisionId = decisionId,
                battleId = entityId,
                candidateIds = eligible
            )
            return ExecutionResult.paused(
                newState.pushContinuation(continuation).withPendingDecision(decision),
                decision,
                events
            )
        }

        return ExecutionResult.success(newState, events)
    }

    /** True if any creature is currently attacking [battleId] (the CR 704.5w carve-out). */
    private fun isBeingAttacked(state: GameState, battleId: EntityId): Boolean =
        state.getBattlefield().any {
            state.getEntity(it)?.get<AttackingComponent>()?.defenderId == battleId
        }

    private fun playerNameOf(state: GameState, playerId: EntityId): String =
        state.getEntity(playerId)?.get<PlayerComponent>()?.name ?: "Player ${playerId.value}"
}

/**
 * Writes a battle's protector (CR 310.8f — a battle has only one protector at a time, and the
 * previous one stops being it the moment another player becomes it). Shared by
 * [BattleProtectorCheck] and its decision resumer so both take the same single path.
 */
object ProtectorAssignment {
    fun assign(state: GameState, battleId: EntityId, protectorId: EntityId): GameState =
        state.updateEntity(battleId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.battlefield.ProtectorComponent(protectorId)
            )
        }
}
