package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lay Claim (AKH #61) — "Enchant permanent / You control enchanted permanent. / Cycling {2}"
 */
class LayClaimScenarioTest : ScenarioTestBase() {

    init {
        context("Lay Claim") {

            test("steals a noncreature permanent and control returns when the Aura leaves") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Lay Claim")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val forest = game.findPermanent("Forest")!!

                game.castSpell(1, "Lay Claim", forest).error shouldBe null
                game.resolveStack()

                withClue("Alice controls the enchanted Forest") {
                    game.isOnBattlefield("Lay Claim") shouldBe true
                    game.state.projectedState.getController(forest) shouldBe game.player1Id
                }

                val claim = game.findPermanent("Lay Claim")!!
                game.state = game.zones.moveToZone(game.state, claim, Zone.GRAVEYARD).state
                withClue("control reverts to Bob once the Aura is gone") {
                    game.state.projectedState.getController(forest) shouldBe game.player2Id
                }
            }

            test("cycling for {2} discards it and draws a card") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Lay Claim")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Lay Claim").error shouldBe null
                game.isInGraveyard(1, "Lay Claim") shouldBe true
                game.handSize(1) shouldBe 1
            }
        }
    }
}
