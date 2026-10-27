package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * In Search of Greatness (KHM #177) — {G}{G} Enchantment.
 *
 *   At the beginning of your upkeep, you may cast a permanent spell from your hand with mana value
 *   equal to 1 plus the greatest mana value among other permanents you control without paying its
 *   mana cost. If you don't, scry 1.
 *
 * "Other" excludes In Search of Greatness itself (MV 2): with only lands beside it, the target
 * mana value is 1, not 3.
 */
class InSearchOfGreatnessScenarioTest : ScenarioTestBase() {

    private fun board(battlefield: List<String>, hand: List<String>) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "In Search of Greatness")
        .apply { battlefield.forEach { withCardOnBattlefield(1, it) } }
        .apply { hand.forEach { withCardInHand(1, it) } }
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)
        .build()

    private fun TestGame.toUpkeepTrigger(): SelectCardsDecision {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
        return state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    init {
        context("In Search of Greatness") {
            test("casts a permanent with mana value one above the greatest other permanent, for free, and doesn't scry") {
                val game = board(
                    battlefield = listOf("Grizzly Bears"),
                    hand = listOf("Llanowar Elves", "Centaur Courser", "Hill Giant")
                )
                val courser = game.findCardsInHand(1, "Centaur Courser").single()

                val pick = game.toUpkeepTrigger()
                withClue("Grizzly Bears is MV 2, so only the MV 3 Centaur Courser is eligible") {
                    pick.options shouldBe listOf(courser)
                }
                game.selectCards(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("Centaur Courser was cast for free and resolved") {
                    game.findPermanent("Centaur Courser") shouldBe courser
                }
                withClue("casting a spell means no scry") { game.state.pendingDecision shouldBe null }
                game.isInHand(1, "Llanowar Elves") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
            }

            test("with only lands beside it, an MV 1 permanent is eligible; declining scries 1") {
                val game = board(
                    battlefield = emptyList(),
                    hand = listOf("Llanowar Elves", "Grizzly Bears", "Centaur Courser")
                )
                val elves = game.findCardsInHand(1, "Llanowar Elves").single()

                val pick = game.toUpkeepTrigger()
                withClue("In Search of Greatness doesn't count itself; lands are MV 0") {
                    pick.options shouldBe listOf(elves)
                }
                game.skipSelection().error shouldBe null

                withClue("not casting a spell scries 1") {
                    val scry = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                    scry.options.size shouldBe 1
                    game.skipSelection().error shouldBe null
                }
                game.isInHand(1, "Llanowar Elves") shouldBe true
                game.findPermanent("Llanowar Elves") shouldBe null
            }
        }
    }
}
