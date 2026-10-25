package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Valakut Exploration (ZNR #175).
 *
 *   Landfall — Whenever a land you control enters, exile the top card of your library. You may play
 *   that card for as long as it remains exiled.
 *   At the beginning of your end step, if there are cards exiled with this enchantment, put them
 *   into their owner's graveyard, then this enchantment deals that much damage to each opponent.
 *
 * The end-step half reads the enchantment's linked-exile pile, so the tests pin that the landfall
 * exile is linked (the cards are found and binned), that the damage equals the number binned, and
 * that a card already played out of exile is neither binned nor counted (no trigger at all when the
 * pile is empty — the intervening "if").
 */
class ValakutExplorationScenarioTest : ScenarioTestBase() {

    private fun valakut(vararg library: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Valakut Exploration")
        .withCardInHand(1, "Forest")
        .withLandsOnBattlefield(1, "Forest", 2)
        .apply { library.forEach { withCardInLibrary(1, it) } }
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.playForestFromHand() {
        val forest = findCardsInHand(1, "Forest").first()
        execute(PlayLand(player1Id, forest)).error shouldBe null
        resolveStack()
    }

    init {
        context("Valakut Exploration") {

            test("landfall exiles the top card; at end step it is binned and deals that much damage") {
                val game = valakut("Hill Giant", "Plains")

                game.playForestFromHand()
                withClue("the landfall trigger exiles the top card of the library") {
                    game.isInExile(1, "Hill Giant") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the exiled card is put into its owner's graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInExile(1, "Hill Giant") shouldBe false
                }
                withClue("one card binned → 1 damage to each opponent") {
                    game.getLifeTotal(2) shouldBe 19
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("a card played out of exile is not binned or counted") {
                val game = valakut("Grizzly Bears", "Plains")

                game.playForestFromHand()
                game.isInExile(1, "Grizzly Bears") shouldBe true

                withClue("the exiled card may be played") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("no cards remain exiled with it, so the end-step ability deals no damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
