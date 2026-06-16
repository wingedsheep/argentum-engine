package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Primary Research {4}{W} Enchantment —
 * "When this enchantment enters, return target nonland permanent card with mana value 3 or less
 *  from your graveyard to the battlefield.
 *  At the beginning of your end step, if a card left your graveyard this turn, draw a card."
 *
 * Covers the ETB reanimation of a MV ≤ 3 nonland permanent and the intervening-if end-step draw
 * (the reanimation itself counts as a card leaving the graveyard). Also covers the end-step
 * trigger NOT firing when no card left the graveyard that turn.
 */
class PrimaryResearchScenarioTest : ScenarioTestBase() {

    init {
        context("Primary Research — ETB reanimate, then conditional end-step draw") {

            test("ETB returns a MV<=3 creature; end step draws because a card left the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Primary Research")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.castSpell(1, "Primary Research").error shouldBe null
                game.resolveStack() // resolve enchantment -> ETB trigger -> target decision

                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears is reanimated onto the battlefield") {
                    (game.findPermanent("Grizzly Bears") != null) shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("A card left the graveyard this turn, so the end step draws a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("end step does not draw when no card left the graveyard that turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Primary Research")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("No card left the graveyard, so the end step trigger does not draw") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
