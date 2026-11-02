package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EnteringAttackChoiceContinuation
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.mechanics.combat.AttackedPermanents
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.replacement.PendingReplacementRider
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Makes a creature that a global replacement put onto the battlefield **tapped and attacking**
 * (`PermanentsEnterTapped.attackDefender` — Kira Nerys, Firebrand: "If a creature you control would
 * enter while you control an attacking creature, instead it enters tapped and attacking") an
 * attacking creature, choosing what it attacks per CR 508.4.
 *
 * The entry seams (permanent-spell resolution, [ZoneTransitionService], token minting) tap the
 * entrant themselves and call [enter] once it is on the battlefield:
 * - a noncreature or battle entrant (CR 506.3a / 506.3f), or one whose controller isn't an attacking
 *   player (CR 506.3b — outside combat, or another player's turn), never attacks;
 * - with exactly one legal choice the creature attacks it straight away;
 * - with several, its controller chooses — and an entry seam has nowhere to pause, so the question
 *   is queued as a [PendingReplacementRider] and asked by [ask] when the putting effect ends (or at
 *   the settle boundary), before state-based actions and priority: the rest of the same replacement.
 *   The creature is attacking from the moment it enters — provisionally the first defending player,
 *   re-pointed by the answer — because trigger detection runs on the entry's events as the question
 *   is raised, and an "enters attacking" trigger (Arni Metalbrow) has to see it attacking then.
 *
 * Such a creature is attacking but never "attacked" (CR 508.4), so no attack trigger fires.
 */
object EnteringAttackChoice {

    fun enter(state: GameState, entrantId: EntityId, controllerId: EntityId): GameState {
        if (state.getEntity(entrantId)?.has<AttackingComponent>() == true) return state
        val legal = legalTargets(state, entrantId, controllerId)
        return when (legal.size) {
            0 -> state
            1 -> attack(state, entrantId, legal.single())
            else -> withDefender(state, entrantId, legal.first()).copy(
                pendingReplacementRiders = state.pendingReplacementRiders + PendingReplacementRider(
                    effect = null,
                    hostId = entrantId,
                    controllerId = controllerId,
                    subjectId = entrantId,
                    chooseEnteringAttackTarget = true,
                )
            )
        }
    }

    /**
     * The queued question: re-read the legal choices now and ask only if there is still a choice. A
     * creature that left combat (or the battlefield) in the meantime is left alone.
     */
    fun ask(state: GameState, entrantId: EntityId, controllerId: EntityId): EffectResult {
        if (state.getEntity(entrantId)?.has<AttackingComponent>() != true) return EffectResult.success(state)
        val legal = legalTargets(state, entrantId, controllerId)
        if (legal.size <= 1) return EffectResult.success(answerWith(state, entrantId, legal.singleOrNull()))
        val name = state.getEntity(entrantId)?.get<CardComponent>()?.name ?: "that creature"
        val decision = { decisionId: String ->
            ChooseTargetsDecision(
                id = decisionId,
                playerId = controllerId,
                prompt = "Choose the player, planeswalker or battle $name is attacking",
                context = DecisionContext(
                    sourceId = entrantId,
                    sourceName = name,
                    phase = DecisionPhase.RESOLUTION,
                ),
                targetRequirements = listOf(
                    TargetRequirementInfo(
                        index = 0,
                        description = "player, planeswalker or battle for $name to attack",
                        minTargets = 1,
                        maxTargets = 1,
                    )
                ),
                legalTargets = mapOf(0 to legal),
            )
        }
        return EffectResult.from(
            state.suspendForDecision(decision, EnteringAttackChoiceContinuation(entrantId, controllerId), emptyList())
        )
    }

    /** Apply the controller's answer; a choice that stopped being legal leaves it unattacking (CR 506.3c). */
    fun answer(state: GameState, entrantId: EntityId, controllerId: EntityId, defenderId: EntityId): GameState {
        if (state.getEntity(entrantId)?.has<AttackingComponent>() != true) return state
        return answerWith(state, entrantId, defenderId.takeIf { it in legalTargets(state, entrantId, controllerId) })
    }

    private fun answerWith(state: GameState, entrantId: EntityId, defenderId: EntityId?): GameState =
        if (defenderId == null) state.updateEntity(entrantId) { it.without<AttackingComponent>() }
        else attack(state, entrantId, defenderId)

    private fun legalTargets(state: GameState, entrantId: EntityId, controllerId: EntityId): List<EntityId> {
        if (entrantId !in state.getBattlefield()) return emptyList()
        val projected = state.projectedState
        if (!projected.isCreature(entrantId) || projected.isBattle(entrantId)) return emptyList()
        return CombatDefenders.enteringAttackTargets(state, controllerId)
    }

    private fun withDefender(state: GameState, entrantId: EntityId, defenderId: EntityId): GameState =
        state.updateEntity(entrantId) {
            it.with(AttackingComponent(defenderId, defendingPlayerId = CombatDefenders.defendingPlayerOf(state, defenderId)))
        }

    private fun attack(state: GameState, entrantId: EntityId, defenderId: EntityId): GameState =
        AttackedPermanents.markAttacked(withDefender(state, entrantId, defenderId), defenderId)
}
