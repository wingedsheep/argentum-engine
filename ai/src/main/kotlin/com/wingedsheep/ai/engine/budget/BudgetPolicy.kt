package com.wingedsheep.ai.engine.budget

import com.wingedsheep.ai.engine.lifePoolsOf
import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Decides how much thought one decision is worth.
 *
 * Lives on `AiProfile`, so a profile is what an arena run is actually measuring: `just arena
 * v0-budget-100 v0-budget-3000` is two agents differing in nothing but this.
 */
interface BudgetPolicy {
    /**
     * The budget for a priority window.
     *
     * @param meaningfulActions the candidate list *after* the meaningful-action filter, or the raw
     *   affordable candidates when the filter is off. Empty means the AI is about to pass.
     */
    fun budgetFor(state: GameState, playerId: EntityId, meaningfulActions: List<LegalAction>): DecisionBudget

    /**
     * The budget for answering a [com.wingedsheep.engine.core.PendingDecision].
     *
     * Separate from [budgetFor] because a decision has no candidate list to size a tier from, and
     * because it is never skippable: the engine is blocked mid-resolution until it is answered, so
     * there is no [BudgetTier.TRIVIAL] equivalent.
     */
    fun budgetForDecision(state: GameState, playerId: EntityId): DecisionBudget
}

/**
 * No tiering: every decision gets today's constants and no global deadline.
 *
 * This is what `AiProfile.LEGACY_V0` runs, and it must stay bit-identical to the pre-Phase-4 AI —
 * `FrozenBaselineTest` hashes V0's action stream precisely so that a refactor cannot quietly move
 * the permanent reference opponent.
 */
object LegacyBudgetPolicy : BudgetPolicy {
    override fun budgetFor(
        state: GameState,
        playerId: EntityId,
        meaningfulActions: List<LegalAction>,
    ): DecisionBudget = DecisionBudget.legacy()

    override fun budgetForDecision(state: GameState, playerId: EntityId): DecisionBudget =
        DecisionBudget.legacy()

    override fun toString(): String = "legacy"
}

/**
 * [LegacyBudgetPolicy]'s constants with the rollout allowance resized.
 *
 * Phase 7 is the first search in this plan whose cost the arena can actually feel: at the nominal
 * 64 playouts a decision costs ~2,400 `process()` calls, so a rollout game is ~1,000× a `v0` game
 * and the 1,000-game merge gate goes from 3.5 minutes to hours. Phase 1's report predicted exactly
 * this ("the budget, not the game count, is what makes an arena expensive") and Phase 4b dodged it
 * because a filtered greedy agent costs nothing.
 *
 * So the arena runs the rollout agents at a reduced playout count, and the ladder of counts is also
 * the phase's own safety net: **strength must be monotone in playouts**, or the search is
 * generating noise rather than signal. Same argument as `ArenaBudgetScalingTest`, one level down.
 */
class RolloutBudgetPolicy(private val playouts: Int) : BudgetPolicy {
    private val allowances = SearchAllowances.LEGACY.copy(rolloutPlayouts = playouts)

    override fun budgetFor(
        state: GameState,
        playerId: EntityId,
        meaningfulActions: List<LegalAction>,
    ): DecisionBudget = budget()

    override fun budgetForDecision(state: GameState, playerId: EntityId): DecisionBudget = budget()

    private fun budget() =
        DecisionBudget(BudgetTier.NORMAL, allowances, DecisionBudget.UNBOUNDED_MILLIS)

    override fun toString(): String = "rollout($playouts playouts)"
}

/**
 * The four-tier policy from `backlog/engine-ai-improvement.md` §4b.
 *
 * | Tier | When |
 * |---|---|
 * | TRIVIAL | Nothing meaningful to choose between — the AI is passing regardless |
 * | ROUTINE | An opponent's-turn window with no immediate threat; upkeep, draw, end step |
 * | NORMAL | Our main phase, or a response to something on the stack |
 * | CRITICAL | Combat declaration, or either side within one swing of lethal |
 *
 * Two entries from the plan's table are **not** implemented and are deliberately not faked:
 * "sweeper castable or on the stack" and "a real counterspell window" both need to know what a card
 * *does*, which is Phase 6's `CardIntent`. Guessing them from a mana cost would put the most
 * expensive tier on the wrong windows, which is worse than leaving them at NORMAL.
 *
 * @param normalMillis size of the [BudgetTier.NORMAL] tier. Every other tier scales off it in the
 *   published ratio (1:10 routine, 5:2 critical), which is what makes `ArenaBudgetScalingTest`'s
 *   "the same agent at 100 / 1000 / 3000 ms" a single-parameter sweep.
 */
class TieredBudgetPolicy(
    val normalMillis: Long = BudgetTier.NORMAL.millis,
    /**
     * Grade the window between "blockers are declared" and "damage is dealt" as
     * [BudgetTier.NORMAL] rather than [BudgetTier.ROUTINE].
     *
     * That window falls to ROUTINE today, and it is the one where an instant converts an attack
     * into a kill. Two facts put it there and neither is about how much the decision is worth:
     * combat declarations are already spent (the `DeclareAttackers`/`DeclareBlockers` branch above
     * has passed), and [someoneIsInLethalRange] does not fire, because attacking **taps** the
     * creature (CR 508.1f) so the attacking side reads as zero power — and even counting attackers
     * would not fire it, since the proxy compares power that exists *now* against life, and a
     * trick's whole job is to make an attack lethal that currently is not.
     *
     * ROUTINE is 200 ms, below `SearchAllowances.NORMAL_MILLIS`, and that threshold is the one
     * that matters: below it `refineTargetsBySimulation` is off and `targetCandidates` drops to 1,
     * so a spell's target is whatever the heuristic names first. On the board `instants-07` pins —
     * an unblocked 2/2, Giant Growth, opponent at 5 — that pick is bad enough to score below
     * passing, and the AI passes on exact lethal. Restoring NORMAL restores the simulation-refined
     * pick, which is the whole of the fix; the tier is deliberately not CRITICAL, since nothing
     * measured here needs the extra playouts and combat already pays for a 5 s tier twice.
     *
     * The window is [GameSimulator.isPreDamageCombatState]'s, and the two are kept the same shape
     * on purpose: this is the tier for the window that simulator was taught to see through.
     */
    val preDamageCombatIsNormal: Boolean = false,
) : BudgetPolicy {

    override fun budgetFor(
        state: GameState,
        playerId: EntityId,
        meaningfulActions: List<LegalAction>,
    ): DecisionBudget {
        val tier = tierFor(state, playerId, meaningfulActions)
        val millis = millisFor(tier)
        return DecisionBudget(tier, SearchAllowances.forMillis(millis), millis)
    }

    override fun budgetForDecision(state: GameState, playerId: EntityId): DecisionBudget {
        val tier = if (someoneIsInLethalRange(state, playerId)) BudgetTier.CRITICAL else BudgetTier.NORMAL
        val millis = millisFor(tier)
        return DecisionBudget(tier, SearchAllowances.forMillis(millis), millis)
    }

    fun millisFor(tier: BudgetTier): Long = when (tier) {
        BudgetTier.TRIVIAL -> 0
        BudgetTier.ROUTINE -> normalMillis / 10
        BudgetTier.NORMAL -> normalMillis
        BudgetTier.CRITICAL -> normalMillis * 5 / 2
    }

    fun tierFor(
        state: GameState,
        playerId: EntityId,
        meaningfulActions: List<LegalAction>,
    ): BudgetTier {
        if (meaningfulActions.isEmpty()) return BudgetTier.TRIVIAL

        // Combat declaration is always CRITICAL, so combat never gets *less* time than it has
        // today — the risk register's "combat's 1 s cap fights the global budget" line.
        if (meaningfulActions.any {
                it.actionType == "DeclareAttackers" || it.actionType == "DeclareBlockers"
            }
        ) {
            return BudgetTier.CRITICAL
        }

        if (someoneIsInLethalRange(state, playerId)) return BudgetTier.CRITICAL

        // Blocks are in and damage is next — the window an instant converts an attack in.
        if (preDamageCombatIsNormal && isPreDamageCombat(state)) return BudgetTier.NORMAL

        // Something is on the stack and we are choosing whether to answer it.
        if (state.stack.isNotEmpty()) return BudgetTier.NORMAL

        val isMyTurn = state.isActiveTurnFor(playerId)
        if (isMyTurn && (state.step == Step.PRECOMBAT_MAIN || state.step == Step.POSTCOMBAT_MAIN)) {
            return BudgetTier.NORMAL
        }

        return BudgetTier.ROUTINE
    }

    // The suffix is conditional so every arena report already published under `tiered(2000ms)`
    // still names the agent it named.
    override fun toString(): String =
        "tiered(${normalMillis}ms" + (if (preDamageCombatIsNormal) ", pre-damage=normal)" else ")")

    /**
     * Is either side one combat step away from dying?
     *
     * A deliberately crude proxy — the total power of a side's untapped creatures against the
     * smallest life pool facing it — because it runs at every priority window. It over-triggers
     * (blockers exist; not every creature can attack) and that is the safe direction: the cost of a
     * false CRITICAL is one slow decision, the cost of a false ROUTINE is a missed lethal.
     */
    private fun someoneIsInLethalRange(state: GameState, playerId: EntityId): Boolean {
        val sides = state.sidesFor(playerId) ?: return false
        val myLife = state.lifePoolsOf(sides.mine).minOrNull() ?: return false
        val myPower = untappedPowerOf(state, sides.mine)
        return sides.opponents.any { opponent ->
            val theirLife = state.lifePoolsOf(opponent).minOrNull() ?: return@any false
            val theirPower = untappedPowerOf(state, opponent)
            theirPower >= myLife || myPower >= theirLife
        }
    }

    private fun untappedPowerOf(state: GameState, side: List<EntityId>): Int {
        val projected = state.projectedState
        return side.sumOf { player ->
            projected.getBattlefieldControlledBy(player).sumOf { entityId ->
                if (!projected.isCreature(entityId)) return@sumOf 0
                if (state.getEntity(entityId)?.get<TappedComponent>() != null) return@sumOf 0
                (projected.getPower(entityId) ?: 0).coerceAtLeast(0)
            }
        }
    }

    /**
     * Blocks are in and damage has not been dealt — see [preDamageCombatIsNormal].
     *
     * The same shape as `GameSimulator.isPreDamageCombatState`: those two steps, and only when
     * something is actually attacking. The attacker check is belt-and-braces rather than a case
     * that arises — the engine skips both steps outright when nothing attacks (CR 508.8), which is
     * also why it has no test: the state cannot be reached by playing the game.
     */
    private fun isPreDamageCombat(state: GameState): Boolean {
        if (state.step != Step.DECLARE_BLOCKERS && state.step != Step.FIRST_STRIKE_COMBAT_DAMAGE) {
            return false
        }
        return state.getBattlefield().any { state.getEntity(it)?.has<AttackingComponent>() == true }
    }
}
