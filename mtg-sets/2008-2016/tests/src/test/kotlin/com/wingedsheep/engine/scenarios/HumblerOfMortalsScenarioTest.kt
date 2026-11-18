package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Humbler of Mortals (JOU #127) — {4}{G}{G} Enchantment Creature — Elemental 5/5.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, creatures
 *   you control gain trample until end of turn.
 */
class HumblerOfMortalsScenarioTest : ScenarioTestBase() {
    init {
        test("another enchantment entering gives your creatures, and only yours, trample") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Humbler of Mortals")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val humbler = game.findPermanent("Humbler of Mortals")!!
            val giant = game.findPermanent("Hill Giant")!!

            withClue("no trample before the trigger") {
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
            }

            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            withClue("creatures you control gained trample") {
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                game.state.projectedState.hasKeyword(humbler, Keyword.TRAMPLE) shouldBe true
            }
            withClue("the opponent's creature did not") {
                game.state.projectedState.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
