package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.doesntUntapDuringUntapStep
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * The crack-back as the opponent's visible board can make it — not only as its creatures stand.
 *
 * [CombatMath.estimateNextTurnDamage] reads the swing back off the creatures alone: their power
 * against our untapped blockers' toughness. The opponent's turn starts with an untap step, though,
 * and the activated abilities beside those creatures come with it. Game `39e8aac6` turn 29: at 3
 * life the AI sent two chip attackers in, keeping exactly the toughness a 7/7 trampler needed to
 * deal nothing — and Living Library ("{6}, Sacrifice: shuffle target creature an opponent controls
 * into its library") sat across the table beside nine lands that untap. One blocker shuffled away
 * was the game.
 *
 * This reads that: the opponent's board as it will stand on their turn (their permanents untapped,
 * except those that don't untap), and each non-mana activated ability it can then afford that can
 * touch a fight ([CombatReach.FIGHT]: removal, a pump, damage to a creature) or keep one of our
 * blockers out of it ([CombatReach.BLOCK_DENIAL]: "target creature can't block") — the engine's own
 * legal actions, so mana, sacrifice and tap costs are the rules' answer. Each is played out once,
 * on the creatures it can hit, and the crack-back is re-estimated on what it leaves. The caller
 * takes the worst.
 *
 * **Visible information only.** Abilities of permanents on the battlefield and the lands that pay
 * for them — nothing in a hidden hand, library or graveyard.
 *
 * **Cost.** Gated per decision ([hasAnswers], cached on the decision's root): a board with no such
 * ability costs one enumeration per decision. Otherwise at most [MAX_SIMULATIONS] activations per
 * scored plan, and only for a plan the board-only estimate already lets us survive.
 */
internal class CrackBackStress(
    private val simulator: GameSimulator,
    private val cardRegistry: CardRegistry,
) {
    private class Gate(val root: GameState, val viewer: EntityId, val answers: Boolean)

    @Volatile
    private var gate: Gate? = null

    /** Whether some opponent of [viewer] has, at [root], an ability this model would try. */
    fun hasAnswers(root: GameState, viewer: EntityId): Boolean {
        gate?.let { if (it.root === root && it.viewer == viewer) return it.answers }
        val answers = root.getOpponents(viewer).any { opponent ->
            simulator.getPermanentAbilityActions(onTheirTurn(root, opponent), opponent).any { usable(root, it) }
        }
        gate = Gate(root, viewer, answers)
        return answers
    }

    /**
     * The worst [crackBack] any one opponent's answer leaves at [after], plus the life that answer
     * takes on its own; null when no answer could be played out.
     *
     * @param crackBack the caller's estimate of the swing back from a board — the same estimate it
     *   made of [after] itself, so the two are like for like.
     */
    fun worstCrackBack(
        root: GameState,
        after: GameState,
        viewer: EntityId,
        myLife: Int,
        crackBack: (GameState) -> Int,
    ): Int? {
        if (!hasAnswers(root, viewer) || after.gameOver || after.pendingDecision != null) return null
        var worst: Int? = null
        var simulationsLeft = MAX_SIMULATIONS
        for (opponent in after.getOpponents(viewer)) {
            val theirTurn = onTheirTurn(after, opponent)
            for (action in answers(theirTurn, opponent, viewer)) {
                if (simulationsLeft-- <= 0) return worst
                val result = simulator.simulate(theirTurn, action)
                if (result !is SimulationResult.Terminal) continue
                val answered = result.state
                val damage = if (answered.gameOver) {
                    if (answered.winnerId == viewer) 0 else myLife
                } else {
                    (myLife - answered.lifeTotal(viewer)).coerceAtLeast(0) + crackBack(answered)
                }
                if (worst == null || damage > worst) worst = damage
                if (damage >= myLife) return worst
            }
        }
        return worst
    }

    /** [state] as [opponent]'s turn finds it: their permanents untapped, and priority theirs. */
    private fun onTheirTurn(state: GameState, opponent: EntityId): GameState {
        val projected = state.projectedState
        var out = state
        for (id in projected.getBattlefieldControlledBy(opponent)) {
            if (out.getEntity(id)?.has<TappedComponent>() != true) continue
            if (projected.doesntUntapDuringUntapStep(id)) continue
            out = out.updateEntity(id) { it.without<TappedComponent>() }
        }
        return out.copy(priorityPlayerId = opponent)
    }

    /** Concrete activations [opponent] could use at [state], aimed at the creatures they matter to. */
    private fun answers(state: GameState, opponent: EntityId, viewer: EntityId): List<GameAction> {
        val projected = state.projectedState
        val creatures = state.getBattlefield().filter { projected.isCreature(it) }
        val out = LinkedHashSet<GameAction>()
        for (offer in simulator.getPermanentAbilityActions(state, opponent)) {
            if (!usable(state, offer)) continue
            val bound = XCostSelection.bindBestX(state, offer)
            val targeted = if (!bound.requiresTargets) listOf(bound.action) else {
                val infos = TargetSelection.targetInfosFor(bound) ?: continue
                val single = infos.singleOrNull()
                if (single != null && single.maxTargets == 1) {
                    // Our blockers by how much they hold off, their attackers by how hard they hit.
                    val ours = single.validTargets
                        .filter { it in creatures && projected.getController(it) == viewer }
                        .sortedByDescending { projected.getToughness(it) ?: 0 }
                        .take(MAX_TARGETS_PER_SIDE)
                    val theirs = single.validTargets
                        .filter { it in creatures && projected.getController(it) == opponent }
                        .sortedByDescending { projected.getPower(it) ?: 0 }
                        .take(MAX_TARGETS_PER_SIDE)
                    (ours + theirs).map { target ->
                        TargetSelection.applyTargets(
                            bound.action, listOf(TargetSelection.toChosenTarget(state, single, target, opponent)),
                        )
                    }
                } else {
                    listOf(TargetSelection.fillHeuristically(state, bound, opponent, fillPartialRequirements = true))
                }
            }
            for (target in targeted) {
                val action = SelectionCostPayment.withPlainCosts(state, offer, target)
                out += action
                // A repeatable pump is tried at the most the mana allows.
                val repeats = minOf(offer.maxRepeatableActivations ?: 1, MAX_REPEATS)
                val activation = action as? ActivateAbility
                if (activation != null && repeats > 1) out += activation.copy(repeatCount = repeats)
            }
        }
        return out.toList()
    }

    /**
     * An affordable non-mana activation whose effect can change a fight — or keep one of our
     * creatures out of the block that would hold their attack off ([CombatReach.BLOCK_DENIAL]).
     */
    private fun usable(state: GameState, offer: LegalAction): Boolean {
        if (!offer.affordable || offer.isManaAbility || offer.hasUnfillableTargetRequirement) return false
        val activation = offer.action as? ActivateAbility ?: return false
        val reach = CombatResponses.reachOf(state, activation, cardRegistry).orEmpty()
        return CombatReach.FIGHT in reach || CombatReach.BLOCK_DENIAL in reach
    }

    companion object {
        /** Activations played out per scored plan. */
        const val MAX_SIMULATIONS = 8

        /** Creatures per side a single-target answer is tried on. */
        const val MAX_TARGETS_PER_SIDE = 2

        /** The most activations of one repeatable ability tried as one answer. */
        const val MAX_REPEATS = 4
    }
}
