package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Millennial Gargoyle (GTC) — {4} 2/2 flying artifact creature. */
class MillennialGargoyleScenarioTest : ScenarioTestBase() {
    init {
        test("casts for generic mana and is a 2/2 flier") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Millennial Gargoyle")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Millennial Gargoyle").error shouldBe null
            game.resolveStack()

            val gargoyle = game.findPermanent("Millennial Gargoyle")!!
            val projected = game.state.projectedState
            projected.getPower(gargoyle) shouldBe 2
            projected.getToughness(gargoyle) shouldBe 2
            projected.hasKeyword(gargoyle, Keyword.FLYING) shouldBe true
            projected.isCreature(gargoyle) shouldBe true
        }
    }
}
