package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Soulblast (CHK #190) — "As an additional cost to cast this spell, sacrifice all creatures you
 * control. Soulblast deals damage to any target equal to the total power of the sacrificed
 * creatures."
 */
class SoulblastScenarioTest : ScenarioTestBase() {

    init {
        context("Soulblast") {

            test("sacrifices every creature you control and deals their total power") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Soulblast")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(1, "Hill Giant")    // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Soulblast", 2).error shouldBe null

                withClue("the cost is paid on cast, before Soulblast resolves") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("the opponent's creature is not yours to sacrifice") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }

                game.resolveStack()
                game.getLifeTotal(2) shouldBe 15
            }

            test("reads the sacrificed creatures' power as they last existed on the battlefield") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Soulblast")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 3/3 under the anthem
                    .withCardOnBattlefield(1, "Hill Giant")    // 4/4 under the anthem
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Soulblast", 2).error shouldBe null
                game.resolveStack()

                withClue("the anthem isn't a creature, so it stays") {
                    game.findPermanent("Glorious Anthem").shouldNotBeNull()
                }
                game.getLifeTotal(2) shouldBe 13
            }

            test("is castable with no creatures and deals no damage") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Soulblast")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("sacrificing nothing is a payable cost") {
                    game.getLegalActions(1).any { it.description == "Cast Soulblast" } shouldBe true
                }
                game.castSpellTargetingPlayer(1, "Soulblast", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
