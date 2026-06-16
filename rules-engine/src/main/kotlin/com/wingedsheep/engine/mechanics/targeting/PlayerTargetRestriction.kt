package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Evaluates the optional [com.wingedsheep.sdk.scripting.targets.TargetPlayer.restriction] /
 * [com.wingedsheep.sdk.scripting.targets.TargetOpponent.restriction] against a single candidate
 * player (CR 115). The candidate is bound to [Player.Candidate] via
 * [EffectContext.candidatePlayerId] so the restriction can read its life total, turn trackers,
 * controlled permanents, etc. through the normal [Condition] vocabulary.
 *
 * This is the *single* place the restriction is interpreted; the three player-target sites
 * (legal-target enumeration in `TargetFinder`, legal-action enumeration in
 * `TargetEnumerationUtils`, and validation/CR-608.2b re-check in `TargetValidator`) all route
 * through here so they can never diverge.
 */
object PlayerTargetRestriction {

    private val conditionEvaluator = ConditionEvaluator()

    /**
     * True if [candidatePlayerId] satisfies [restriction] (or there is no restriction).
     *
     * @param controllerId the player choosing the target — used to resolve `Player.You`/`Opponent`
     *   inside the restriction, so "an opponent who ..." reads relative to the chooser.
     * @param sourceId the targeting source, for source-relative restriction reads.
     */
    fun isSatisfied(
        state: GameState,
        restriction: Condition?,
        candidatePlayerId: EntityId,
        controllerId: EntityId,
        sourceId: EntityId? = null
    ): Boolean {
        if (restriction == null) return true
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            candidatePlayerId = candidatePlayerId
        )
        return conditionEvaluator.evaluate(state, restriction, context)
    }
}
