package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Endrek Sahr, Master Breeder — X Thrulls per creature spell (X = its mana value), and the state
 * trigger that sacrifices Endrek once you control seven or more Thrulls.
 */
class EndrekSahrMasterBreederScenarioTest : ScenarioTestBase() {
    init {
        context("Endrek Sahr, Master Breeder") {
            test("creates Thrulls equal to the creature spell's mana value, then is sacrificed at seven") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Endrek Sahr, Master Breeder")
                    .withCardInHand(1, "Hill Giant") // MV 4
                    .withCardInHand(1, "Centaur Courser") // MV 3
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                withClue("Hill Giant has mana value 4") {
                    game.findPermanents("Thrull Token").size shouldBe 4
                }
                game.findPermanent("Endrek Sahr, Master Breeder").shouldNotBeNull()

                game.castSpell(1, "Centaur Courser").error shouldBe null
                game.resolveStack()
                withClue("Centaur Courser adds three more, for seven Thrulls") {
                    game.findPermanents("Thrull Token").size shouldBe 7
                }
                withClue("Controlling seven Thrulls sacrifices Endrek Sahr") {
                    game.findPermanent("Endrek Sahr, Master Breeder") shouldBe null
                    game.isInGraveyard(1, "Endrek Sahr, Master Breeder") shouldBe true
                }
            }
        }
    }
}
