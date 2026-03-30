package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ScalesOfShaleScenarioTest : ScenarioTestBase() {

    init {
        context("Scales of Shale - combat trick") {
            test("gives target creature +2/+0, lifelink, and indestructible until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scales of Shale")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Steampath Charger") // 2/1 Lizard Warlock
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Steampath Charger")!!
                game.castSpell(1, "Scales of Shale", targetId)
                game.resolveStack()

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]!!

                withClue("Steampath Charger should have 4 power (2 + 2)") {
                    cardInfo.power shouldBe 4
                }
                withClue("Steampath Charger should have 1 toughness (unchanged)") {
                    cardInfo.toughness shouldBe 1
                }
                withClue("Steampath Charger should have lifelink") {
                    cardInfo.keywords.contains(Keyword.LIFELINK) shouldBe true
                }
                withClue("Steampath Charger should have indestructible") {
                    cardInfo.keywords.contains(Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }
        }

        context("Scales of Shale - affinity for Lizards") {
            test("costs less with Lizards on the battlefield") {
                // Scales of Shale costs {2}{B}, with 2 Lizards it should cost just {B}
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scales of Shale")
                    .withLandsOnBattlefield(1, "Swamp", 1) // Only 1 Swamp — enough with 2 Lizards
                    .withCardOnBattlefield(1, "Steampath Charger") // Lizard
                    .withCardOnBattlefield(1, "Cindering Cutthroat") // Lizard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // With 2 Lizards, cost is {2}{B} - 2 = {B}
                // Player has 1 Swamp = {B}, so this should be castable
                val targetId = game.findPermanent("Steampath Charger")!!
                val result = game.castSpell(1, "Scales of Shale", targetId)

                withClue("Should be able to cast with reduced cost") {
                    result.isSuccess shouldBe true
                }
            }
        }
    }
}
