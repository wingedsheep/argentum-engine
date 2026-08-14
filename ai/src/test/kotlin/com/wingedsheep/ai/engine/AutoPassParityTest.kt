package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.legalactions.MeaningfulActionFilter
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.engine.view.asPriorityAction
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * The Phase 4a regression net: the auto-pass rules must mean the same thing on both sides of the
 * engine/DTO seam, and the enumeration-skipping fast path must never disagree with the full check.
 *
 * `AutoPassManager` used to own these rules over the client DTO `LegalActionInfo`, which is why the
 * AI could not call them. They now live in [MeaningfulActionFilter] over engine `LegalAction`s, and
 * `AutoPassManager` adapts the DTO and delegates. Two things could silently break as a result, and
 * this test is here for both:
 *
 * 1. **The adapter drifts.** `LegalActionEnricher` is a lossy projection — if it ever stopped
 *    carrying (say) `holdPriority` or `validBlockers`, the client would start stopping in different
 *    places from the AI and nothing else would notice.
 * 2. **The fast path over-fires.** [MeaningfulActionFilter.canAutoPassWithoutEnumerating] decides
 *    a whole priority window from the state alone. It is only sound if it is a strict subset of the
 *    full verdict, and that claim is an argument about which `when` branches ignore their action
 *    list — exactly the kind of argument a later edit invalidates.
 *
 * The corpus is real: two full seeded AI-vs-AI games, every priority window either player reaches.
 */
class AutoPassParityTest : FunSpec({

    val set = MtgSetCatalog.requireByCode("BLB")
    val registry = CardRegistry().apply {
        register(set.cards)
        register(set.basicLands)
    }
    val enricher = LegalActionEnricher(ManaSolver(registry), registry)

    /** One captured priority window: the state, whose priority it is, and what they may do. */
    data class Window(val state: GameState, val playerId: EntityId, val actions: List<LegalAction>)

    /**
     * Play a seeded AI-vs-AI game, capturing every priority window on the way through.
     *
     * A cut-down `TableGameRunner`: the arena's runner returns an outcome, not the states, and the
     * point here is the states.
     */
    fun harvest(seed: Long, maxActions: Int = 1_200): List<Window> {
        val processor = ActionProcessor(registry)
        val enumerator = LegalActionEnumerator.create(registry)
        val deck = buildSeededSealedDeck(set.cards, Random(seed))
        val init = GameInitializer(registry).initializeGame(
            GameConfig(
                players = listOf(PlayerConfig("Seat0", deck), PlayerConfig("Seat1", deck)),
                skipMulligans = true,
                startingPlayerIndex = 0,
                seed = seed,
            )
        )
        val ai = init.state.turnOrder.associateWith { AIPlayer.create(registry, it, AiProfile.LEGACY_V0) }

        var state = init.state
        val windows = mutableListOf<Window>()
        var actions = 0
        while (!state.gameOver && actions < maxActions) {
            val decision = state.pendingDecision
            if (decision != null) {
                val response = ai.getValue(decision.playerId).respondToDecision(state, decision)
                val result = processor.process(state, SubmitDecision(decision.playerId, response)).result
                if (result.error != null) break
                state = result.state
                actions++
                continue
            }
            val priorityPlayer = state.priorityPlayerId ?: break
            val legalActions = enumerator.enumerate(state, priorityPlayer, EnumerationMode.ACTIONS_ONLY)
            windows += Window(state, priorityPlayer, legalActions)

            val result = processor.process(state, ai.getValue(priorityPlayer).chooseAction(state)).result
            val next = if (result.error != null) {
                processor.process(state, safeFallbackAction(state, priorityPlayer, enumerator)).result
            } else {
                result
            }
            if (next.error != null || next.state === state) break
            state = next.state
            actions++
        }
        return windows
    }

    val corpus = listOf(20260728L, 20260729L).flatMap { harvest(it) }

    test("the corpus is big and varied enough to mean something") {
        corpus.size shouldBeGreaterThan 300
        // Both verdicts must occur, or "identical on every window" is a vacuous claim.
        val verdicts = corpus.map { MeaningfulActionFilter.shouldAutoPass(it.state, it.playerId, it.actions, cardRegistry = registry) }
        verdicts.count { it } shouldBeGreaterThan 20
        verdicts.count { !it } shouldBeGreaterThan 20
    }

    test("the DTO and the engine action agree on what is meaningful") {
        val mismatches = corpus.mapNotNull { window ->
            val fromEngine = MeaningfulActionFilter.filterMeaningful(window.actions).map { it.description }
            val fromDto = enricher.enrich(window.actions, window.state, window.playerId)
                .filter { MeaningfulActionFilter.isMeaningful(it.asPriorityAction()) }
                .map { it.description }
            if (fromEngine == fromDto) null else {
                "turn ${window.state.turnNumber} ${window.state.step}: engine=$fromEngine dto=$fromDto"
            }
        }
        mismatches.shouldBeEmpty()
    }

    test("the DTO and the engine action reach the same auto-pass verdict") {
        val mismatches = corpus.mapNotNull { window ->
            val fromEngine = MeaningfulActionFilter.autoPassVerdict(
                window.state, window.playerId, window.actions, cardRegistry = registry
            )
            val fromDto = MeaningfulActionFilter.autoPassVerdict(
                window.state, window.playerId,
                enricher.enrich(window.actions, window.state, window.playerId).map { it.asPriorityAction() },
                cardRegistry = registry
            )
            if (fromEngine == fromDto) null else {
                "turn ${window.state.turnNumber} ${window.state.step}: engine=$fromEngine dto=$fromDto"
            }
        }
        mismatches.shouldBeEmpty()
    }

    test("the enumeration-free fast path never contradicts the full verdict") {
        val violations = corpus.filter { window ->
            MeaningfulActionFilter.canAutoPassWithoutEnumerating(window.state, window.playerId) &&
                !MeaningfulActionFilter.shouldAutoPass(
                    window.state, window.playerId, window.actions, cardRegistry = registry
                )
        }.map { "turn ${it.state.turnNumber} ${it.state.step}: fast path passes, full check stops" }
        violations.shouldBeEmpty()
    }

    test("the fast path fires on a real share of windows — otherwise it is not worth having") {
        val skippable = corpus.count {
            MeaningfulActionFilter.canAutoPassWithoutEnumerating(it.state, it.playerId)
        }
        println(
            "enumeration-free windows: $skippable / ${corpus.size} " +
                "(${skippable * 100 / corpus.size}%) — quoted in docs/ai/baseline-metrics.md"
        )
        // Phase 0 measured 76.2% of windows offering zero candidates. The fast path is a strict
        // subset of that (it declines every window whose verdict depends on the hand), so a third
        // is the bar: below that the enumeration saving would not pay for the check.
        withClue("only $skippable of ${corpus.size} windows were skippable without enumerating") {
            (skippable * 3 > corpus.size) shouldBe true
        }
    }
})
