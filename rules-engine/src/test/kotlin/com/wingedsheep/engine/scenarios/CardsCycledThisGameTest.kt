package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CardsCycledThisGame] — "the number of times
 * you've cycled a card [named X] this game". Typecycling is cycling (CR 702.29f), the count is keyed
 * on the cycled card's name, scoped to the cycling player, and never resets between turns.
 */
class CardsCycledThisGameTest : ScenarioTestBase() {

    private val cycler = card("Test Plain Cycler") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Lizard"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.cycling("{1}"))
    }

    private val otherCycler = card("Test Other Cycler") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Lizard"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.cycling("{1}"))
    }

    private val landcycler = card("Test Swampcycler") {
        manaCost = "{5}{B}"
        typeLine = "Creature — Horror"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.typecycling("Swamp", "{1}"))
    }

    private fun TestGame.cycled(controllerId: EntityId, name: String? = null, player: Player = Player.You): Int =
        services.predicateEvaluator.amounts.evaluate(
            state,
            DynamicAmounts.cardsCycledThisGame(name, player),
            EffectContext(sourceId = null, controllerId = controllerId)
        )

    private fun TestGame.cycle(name: String) {
        withClue("cycling $name") { cycleCard(1, name).error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        cardRegistry.register(cycler)
        cardRegistry.register(otherCycler)
        cardRegistry.register(landcycler)

        test("counts each cycle by the cycled card's name; a null name counts every card") {
            val game = base()
                .withCardInHand(1, "Test Plain Cycler")
                .withCardInHand(1, "Test Plain Cycler")
                .withCardInHand(1, "Test Other Cycler")
                .build()
            val you = game.player1Id

            game.cycled(you) shouldBe 0
            game.cycle("Test Plain Cycler")
            game.cycle("Test Plain Cycler")
            game.cycle("Test Other Cycler")

            withClue("two physical copies of the same name both count") {
                game.cycled(you, "Test Plain Cycler") shouldBe 2
            }
            game.cycled(you, "Test Other Cycler") shouldBe 1
            game.cycled(you, "Test Swampcycler") shouldBe 0
            game.cycled(you) shouldBe 3
        }

        test("typecycling counts as cycling (CR 702.29f)") {
            val game = base().withCardInHand(1, "Test Swampcycler").build()

            withClue("typecycling") { game.typecycleCard(1, "Test Swampcycler").error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()

            game.isInGraveyard(1, "Test Swampcycler") shouldBe true
            game.cycled(game.player1Id, "Test Swampcycler") shouldBe 1
        }

        test("the count is per player — an opponent's count is untouched") {
            val game = base().withCardInHand(1, "Test Plain Cycler").build()
            game.cycle("Test Plain Cycler")

            game.cycled(game.player1Id, player = Player.EachOpponent) shouldBe 0
            game.cycled(game.player2Id) shouldBe 0
            game.cycled(game.player2Id, player = Player.EachOpponent) shouldBe 1
        }

        test("the count lasts the whole game, not just the turn") {
            val game = base().withCardInHand(1, "Test Plain Cycler").build()
            game.cycle("Test Plain Cycler")

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("now on the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            game.cycled(game.player1Id, "Test Plain Cycler") shouldBe 1
        }
    }
}
