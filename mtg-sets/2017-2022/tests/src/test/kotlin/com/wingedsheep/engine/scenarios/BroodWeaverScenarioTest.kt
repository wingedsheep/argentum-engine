package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Brood Weaver (MID #173) — Reach; dies: create a 1/2 green Spider creature token with reach. */
class BroodWeaverScenarioTest : ScenarioTestBase() {
    init {
        test("dying creates a 1/2 green Spider token with reach") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Brood Weaver")
                .withCardInHand(2, "Doom Blade")
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val weaver = game.findPermanent("Brood Weaver")!!
            game.state.projectedState.hasKeyword(weaver, Keyword.REACH) shouldBe true

            game.castSpell(2, "Doom Blade", weaver).error shouldBe null
            game.resolveStack()

            withClue("Brood Weaver died") { game.isInGraveyard(1, "Brood Weaver") shouldBe true }
            val spider = game.findPermanent("Spider Token").shouldNotBeNull()
            val projected = game.state.projectedState
            projected.getPower(spider) shouldBe 1
            projected.getToughness(spider) shouldBe 2
            projected.hasKeyword(spider, Keyword.REACH) shouldBe true
            game.state.getBattlefield(game.player1Id).contains(spider) shouldBe true
        }
    }
}
