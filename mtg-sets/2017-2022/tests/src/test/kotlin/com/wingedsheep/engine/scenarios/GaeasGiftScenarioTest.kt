package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gaea's Gift (BRO #182) — +1/+1 counter on target creature you control, and it gains reach,
 * trample, hexproof, and indestructible until end of turn.
 */
class GaeasGiftScenarioTest : ScenarioTestBase() {

    init {
        test("puts a counter on the creature and grants all four keywords") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gaea's Gift")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Gaea's Gift", bears).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.REACH) shouldBe true
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
}
