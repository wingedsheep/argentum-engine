package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unhallowed Pact (AVR #124) — {2}{B} Enchantment — Aura.
 *
 * "When enchanted creature dies, return that card to the battlefield under your control."
 *
 * Pins that "your" is the Aura's controller — an opponent's creature comes back under the Pact
 * controller's control — and that a token doesn't return (2012-05-01 ruling).
 */
class UnhallowedPactScenarioTest : ScenarioTestBase() {

    private fun killEnchantedBears(bearsController: Int, bearsIsToken: Boolean = false): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(bearsController, "Grizzly Bears", isToken = bearsIsToken)
            .withCardAttachedTo(1, "Unhallowed Pact", "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
        game.resolveStack()
        game.checkStateBasedActions()
        game.resolveStack()
        return game
    }

    init {
        context("Unhallowed Pact") {

            test("an opponent's creature returns under the Aura controller's control") {
                val game = killEnchantedBears(bearsController = 2)

                val bears = game.findPermanent("Grizzly Bears")
                withClue("Bob's creature card came back from his graveyard") {
                    (bears != null) shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("under Alice's control, since she controls Unhallowed Pact") {
                    game.state.projectedState.getController(bears!!) shouldBe game.player1Id
                }
                withClue("the Aura itself stays in its owner's graveyard") {
                    game.isInGraveyard(1, "Unhallowed Pact") shouldBe true
                }
            }

            test("your own creature returns under your control") {
                val game = killEnchantedBears(bearsController = 1)

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getController(bears) shouldBe game.player1Id
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }

            test("a token creature doesn't return") {
                val game = killEnchantedBears(bearsController = 2, bearsIsToken = true)

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Unhallowed Pact") shouldBe true
            }
        }
    }
}
