package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ascended Lawmage (DGM #53) — {2}{W}{U} 3/2 Creature — Vedalken Wizard.
 *
 *   Flying
 *   Hexproof
 */
class AscendedLawmageScenarioTest : ScenarioTestBase() {
    init {
        test("has flying and can't be targeted by an opponent's spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ascended Lawmage")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lawmage = game.findPermanent("Ascended Lawmage")!!
            game.state.projectedState.hasKeyword(lawmage, Keyword.FLYING) shouldBe true

            withClue("hexproof: the opponent's Lightning Bolt can't target it") {
                game.castSpell(2, "Lightning Bolt", lawmage).error shouldNotBe null
            }
            game.isOnBattlefield("Ascended Lawmage") shouldBe true
        }
    }
}
