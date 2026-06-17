package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Pippin's Bravery (LTR, {G} instant) — "You may sacrifice a Food. If you do, target creature gets
 * +4/+4 until end of turn. Otherwise, that creature gets +2/+2 until end of turn."
 *
 * Bug: with no Food the optional Sacrifice cost is unpayable, but the gate failed open and still
 * offered "Sacrifice a Food? -> Yes", wrongly granting +4/+4 without sacrificing anything. The
 * affordability check must recognize a sacrifice cost so the impossible "yes" is not offered and
 * the otherwise (+2/+2) branch runs instead.
 */
class PippinsBraveryScenarioTest : ScenarioTestBase() {

    init {
        test("no Food: the sacrifice is unpayable, so no 'yes' is offered and the target gets +2/+2") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Pippin's Bravery")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Pippin's Bravery", targetId = bear).error shouldBe null
            game.resolveStack()

            // No "may sacrifice a Food" question — you control none.
            game.hasPendingDecision() shouldBe false
            game.state.projectedState.getPower(bear) shouldBe 4
            game.state.projectedState.getToughness(bear) shouldBe 4
        }

        test("with a Food: accepting the sacrifice gives +4/+4 and sacrifices the Food") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Food")
                .withCardInHand(1, "Pippin's Bravery")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Pippin's Bravery", targetId = bear).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 6
            game.state.projectedState.getToughness(bear) shouldBe 6
            game.findPermanent("Food") shouldBe null
        }
    }
}
