package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EnteringAttackDefenderContinuation
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Raises the CR 508.4 choice for creature tokens put onto the battlefield attacking under
 * [com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender.CONTROLLER_CHOOSES]: "its controller
 * chooses which defending player, planeswalker a defending player controls, or battle a defending
 * player protects it's attacking as it enters".
 *
 * Every choice is made *before* any token exists, one per token in creation order, so each token
 * enters already attacking its own defender. The answers accumulate on
 * [EffectContext.entryAttackDefenders] and the creating effect is executed again from the top once
 * per answer ([EnteringAttackDefenderContinuation]); nothing about the board changes while a
 * question is open, so each re-execution reaches the same token count and the same choices.
 *
 * A creating effect that names the player ("attacking that player or a planeswalker they control")
 * passes it as `restrictTo`: the choices narrow to that player and the planeswalkers they control.
 */
internal object EnteringAttackDefenderChooser {

    /**
     * The defender for each of [count] tokens, or null when the controller still owes an answer.
     * With no legal defender every token enters unattacking (null entries); with exactly one there
     * is nothing to choose. An answer that stopped being legal leaves its token unattacking
     * (CR 506.3c) rather than redirecting it.
     */
    fun defenders(
        state: GameState,
        context: EffectContext,
        controllerId: EntityId,
        count: Int,
        restrictTo: EntityId? = null,
    ): List<EntityId?>? {
        val legal = legalDefenders(state, controllerId, restrictTo)
        if (legal.size <= 1) return List(count) { legal.firstOrNull() }
        val chosen = context.entryAttackDefenders
        if (chosen.size < count) return null
        return chosen.take(count).map { defender ->
            defender.takeIf { it in legal }
        }
    }

    /** Ask [controllerId] what token number `entryAttackDefenders.size + 1` of [count] attacks. */
    fun pause(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        controllerId: EntityId,
        count: Int,
        tokenName: String,
        restrictTo: EntityId? = null,
    ): EffectResult {
        val legal = legalDefenders(state, controllerId, restrictTo)
        val restricted = restrictTo != null
        val ordinal = context.entryAttackDefenders.size + 1
        val subject = if (count == 1) tokenName else "$tokenName $ordinal of $count"
        val decision = { decisionId: String ->
            ChooseTargetsDecision(
                id = decisionId,
                playerId = controllerId,
                prompt = if (restricted) "Choose the player or planeswalker $subject is attacking"
                    else "Choose the player, planeswalker or battle $subject is attacking",
                context = DecisionContext(
                    sourceId = context.sourceId,
                    sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                    phase = DecisionPhase.RESOLUTION,
                ),
                targetRequirements = listOf(
                    TargetRequirementInfo(
                        index = 0,
                        description = if (restricted) "player or planeswalker for $subject to attack"
                            else "player, planeswalker or battle for $subject to attack",
                        minTargets = 1,
                        maxTargets = 1,
                    )
                ),
                legalTargets = mapOf(0 to legal),
            )
        }
        return EffectResult.from(
            state.suspendForDecision(decision, EnteringAttackDefenderContinuation(effect, context), emptyList())
        )
    }

    /**
     * The CR 508.4 choices for a token [controllerId] puts onto the battlefield attacking — all of
     * them, or with [restrictTo] only that player and the planeswalkers they control.
     */
    private fun legalDefenders(state: GameState, controllerId: EntityId, restrictTo: EntityId?): List<EntityId> {
        val legal = CombatDefenders.enteringAttackTargets(state, controllerId)
        if (restrictTo == null) return legal
        val projected = state.projectedState
        return legal.filter {
            it == restrictTo || (projected.isPlaneswalker(it) && projected.getController(it) == restrictTo)
        }
    }
}
