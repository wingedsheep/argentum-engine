package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.ai.engine.advisor.modules.OnslaughtAdvisorModule
import com.wingedsheep.ai.engine.budget.RolloutBudgetPolicy
import com.wingedsheep.ai.engine.budget.TieredBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.rollout.RolloutSettings
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.EntityId

/**
 * A named competitor in the arena — an [AiProfile] plus the short name you type on the command
 * line (`just arena v0 blb-advisors 1000`).
 *
 * One [AIPlayer] is built per seat per game and never shared: `GameSimulator.isResolving` and
 * `decisionResolver` are mutable instance state, so a shared instance would corrupt its own
 * recursion guard across concurrent games.
 */
data class ArenaAgent(val name: String, val profile: AiProfile) {
    fun createPlayer(
        registry: CardRegistry,
        playerId: EntityId,
        opponentDecks: Map<EntityId, Map<String, Int>> = emptyMap(),
    ): AIPlayer =
        AIPlayer.create(
            registry,
            playerId,
            profile,
            opponentDecks.mapValues { OpponentModel.KnownDecklist(it.value) },
        )
}

/**
 * The agents `just arena` can name.
 *
 * Adding an agent here is how a later phase enters the scoreboard: build the [AiProfile], give it
 * a name, and every arena/gauntlet recipe can reach it with no other wiring.
 */
object ArenaAgents {

    private val builtIn: List<ArenaAgent> = listOf(
        // The permanent reference opponent. Every version reports against this one.
        ArenaAgent("v0", AiProfile.LEGACY_V0),
        // Whatever `AIPlayer.create(registry, playerId)` builds today.
        ArenaAgent("current", AiProfile.CURRENT),
        // What a player actually faces in a real game: BLB + ONS card advisors.
        ArenaAgent("production", AiProfile.PRODUCTION),
        // The same, plus everything Phases 4, 7 and 8 built and left switched off. The promotion
        // gate is `just arena production production-candidate` — not against `v0`, which has
        // neither the advisors nor `CardIntent` that already shipped.
        ArenaAgent("production-candidate", AiProfile.PRODUCTION_CANDIDATE),
        // The cheap targeted fixes — combat-damage horizon + concave hand curve — without and
        // with rollouts. `just arena production production-tuned` prices them on their own.
        ArenaAgent("production-tuned", AiProfile.PRODUCTION_TUNED),
        ArenaAgent("production-candidate-tuned", AiProfile.PRODUCTION_CANDIDATE_TUNED),
        // One variable each, so an arena delta can be attributed the same way a puzzle delta is.
        ArenaAgent("production-horizon", AiProfile.PRODUCTION_HORIZON),
        ArenaAgent("production-concave", AiProfile.PRODUCTION_CONCAVE),
        ArenaAgent("production-concave-2", AiProfile.PRODUCTION_CONCAVE_2),
        ArenaAgent("production-horizon-concave", AiProfile.PRODUCTION_HORIZON_CONCAVE),
        // The suite's best zero-regression combination: 63/66 against production's 60/66.
        ArenaAgent("production-horizon-concave-2", AiProfile.PRODUCTION_HORIZON_CONCAVE_2),
        ArenaAgent("production-crackback", AiProfile.PRODUCTION_CRACKBACK),
        // The land-drop accounting alone, and — since 2026-08-08 — what players actually face.
        // `just arena production production-landdrop 1000` prices the accounting without paying for
        // rollouts on either seat; `just arena production-candidate-tuned production-candidate-landdrop`
        // was the promotion gate.
        ArenaAgent("production-landdrop", AiProfile.PRODUCTION_LANDDROP),
        ArenaAgent("production-candidate-landdrop", AiProfile.PRODUCTION_CANDIDATE_LANDDROP),
        // Land *order* — which of two lands to drop, rather than whether to drop one.
        // `just arena production production-landseq 1000` prices the term on its own;
        // `just arena production-candidate-landdrop production-candidate-landseq 300` is its
        // promotion gate, against what players face today.
        ArenaAgent("production-landseq", AiProfile.PRODUCTION_LANDSEQ),
        ArenaAgent("production-candidate-landseq", AiProfile.PRODUCTION_CANDIDATE_LANDSEQ),
        // The combat trick window: hold it until blocks are in, and give the pre-damage window the
        // budget to spend it. `just arena production production-trickwindow 1000` prices the
        // holding half on its own (the budget half is a no-op without tiers);
        // `just arena production-candidate-landseq production-candidate-trickwindow 300` is the
        // promotion gate, against what players face today.
        ArenaAgent("production-trickwindow", AiProfile.PRODUCTION_TRICKWINDOW),
        ArenaAgent("production-candidate-trickwindow", AiProfile.PRODUCTION_CANDIDATE_TRICKWINDOW),
        // The race-clock bound: stop the "no attackers" sentinel from swamping the evaluator.
        // `just arena production production-raceclock 1000` prices the term on its own;
        // `just arena production-candidate-trickwindow production-candidate-raceclock 300` is its
        // promotion gate, against what players face today.
        ArenaAgent("production-raceclock", AiProfile.PRODUCTION_RACECLOCK),
        ArenaAgent("production-candidate-raceclock", AiProfile.PRODUCTION_CANDIDATE_RACECLOCK),
        // Removal patience: don't spend the Pacifism on the first 1/1.
        // `just arena production production-patience 1000` prices the term on its own — read that
        // column for what it *costs*, since the puzzle it targets needs the race clock to move at
        // all. `just arena production-candidate-raceclock production-candidate-patience 300` is its
        // promotion gate, against what players face today.
        ArenaAgent("production-patience", AiProfile.PRODUCTION_PATIENCE),
        ArenaAgent("production-candidate-patience", AiProfile.PRODUCTION_CANDIDATE_PATIENCE),
        // The two `BoardPresence.creatureValue` corrections the race-clock trade exposed: marked
        // damage is not progress, and "can't attack" costs the power rather than 15% of everything.
        // `just arena production production-damagefades 300` and `… production-pacified 300` price
        // each term on its own; `just arena production-candidate-patience
        // production-candidate-boardvalue 300` is the promotion gate, against what players face today.
        ArenaAgent("production-damagefades", AiProfile.PRODUCTION_DAMAGEFADES),
        ArenaAgent("production-pacified", AiProfile.PRODUCTION_PACIFIED),
        ArenaAgent("production-candidate-boardvalue", AiProfile.PRODUCTION_CANDIDATE_BOARDVALUE),
        // The cantrip end-step window. `just arena production production-cantrip 300` prices the
        // term on its own; `just arena production-candidate-boardvalue production-candidate-cantrip
        // 300` is the promotion gate.
        ArenaAgent("production-cantrip", AiProfile.PRODUCTION_CANTRIP),
        ArenaAgent("production-candidate-cantrip", AiProfile.PRODUCTION_CANDIDATE_CANTRIP),
        // A land in hand is a card worth less than a spell and less than the same land on the
        // battlefield. `just arena production production-manalands 300` prices the model on its own;
        // `just arena production-candidate-cantrip production-candidate-manalands 300` is the
        // promotion gate. Expect this one to *move*, unlike the four before it.
        ArenaAgent("production-manalands", AiProfile.PRODUCTION_MANALANDS),
        ArenaAgent("production-candidate-manalands", AiProfile.PRODUCTION_CANDIDATE_MANALANDS),
        // The counterspell half of removal patience: a counter is worth holding while the caster
        // still has the mana for something bigger. `just arena production production-counterpatience
        // 300` prices the term on its own; `just arena production-candidate-cantrip
        // production-candidate-counterpatience 300` is the promotion gate, against what players face
        // today. Expect a rare shape and therefore a wide or degenerate CI, as with patience.
        ArenaAgent("production-counterpatience", AiProfile.PRODUCTION_COUNTERPATIENCE),
        ArenaAgent("production-candidate-counterpatience", AiProfile.PRODUCTION_CANDIDATE_COUNTERPATIENCE),
        // Flash creatures held for the ambush window instead of dumped in our own main phase.
        // `just arena production production-ambush 300` prices the term on its own; `just arena
        // production-candidate-counterpatience production-candidate-ambush 300` is the promotion
        // gate, against what players face today.
        ArenaAgent("production-ambush", AiProfile.PRODUCTION_AMBUSH),
        ArenaAgent("production-candidate-ambush", AiProfile.PRODUCTION_CANDIDATE_AMBUSH),
        // Activated abilities whose payoff expires at cleanup, held for a window that can spend it.
        // `just arena production production-expiring 300` prices the term on its own; `just arena
        // production-candidate-counterpatience production-candidate-expiring 300` is the promotion
        // gate, against what players face today.
        ArenaAgent("production-expiring", AiProfile.PRODUCTION_EXPIRING),
        ArenaAgent("production-candidate-expiring", AiProfile.PRODUCTION_CANDIDATE_EXPIRING),
        ArenaAgent("production-targeted", AiProfile.PRODUCTION_TARGETED),
        // Explicit ECL candidates. Their resource-backed weights fail closed to production's
        // evaluator until a validated artifact is installed; automatic selection is set-gated.
        ArenaAgent("ecl-apprentice", AiProfile.ECL_APPRENTICE),
        ArenaAgent("ecl-overlay", AiProfile.ECL_OVERLAY),
        // V0 plus one advisor module each — this is the split `AdvisorBenchmark` measured, so
        // `just arena v0 blb-advisors` is directly comparable to its published number.
        ArenaAgent("blb-advisors", AiProfile.LEGACY_V0.copy(
            id = "blb-advisors",
            advisorModules = listOf(BloomburrowAdvisorModule()),
        )),
        ArenaAgent("ons-advisors", AiProfile.LEGACY_V0.copy(
            id = "ons-advisors",
            advisorModules = listOf(OnslaughtAdvisorModule()),
        )),
        // Not a playable agent — every evaluation weight is zero, so the Strategist can never
        // prefer an action to passing. It exists to prove the harness *discriminates*: an arena
        // that cannot separate this from `v0` is measuring noise, not strength.
        ArenaAgent("v0-blind", AiProfile.LEGACY_V0.copy(
            id = "v0-blind",
            evalWeightsId = "blind",
        )),
        // ── Phase 4 ──
        // The meaningful-action filter alone. `just arena v0 v0-meaningful 1000` is the phase's
        // exit criterion: at ≥50% the filter costs nothing, and if it *loses* it is discarding a
        // real option and has found a bug for free.
        ArenaAgent("v0-meaningful", AiProfile.LEGACY_V0.copy(
            id = "v0-meaningful",
            useMeaningfulFilter = true,
        )),
        // The decision budget alone, at four sizes. `ArenaBudgetScalingTest` plays these against
        // each other: strength must be monotone in the budget, or the search is making noise.
        ArenaAgent("v0-budget-100", budgetAgent(100)),
        ArenaAgent("v0-budget-1000", budgetAgent(1_000)),
        ArenaAgent("v0-budget-2000", budgetAgent(2_000)),
        ArenaAgent("v0-budget-3000", budgetAgent(3_000)),
        // Both, at the nominal budget sizes — what Phase 4 proposes to ship.
        ArenaAgent("v0-phase4", AiProfile.PHASE4),
        // ── Phase 6 ──
        // Structural card knowledge alone. `just arena v0 v0-intent 1000` is the phase's merge
        // gate; keeping it off Phase 4 is what makes the number attributable to CardIntent.
        ArenaAgent("v0-intent", AiProfile.PHASE6),
        // Phases 4 and 6 together — what the plan proposes to ship.
        ArenaAgent("v0-phase4-intent", AiProfile.PHASE4_PHASE6),
        // ── Phase 7 ──
        // The rollout evaluator alone, at the shipped 16 playouts. Keeping it off Phases 4 and 6
        // is what makes its number attributable to the rollouts. Still ~50× a `v0` game, so size
        // runs accordingly.
        ArenaAgent("v0-rollout", AiProfile.PHASE7),
        ArenaAgent("v0-rollout-determinized", AiProfile.PHASE8),
        // The rollout ladder: the same agent at four playout counts. Two jobs. It makes the arena
        // affordable, and it is Phase 7's safety net — the direct analogue of
        // `ArenaBudgetScalingTest`. What it measured: strength rises from 4 to 8 playouts and then
        // **plateaus** (4-vs-32 is 50.7%, CI [47.5%, 53.7%] over 400 games), which is why
        // `SearchAllowances.NORMAL_PLAYOUTS` is 16 rather than the 60-odd a 2 s tier affords.
        ArenaAgent("v0-rollout-4", rolloutAgent(4)),
        ArenaAgent("v0-rollout-8", rolloutAgent(8)),
        ArenaAgent("v0-rollout-16", rolloutAgent(16)),
        ArenaAgent("v0-rollout-32", rolloutAgent(32)),
        // The same agent with sequential halving off — the honest control for the *allocation*
        // rather than for the rollouts. `just arena v0-rollout-flat v0-rollout 1000` prices what
        // spending the budget on the contenders is worth on its own.
        ArenaAgent("v0-rollout-flat", AiProfile.PHASE7.copy(
            id = "v0-rollout-flat",
            rollouts = RolloutSettings.DEFAULT.copy(sequentialHalving = false),
        )),
        // A deeper horizon at the same playout count. Depth and samples compete for one budget, so
        // this is the A/B that says which the budget should buy.
        ArenaAgent("v0-rollout-deep", AiProfile.PHASE7.copy(
            id = "v0-rollout-deep",
            rollouts = RolloutSettings.DEFAULT.copy(horizonPlayerTurns = 4),
        )),
        // Everything Phases 4, 6 and 7 add — what the plan proposes to ship.
        ArenaAgent("v0-phase4-intent-rollout", AiProfile.PHASE4_PHASE6_PHASE7),
    )

    /**
     * Every resource vector is automatically arena-addressable as `eval-<id>`. A tuning run can
     * replace the JSON artifact and immediately A/B its candidates without changing Kotlin.
     */
    private val all: List<ArenaAgent> = builtIn + EvalWeights.ids.map { weightsId ->
        ArenaAgent(
            name = "eval-$weightsId",
            profile = AiProfile.LEGACY_V0.copy(
                id = "eval-$weightsId",
                evalWeightsId = weightsId,
            ),
        )
    }

    /** `v0` plus rollouts, with nothing changed but how many playouts a decision may spend. */
    private fun rolloutAgent(playouts: Int): AiProfile = AiProfile.PHASE7.copy(
        id = "v0-rollout-$playouts",
        budgetPolicy = RolloutBudgetPolicy(playouts),
    )

    /** `v0` with nothing changed but the size of a [TieredBudgetPolicy]'s NORMAL tier. */
    private fun budgetAgent(normalMillis: Long): AiProfile = AiProfile.LEGACY_V0.copy(
        id = "v0-budget-$normalMillis",
        budgetPolicy = TieredBudgetPolicy(normalMillis),
    )

    private val byName: Map<String, ArenaAgent> = all.associateBy { it.name }

    val names: List<String> get() = all.map { it.name }

    fun resolve(name: String): ArenaAgent = byName[name]
        ?: throw IllegalArgumentException(
            "Unknown arena agent \"$name\". Known agents: ${names.joinToString(", ")}"
        )
}
