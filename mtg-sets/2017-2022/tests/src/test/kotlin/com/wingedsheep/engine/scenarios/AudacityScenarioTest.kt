package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Audacity (BRO #169) — enchanted creature gets +2/+0 and has trample; when the Aura is put into
 * a graveyard from the battlefield, its controller draws a card.
 */
class AudacityScenarioTest : ScenarioTestBase() {

    init {
        test("grants +2/+0 and trample, and draws when the Aura is destroyed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Audacity")
                .withCardInHand(2, "Disenchant")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Audacity", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Grizzly Bears is a 4/2 with trample") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }

            val aura = game.findPermanent("Audacity")!!
            val handBefore = game.handSize(1)
            game.passPriority()
            game.castSpell(2, "Disenchant", aura).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Audacity") shouldBe true
            withClue("the Aura's graveyard trigger drew a card") {
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
