package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Iymrith, Desert Doom (AFR).
 *
 * Ward {4} applies only while Iymrith is untapped; the combat-damage trigger draws one card, then
 * refills the hand to three, reading the hand size after the first draw.
 */
class IymrithDesertDoomScenarioTest : ScenarioTestBase() {

    init {
        context("Iymrith, Desert Doom") {

            test("untapped, ward {4} counters unpaid removal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Iymrith, Desert Doom")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Murder", game.findPermanent("Iymrith, Desert Doom")!!).error shouldBe null
                game.resolveStack()

                withClue("ward {4} went unpaid, so Murder was countered") {
                    game.isOnBattlefield("Iymrith, Desert Doom") shouldBe true
                    game.isInGraveyard(2, "Murder") shouldBe true
                }
            }

            test("tapped, Iymrith has no ward and removal resolves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Iymrith, Desert Doom", tapped = true)
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Murder", game.findPermanent("Iymrith, Desert Doom")!!).error shouldBe null
                game.resolveStack()

                withClue("no ward trigger while tapped, so Murder destroyed Iymrith") {
                    game.getPendingDecision() shouldBe null
                    game.isInGraveyard(1, "Iymrith, Desert Doom") shouldBe true
                }
            }

            test("combat damage with an empty hand draws one, then refills to three") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Iymrith, Desert Doom")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.handSize(1) shouldBe 0
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Iymrith, Desert Doom" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("draw 1 (hand 1), then draw 3 - 1 = 2 more") {
                    game.getLifeTotal(2) shouldBe 15
                    game.handSize(1) shouldBe 3
                }
            }

            test("combat damage with a full hand draws only the first card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Iymrith, Desert Doom")
                    .withCardInHand(1, "Island")
                    .withCardInHand(1, "Island")
                    .withCardInHand(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Iymrith, Desert Doom" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("hand of 3 becomes 4; the refill amount floors at zero") {
                    game.handSize(1) shouldBe 4
                }
            }
        }
    }
}
