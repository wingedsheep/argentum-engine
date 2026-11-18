package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cyclops of Eternal Fury (JOU #92) — {4}{R}{R} Enchantment Creature — Cyclops 5/3.
 *
 *   Creatures you control have haste.
 *
 * Covers the Cyclops hasting itself (not "other") and only its controller's creatures.
 */
class CyclopsOfEternalFuryScenarioTest : ScenarioTestBase() {
    init {
        test("the freshly cast Cyclops and your other creatures have haste and can attack; the opponent's don't") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Cyclops of Eternal Fury")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cyclops of Eternal Fury").error shouldBe null
            game.resolveStack()

            val cyclops = game.findPermanent("Cyclops of Eternal Fury")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projected = game.state.projectedState
            withClue("haste on the Cyclops itself and on your Bears") {
                projected.hasKeyword(cyclops, Keyword.HASTE) shouldBe true
                projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
            }
            withClue("not on the opponent's creature") {
                projected.hasKeyword(giant, Keyword.HASTE) shouldBe false
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("both summoning-sick creatures can attack") {
                game.declareAttackers(mapOf("Cyclops of Eternal Fury" to 2, "Grizzly Bears" to 2)).error shouldBe null
            }
        }
    }
}
