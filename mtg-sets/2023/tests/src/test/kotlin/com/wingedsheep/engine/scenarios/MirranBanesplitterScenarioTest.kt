package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent

/** Mirran Banesplitter — flash Equipment that attaches on entering; equipped creature gets +2/+0. */
class MirranBanesplitterScenarioTest : ScenarioTestBase() {
    init {
        test("entering attaches it to the targeted creature, which gets +2/+0") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mirran Banesplitter")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mirran Banesplitter").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val gear = game.findPermanent("Mirran Banesplitter")!!
            game.state.getEntity(gear)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 2
        }
    }
}
