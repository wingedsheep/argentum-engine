package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class StreamOfAcidScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Acid Test Black Land") {
            typeLine = "Land"
            colorIndicator = "B"
        })

        for (victim in listOf("Grizzly Bears", "Forest", "Acid Test Black Land")) {
            test("destroys $victim") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stream of Acid")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, victim)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stream of Acid", game.findPermanent(victim)!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, victim) shouldBe true
                game.isInGraveyard(1, "Stream of Acid") shouldBe true
            }
        }

        test("cannot target a black creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Stream of Acid")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Dakmor Ghoul")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Stream of Acid", game.findPermanent("Dakmor Ghoul")!!).error shouldNotBe null
            game.isOnBattlefield("Dakmor Ghoul") shouldBe true
        }
    }
}
