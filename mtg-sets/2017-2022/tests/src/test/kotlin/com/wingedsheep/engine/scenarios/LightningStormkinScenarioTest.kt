package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lightning Stormkin (M20 #213) — {U}{R} Creature — Elemental Wizard 2/2. "Flying / Haste"
 */
class LightningStormkinScenarioTest : ScenarioTestBase() {
    init {
        context("Lightning Stormkin") {
            test("attacks the turn it arrives (haste) and has flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Stormkin", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val stormkin = game.findPermanent("Lightning Stormkin")!!
                game.state.projectedState.hasKeyword(stormkin, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(stormkin, Keyword.HASTE) shouldBe true

                withClue("haste lets a summoning-sick Stormkin attack") {
                    game.declareAttackers(mapOf("Lightning Stormkin" to 2)).error shouldBe null
                }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("unblocked Stormkin deals 2 (20 -> 18)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
