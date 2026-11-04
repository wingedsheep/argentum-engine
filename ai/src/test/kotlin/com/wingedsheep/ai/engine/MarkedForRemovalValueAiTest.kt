package com.wingedsheep.ai.engine

import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.TetzimocPrimalDeath
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A creature of theirs carrying a counter that a card in our hand destroys on entry (Tetzimoc,
 * Primal Death's prey counter) is already half gone — the
 * [com.wingedsheep.ai.engine.evaluation.MarkedForRemoval] term, [AiProfile.marksBeforeTheSweep].
 *
 * Without it a prey counter is worth nothing on the board, so with seven mana the AI cast
 * Tetzimoc bare instead of paying {B} to mark the Serra Angel first and destroying it on entry.
 */
class MarkedForRemovalValueAiTest : ScenarioTestBase() {

    private val withoutFlag = AiProfile.LIVE.copy(id = "live-without-marks", marksBeforeTheSweep = false)

    private fun board(swamps: Int): TestGame = scenario().withPlayers()
        .withRngSeed(7)
        .withLandsOnBattlefield(1, "Swamp", swamps)
        .withCardInHand(1, TetzimocPrimalDeath.name)
        .withCardOnBattlefield(2, "Serra Angel")
        .apply { repeat(8) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Plains") } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    private fun choose(game: TestGame, profile: AiProfile = AiProfile.LIVE): Pair<GameAction, List<AiActionOption>> {
        var options = emptyList<AiActionOption>()
        val sink = AiInsightSink { _, insight -> if (insight.options.size > 1) options = insight.options }
        val action = AIPlayer.create(cardRegistry, game.player1Id, profile, insightSink = sink).chooseAction(game.state)
        return action to options
    }

    /** Player one's main phase played by [AiProfile.LIVE] until it moves past it or passes. */
    private fun playMainPhase(game: TestGame) {
        val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
        var guard = 0
        while (game.state.step == Step.PRECOMBAT_MAIN && guard++ < 20) {
            val decision = game.state.pendingDecision
            val action = when {
                decision != null -> SubmitDecision(decision.playerId, ai.respondToDecision(game.state, decision))
                game.state.priorityPlayerId == game.player1Id -> ai.chooseAction(game.state)
                else -> AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            }
            withClue("$action") { game.execute(action).error shouldBe null }
        }
    }

    init {
        test("seven mana: marks the Serra Angel with the flag, casts Tetzimoc bare without it") {
            val game = board(swamps = 7)
            val (before, beforeOptions) = choose(game, withoutFlag)
            withClue("reproduction: chose $before\n${beforeOptions.joinToString("\n")}") {
                before.shouldBeInstanceOf<CastSpell>()
            }
            val (after, options) = choose(game)
            withClue("chose $after\n${options.joinToString("\n")}") { after.shouldBeInstanceOf<ActivateAbility>() }
        }

        test("seven mana: marked, then Tetzimoc enters and the Serra Angel dies") {
            val game = board(swamps = 7)
            playMainPhase(game)
            game.findPermanent("Serra Angel") shouldBe null
            (game.findPermanent(TetzimocPrimalDeath.name) != null) shouldBe true
        }

        test("six mana: marking would strand the cast, so Tetzimoc comes down now") {
            val game = board(swamps = 6)
            val (action, options) = choose(game)
            withClue("chose $action\n${options.joinToString("\n")}") { action.shouldBeInstanceOf<CastSpell>() }
        }

        test("with nothing of theirs to mark, the mana goes to Tetzimoc") {
            val game = scenario().withPlayers().withRngSeed(7)
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withCardInHand(1, TetzimocPrimalDeath.name)
                .apply { repeat(8) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Plains") } }
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val (action, options) = choose(game)
            withClue("chose $action\n${options.joinToString("\n")}") { action.shouldBeInstanceOf<CastSpell>() }
        }

        test("a marked creature without the sweeper in hand is worth nothing extra") {
            val game = board(swamps = 7)
            val term = com.wingedsheep.ai.engine.evaluation.MarkedForRemoval(
                com.wingedsheep.ai.engine.knowledge.IntentCatalog.of(cardRegistry),
            )
            val angel = game.findPermanent("Serra Angel")!!
            val marked = game.state.updateEntity(angel) { c ->
                c.with(
                    com.wingedsheep.engine.state.components.battlefield.CountersComponent()
                        .withAdded(com.wingedsheep.sdk.core.CounterType.PREY, 1),
                )
            }
            (term.score(marked, marked.projectedState, game.player1Id) > 0.0) shouldBe true
            val tetzimoc = marked.getZone(game.player1Id, Zone.HAND).single()
            val handless = marked.removeFromZone(com.wingedsheep.engine.state.ZoneKey(game.player1Id, Zone.HAND), tetzimoc)
            term.score(handless, handless.projectedState, game.player1Id) shouldBe 0.0
        }
    }
}
