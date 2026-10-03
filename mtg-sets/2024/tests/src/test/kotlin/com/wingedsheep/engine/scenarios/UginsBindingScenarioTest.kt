package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ugin's Binding (MH3) — bounce target nonland permanent you don't control; from the graveyard,
 * casting a colorless spell with MV 7+ lets you exile it to bounce every nonland permanent you
 * don't control.
 */
class UginsBindingScenarioTest : ScenarioTestBase() {

    private fun graveyardSetup(bigSpell: String = "Pathrazer of Ulamog", lands: Int = 11) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInGraveyard(1, "Ugin's Binding")
        .withCardInHand(1, bigSpell)
        .withLandsOnBattlefield(1, "Wastes", lands)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Ornithopter")
        .withLandsOnBattlefield(2, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Ugin's Binding") {
            test("the spell returns target nonland permanent an opponent controls to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ugin's Binding")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ugin's Binding", bears).error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Ugin's Binding") shouldBe true
            }

            test("casting a 7+ MV colorless spell lets you exile it to bounce every nonland permanent you don't control") {
                val game = graveyardSetup()

                game.castSpell(1, "Pathrazer of Ulamog").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Ugin's Binding exiled from the graveyard") {
                    game.isInExile(1, "Ugin's Binding") shouldBe true
                }
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isInHand(2, "Ornithopter") shouldBe true
                withClue("lands stay") { game.findPermanent("Forest") shouldNotBe null }
                withClue("your own permanents stay") {
                    game.findPermanent("Hill Giant") shouldNotBe null
                    game.findPermanent("Pathrazer of Ulamog") shouldNotBe null
                }
            }

            test("declining leaves it in the graveyard and bounces nothing") {
                val game = graveyardSetup()

                game.castSpell(1, "Pathrazer of Ulamog").error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Ugin's Binding") shouldBe true
                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.findPermanent("Ornithopter") shouldNotBe null
            }

            test("a colored spell with mana value 7 does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Ugin's Binding")
                    .withCardInHand(1, "Whiptail Wurm")
                    .withLandsOnBattlefield(1, "Wastes", 6)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Whiptail Wurm").error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.isInGraveyard(1, "Ugin's Binding") shouldBe true
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }

            test("a colorless spell with mana value below 7 does not trigger it") {
                val game = graveyardSetup(bigSpell = "Ornithopter", lands = 0)

                game.castSpell(1, "Ornithopter").error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.isInGraveyard(1, "Ugin's Binding") shouldBe true
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }
    }
}
