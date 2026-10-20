package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Puppet Master (LEG #69) — {U}{U}{U} Enchantment — Aura.
 *
 * "When enchanted creature dies, return that card to its owner's hand. If that card is returned to
 *  its owner's hand this way, you may pay {U}{U}{U}. If you do, return this card to its owner's hand."
 *
 * Pins the owner routing of both returns and the "returned this way" gate on the {U}{U}{U} offer:
 * a dying token never reaches a hand, so no payment is offered.
 */
class PuppetMasterScenarioTest : ScenarioTestBase() {

    private fun killEnchantedBears(islands: Int, bearsIsToken: Boolean = false): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Grizzly Bears", isToken = bearsIsToken)
            .withCardAttachedTo(1, "Puppet Master", "Grizzly Bears")
            .withCardInHand(1, "Doom Blade")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (islands > 0) builder = builder.withLandsOnBattlefield(1, "Island", islands)
        val game = builder.build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Doom Blade", bears).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Puppet Master") {

            test("returns the dead creature to its owner's hand, then paying returns the Aura") {
                val game = killEnchantedBears(islands = 5)

                withClue("the opponent's creature card returns to its owner's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("the creature was returned this way, so {U}{U}{U} is offered") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Puppet Master returns from the graveyard to its owner's hand") {
                    game.isInHand(1, "Puppet Master") shouldBe true
                    game.isInGraveyard(1, "Puppet Master") shouldBe false
                }
            }

            test("declining to pay leaves Puppet Master in the graveyard") {
                val game = killEnchantedBears(islands = 5)

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(1, "Puppet Master") shouldBe true
                game.isInHand(1, "Puppet Master") shouldBe false
            }

            test("a token isn't returned to hand, so no payment is offered") {
                val game = killEnchantedBears(islands = 5, bearsIsToken = true)

                withClue("the token never reaches a hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe false
                }
                withClue("nothing was returned this way, so there is no {U}{U}{U} offer") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isInGraveyard(1, "Puppet Master") shouldBe true
            }
        }
    }
}
