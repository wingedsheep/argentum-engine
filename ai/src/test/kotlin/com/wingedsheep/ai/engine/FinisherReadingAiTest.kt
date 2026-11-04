package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class FinisherReadingAiTest : ScenarioTestBase() {

    private fun tags(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags

    private fun vraskaBoard(loyalty: Int, ours: List<String>, theirs: List<String>): TestGame {
        val game = scenario().withPlayers()
            .withRngSeed(7)
            .withCardOnBattlefield(1, "Vraska, Scheming Gorgon")
            .apply { ours.forEach { withCardOnBattlefield(1, it) } }
            .apply { theirs.forEach { withCardOnBattlefield(2, it) } }
            .withLandsOnBattlefield(1, "Swamp", 3)
            .apply { repeat(8) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Plains") } }
            .build()
        val id = game.findPermanent("Vraska, Scheming Gorgon")!!
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty))
        }
        return game.advanceToPriority(1, Step.PRECOMBAT_MAIN)
    }

    private fun choose(game: TestGame, profile: AiProfile = AiProfile.LIVE): Pair<GameAction, List<AiActionOption>> {
        var options = emptyList<AiActionOption>()
        val sink = AiInsightSink { _, insight -> if (insight.options.size > 1) options = insight.options }
        val action = AIPlayer.create(cardRegistry, game.player1Id, profile, insightSink = sink).chooseAction(game.state)
        return action to options
    }

    private fun TestGame.loyaltyChange(action: GameAction): Int? {
        if (action !is ActivateAbility || action.sourceId != findPermanent("Vraska, Scheming Gorgon")) return null
        val ability = cardRegistry.getCard("Vraska, Scheming Gorgon")!!.script.activatedAbilities.find { it.id == action.abilityId }
        return (ability?.cost as? AbilityCost.Loyalty)?.change
    }

    private val withoutFlag = AiProfile.LIVE.copy(id = "live-without-finishers", finishersEndTheGame = false)
    private val threeBears = listOf("Grizzly Bears", "Grizzly Bears", "Grizzly Bears")

    /** Both seats played by [AiProfile.LIVE] until the turn ends or the game does; the winner, if any. */
    private fun playOutTurn(game: TestGame): com.wingedsheep.sdk.model.EntityId? {
        val ais = mapOf(
            game.player1Id to AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE),
            game.player2Id to AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE),
        )
        var guard = 0
        while (!game.state.gameOver && game.state.turnNumber == 1 && guard++ < 80) {
            val decision = game.state.pendingDecision
            val action = if (decision != null) {
                SubmitDecision(decision.playerId, ais.getValue(decision.playerId).respondToDecision(game.state, decision))
            } else {
                ais.getValue(game.state.priorityPlayerId ?: break).chooseAction(game.state)
            }
            withClue("$action") { game.execute(action).error shouldBe null }
        }
        return game.state.winnerId
    }

    init {
        test("a trigger that makes the damaged opponent lose the game reads as a game win, printed or granted") {
            tags("Vraska, Scheming Gorgon") shouldContain IntentTag.GAME_WIN
            tags("Phage the Untouchable") shouldContain IntentTag.GAME_WIN
        }

        test("Vraska at 10 with three attackers into one blocker: −10 with the flag, +2 without") {
            val game = vraskaBoard(10, ours = threeBears, theirs = listOf("Grizzly Bears"))
            val (before, beforeOptions) = choose(game, withoutFlag)
            withClue("reproduction: chose $before\n${beforeOptions.joinToString("\n")}") { game.loyaltyChange(before) shouldBe 2 }
            val (after, options) = choose(game)
            withClue("chose $after\n${options.joinToString("\n")}") { game.loyaltyChange(after) shouldBe -10 }
        }

        test("after the −10 the AI sends its creatures in and wins this turn") {
            val game = vraskaBoard(10, ours = threeBears, theirs = listOf("Grizzly Bears"))
            playOutTurn(game) shouldBe game.player1Id
        }

        test("Vraska at 10 with as many blockers as attackers keeps her loyalty") {
            val game = vraskaBoard(10, ours = threeBears, theirs = threeBears)
            val (action, options) = choose(game)
            withClue("chose $action\n${options.joinToString("\n")}") { (game.loyaltyChange(action) == -10) shouldBe false }
        }
    }
}
