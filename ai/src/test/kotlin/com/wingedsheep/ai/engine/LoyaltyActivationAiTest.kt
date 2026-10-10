package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.fuelLoyaltyIsNotBoardValue]. The empower-Jace token ("−1: Surveil 1. −3: Draw a
 * card.") was offered every main phase from turn 5 to 24 of a 2026-10-10 `live` AI-vs-AI log (FRA
 * vs BLB, game 2) and never activated: the evaluator charges each spent loyalty counter as board
 * value, and neither ability buys anything it can see.
 */
class LoyaltyActivationAiTest : ScenarioTestBase() {

    /** A walker with a + ability: its loyalty is a real stock, so a minus is still charged. */
    private val plusWalker = card("Test Plus Walker") {
        manaCost = "{3}"
        typeLine = "Planeswalker — Test"
        startingLoyalty = 0
        loyaltyAbility(1) { effect = Effects.GainLife(1) }
        loyaltyAbility(-1) { effect = Patterns.Library.surveil(1) }
    }

    init {
        cardRegistry.register(listOf(plusWalker))
    }

    private fun TestGame.withLoyalty(cardName: String, loyalty: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, loyalty))
        }
    }

    private fun walkerBoard(walker: String, loyalty: Int): TestGame {
        val game = scenario().withPlayers()
            .withCardOnBattlefield(1, walker, isToken = walker == "Jace")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 3)
            .apply { repeat(8) { withCardInLibrary(1, if (it % 2 == 0) "Plains" else "Grizzly Bears") } }
            .apply { repeat(8) { withCardInLibrary(2, "Plains") } }
            .build()
        game.withLoyalty(walker, loyalty)
        return game.advanceToPriority(1, Step.PRECOMBAT_MAIN)
    }

    /** The action [profile] takes, and every option it scored, for the failure message. */
    private fun choose(game: TestGame, profile: AiProfile): Pair<GameAction, List<AiActionOption>> {
        var options = emptyList<AiActionOption>()
        val sink = AiInsightSink { _, insight -> if (insight.options.size > 1) options = insight.options }
        val action = AIPlayer.create(cardRegistry, game.player1Id, profile, insightSink = sink)
            .chooseAction(game.state)
        return action to options
    }

    private fun TestGame.activates(action: GameAction, walker: String) =
        action is ActivateAbility && action.sourceId == findPermanent(walker)

    init {
        test("the Jace token's loyalty is only fuel; a walker with a + ability's is not") {
            val intents = IntentCatalog.of(cardRegistry)
            intents.loyaltyIsOnlyFuel("Jace") shouldBe true
            intents.loyaltyIsOnlyFuel("Test Plus Walker") shouldBe false
            intents.loyaltyIsOnlyFuel("Grizzly Bears") shouldBe false
        }

        for (loyalty in listOf(3, 5)) {
            test("the Jace token at $loyalty loyalty: passed before the flag, activated with it") {
                val game = walkerBoard("Jace", loyalty)

                val (before, beforeOptions) = choose(game, AiProfile.PRODUCTION_CANDIDATE_EXPIRING)
                withClue("reproduction: chose $before\n${beforeOptions.joinToString("\n")}") {
                    game.activates(before, "Jace") shouldBe false
                }

                for (profile in listOf(AiProfile.PRODUCTION_CANDIDATE_FUELLOYALTY, AiProfile.LIVE)) {
                    val (after, options) = choose(game, profile)
                    withClue("${profile.id} chose $after\n${options.joinToString("\n")}") {
                        game.activates(after, "Jace") shouldBe true
                    }
                }
            }
        }

        test("the Jace token at 1 loyalty: a −1 that kills it is still charged the walker") {
            val game = walkerBoard("Jace", 1)
            val (action, options) = choose(game, AiProfile.LIVE)
            withClue("chose $action\n${options.joinToString("\n")}") {
                options.none { it.note?.contains("loyalty is only fuel") == true } shouldBe true
                game.activates(action, "Jace") shouldBe false
            }
        }

        test("a walker with a + ability gets no refund on its minus") {
            val game = walkerBoard("Test Plus Walker", 3)
            val (action, options) = choose(game, AiProfile.LIVE)
            withClue("chose $action\n${options.joinToString("\n")}") {
                options.none { it.note?.contains("loyalty is only fuel") == true } shouldBe true
            }
        }
    }
}
