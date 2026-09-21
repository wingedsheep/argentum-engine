package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import com.wingedsheep.sdk.core.Keyword

class GeistOfSaintThaliaScenarioTest : ScenarioTestBase() {
    init {
        test("reduces your noncreature spell's generic cost") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state.projectedState.hasKeyword(game.findPermanent("Geist of Saint Thalia")!!, Keyword.FLYING) shouldBe true
            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()
            game.findCardsInHand(1, "Island").size shouldBe 2
        }
        test("does not discount creature spells") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Grizzly Bears").error.shouldNotBeNull()
        }
        test("does not discount an opponent's spells") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(2, "Divination")
                .withLandsOnBattlefield(2, "Island", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(2, "Divination").error.shouldNotBeNull()
        }
    }
}
