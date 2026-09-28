package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Invasion of Karsus // Refraction Elemental. */
class InvasionOfKarsusScenarioTest : ScenarioTestBase() {
    init {
        test("front: 3 damage to each creature — small ones die, big ones survive damaged") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Karsus")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Air Elemental")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Karsus").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Air Elemental") shouldBe true
            game.isOnBattlefield("Invasion of Karsus") shouldBe true
        }

        test("back: defeated Siege becomes Refraction Elemental, which pings on each spell cast") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Karsus")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Karsus")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val elemental = game.findPermanent("Refraction Elemental")!!
            game.state.projectedState.getPower(elemental) shouldBe 4

            val before = game.getLifeTotal(2)
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe before - 3 - 2
        }
    }
}
