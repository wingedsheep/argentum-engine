package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Banefire (CON #58, reprinted J22 #495).
 *
 * "Banefire deals X damage to any target.
 *  If X is 5 or more, this spell can't be countered and the damage can't be prevented."
 *
 * Both halves of the rider hinge on the spell's own X: the card-level `cantBeCounteredIf` is read
 * off the spell on the stack when Cancel tries to counter it, and the prevention half is an `If`
 * at resolution. Crystal Barricade ("Prevent all noncombat damage that would be dealt to other
 * creatures you control") is the prevention shield.
 */
class BanefireScenarioTest : ScenarioTestBase() {

    private fun counterBattle(x: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Banefire")
        .withLandsOnBattlefield(1, "Mountain", x + 1)
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardInHand(2, "Cancel")
        .withLandsOnBattlefield(2, "Island", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun preventionBattle(x: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Banefire")
        .withLandsOnBattlefield(1, "Mountain", x + 1)
        .withCardOnBattlefield(2, "Crystal Barricade")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Banefire") {

            test("X = 5: Cancel can't counter it") {
                val game = counterBattle(5)
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castXSpell(1, "Banefire", 5, wurm).error shouldBe null
                game.passPriority()
                withClue("Cancel is still legal to cast — it just won't counter") {
                    game.castSpellTargetingStackSpell(2, "Cancel", "Banefire").error shouldBe null
                }
                game.resolveStack()

                withClue("Banefire resolved and 5 damage killed the 6/4 Craw Wurm") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                    game.isInGraveyard(2, "Cancel") shouldBe true
                    game.isInGraveyard(1, "Banefire") shouldBe true
                }
            }

            test("X = 4: Cancel counters it") {
                val game = counterBattle(4)
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castXSpell(1, "Banefire", 4, wurm).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Cancel", "Banefire").error shouldBe null
                game.resolveStack()

                withClue("Banefire was countered, so the Craw Wurm took no damage") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isInGraveyard(1, "Banefire") shouldBe true
                }
            }

            test("X = 5: the damage can't be prevented") {
                val game = preventionBattle(5)
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castXSpell(1, "Banefire", 5, wurm).error shouldBe null
                game.resolveStack()

                withClue("Crystal Barricade's shield doesn't stop 5 damage to the Craw Wurm") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                }
            }

            test("X = 4: the damage is prevented") {
                val game = preventionBattle(4)
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castXSpell(1, "Banefire", 4, wurm).error shouldBe null
                game.resolveStack()

                withClue("Crystal Barricade prevents all 4 damage — the Craw Wurm survives undamaged") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
            }
        }
    }
}
