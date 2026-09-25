package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kodama of the South Tree (CHK #223) — "Whenever you cast a Spirit or Arcane spell, each other
 * creature you control gets +1/+1 and gains trample until end of turn."
 */
class KodamaOfTheSouthTreeScenarioTest : ScenarioTestBase() {

    init {
        context("Kodama of the South Tree") {

            test("an Arcane spell pumps and grants trample to each other creature you control only") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Kodama of the South Tree")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Part the Veil")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kodama = game.findPermanent("Kodama of the South Tree")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Part the Veil").error shouldBe null
                // Resolve only the Kodama trigger, leaving Part the Veil on the stack.
                game.passPriority()
                game.passPriority()

                val projected = game.state.projectedState
                withClue("the other creature Alice controls got +1/+1 and trample") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
                withClue("the Kodama itself is untouched") {
                    projected.getPower(kodama) shouldBe 4
                    projected.hasKeyword(kodama, Keyword.TRAMPLE) shouldBe false
                }
                withClue("the opponent's creature is untouched") {
                    projected.getPower(giant) shouldBe 3
                    projected.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("a non-Spirit, non-Arcane spell doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Kodama of the South Tree")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 2
            }
        }
    }
}
