package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Aspirant's Ascent (ONE #40) — {U} Instant.
 *
 * "Until end of turn, target creature gets +1/+3 and gains flying and toxic 1."
 */
class AspirantsAscentScenarioTest : ScenarioTestBase() {

    init {
        test("target gets +1/+3, flying and toxic 1 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Aspirant's Ascent")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Aspirant's Ascent", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 5
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.hasKeyword(bears, "TOXIC_1") shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            val nextTurn = game.state.projectedState
            nextTurn.getPower(bears) shouldBe 2
            nextTurn.getToughness(bears) shouldBe 2
            nextTurn.hasKeyword(bears, Keyword.FLYING) shouldBe false
            nextTurn.hasKeyword(bears, "TOXIC_1") shouldBe false
        }
    }
}
