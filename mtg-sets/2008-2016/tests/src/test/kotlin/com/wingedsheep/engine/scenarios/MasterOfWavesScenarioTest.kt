package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Master of Waves (THS #53) — {3}{U} Creature — Merfolk Wizard 2/1.
 *
 * "Protection from red
 *  Elemental creatures you control get +1/+1.
 *  When this creature enters, create a number of 1/0 blue Elemental creature tokens equal to your
 *  devotion to blue."
 *
 * The tokens are 1/0 and only survive state-based actions because the Master's lord applies to
 * them: they enter as 2/1 and die once the Master leaves the battlefield.
 */
class MasterOfWavesScenarioTest : ScenarioTestBase() {

    init {
        context("Master of Waves") {

            test("creates tokens equal to devotion to blue; they live on the lord and die when it leaves") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vaporkin")
                    .withCardInHand(1, "Master of Waves")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInHand(1, "Unsummon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder.withCardInLibrary(2, "Swamp") }
                val game = builder.build()

                game.castSpell(1, "Master of Waves").error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Elemental Token")
                withClue("devotion to blue is 2 (Vaporkin's {U} + the Master's {U})") {
                    tokens shouldHaveSize 2
                }
                val projected = game.state.projectedState
                withClue("each 1/0 token is a 2/1 under the lord") {
                    tokens.forEach {
                        projected.getPower(it) shouldBe 2
                        projected.getToughness(it) shouldBe 1
                    }
                }
                val vaporkin = game.findPermanent("Vaporkin")!!
                withClue("nontoken Elementals you control are pumped too") {
                    projected.getPower(vaporkin) shouldBe 3
                    projected.getToughness(vaporkin) shouldBe 2
                }
                val master = game.findPermanent("Master of Waves")!!
                withClue("the Master is a Merfolk, so its own lord doesn't pump it") {
                    projected.getPower(master) shouldBe 2
                    projected.getToughness(master) shouldBe 1
                }

                game.castSpell(1, "Unsummon", master).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Master of Waves") shouldBe false
                withClue("without the lord the 1/0 tokens die to state-based actions") {
                    game.findPermanents("Elemental Token").shouldBeEmpty()
                }
                game.state.projectedState.getPower(vaporkin) shouldBe 2
            }
        }
    }
}
