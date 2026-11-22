package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Urbis Protector (GTC) — When this creature enters, create a 4/4 white Angel creature token with flying. */
class UrbisProtectorScenarioTest : ScenarioTestBase() {
    init {
        test("entering creates a 4/4 flying Angel token") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Urbis Protector")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Urbis Protector")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Urbis Protector") shouldBe true
            val angels = game.findPermanents("Angel Token")
            withClue("exactly one Angel token") { angels.size shouldBe 1 }
            val angel = angels.single()
            game.state.projectedState.getPower(angel) shouldBe 4
            game.state.projectedState.getToughness(angel) shouldBe 4
            game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
        }
    }
}
