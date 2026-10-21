package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Novice Occultist (MID #117) — "When this creature dies, you draw a card and you lose 1 life." */
class NoviceOccultistScenarioTest : ScenarioTestBase() {
    init {
        test("dying draws its controller a card and costs them 1 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Novice Occultist")
                .withCardInLibrary(1, "Swamp")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val occultist = game.findPermanent("Novice Occultist")!!
            val handBefore = game.handSize(1)

            game.castSpell(2, "Lightning Bolt", occultist).error shouldBe null
            game.resolveStack()

            withClue("Novice Occultist died") { game.isInGraveyard(1, "Novice Occultist") shouldBe true }
            game.handSize(1) shouldBe handBefore + 1
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
