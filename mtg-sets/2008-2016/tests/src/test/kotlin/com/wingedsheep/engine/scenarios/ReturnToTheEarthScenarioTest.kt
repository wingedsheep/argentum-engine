package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Return to the Earth (FRF #135) — {3}{G} Instant.
 *
 *   Destroy target artifact, enchantment, or creature with flying.
 *
 * "With flying" qualifies only the creature branch: any artifact is legal, a non-flying non-artifact
 * creature is not.
 */
class ReturnToTheEarthScenarioTest : ScenarioTestBase() {

    private fun board(victim: String): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Return to the Earth")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardOnBattlefield(2, victim)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Return to the Earth") {

            test("destroys a creature with flying") {
                val game = board("Wind Drake")
                game.castSpell(1, "Return to the Earth", game.findPermanent("Wind Drake")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Wind Drake") shouldBe true
            }

            test("destroys an artifact without flying") {
                val game = board("Bottle Gnomes")
                game.castSpell(1, "Return to the Earth", game.findPermanent("Bottle Gnomes")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
            }

            test("can't target a creature without flying") {
                val game = board("Centaur Courser")
                withClue("Centaur Courser is neither an artifact, an enchantment, nor a flyer") {
                    game.castSpell(1, "Return to the Earth", game.findPermanent("Centaur Courser")!!).error shouldNotBe null
                }
                game.isOnBattlefield("Centaur Courser") shouldBe true
            }
        }
    }
}
