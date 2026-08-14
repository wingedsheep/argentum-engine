package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class GoddricCloakedRevelerScenarioTest : ScenarioTestBase() {

    init {
        test("the second nonland entry turns Goddric into a 4/4 flying Dragon") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Goddric, Cloaked Reveler")
                .withCardsInHand(1, "Ornithopter", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val goddric = game.findPermanent("Goddric, Cloaked Reveler")!!
            game.state.projectedState.getPower(goddric) shouldBe 3

            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(goddric) shouldBe 3

            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(goddric) shouldBe 4
            game.state.projectedState.getToughness(goddric) shouldBe 4
            game.state.projectedState.getSubtypes(goddric) shouldBe setOf("Dragon")
            game.state.projectedState.hasKeyword(goddric, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(goddric, Keyword.HASTE) shouldBe true
        }
    }
}
