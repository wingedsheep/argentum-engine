package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pippin's Bravery (LTR, {G} instant) — "You may sacrifice a Food. If you do, target creature gets
 * +4/+4 until end of turn. Otherwise, that creature gets +2/+2 until end of turn."
 *
 * Modeled as an [com.wingedsheep.sdk.scripting.effects.IfYouDoEffect] over a Gather → choose-up-to-1
 * → sacrifice pipeline. The optional selection is the "you may": choosing a Food sacrifices it and
 * grants +4/+4; declining (or controlling no Food, in which case there's nothing to choose and no
 * prompt appears) grants +2/+2.
 */
class PippinsBraveryScenarioTest : ScenarioTestBase() {

    init {
        test("no Food: nothing to sacrifice, so no prompt appears and the target gets +2/+2") {
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

        test("with a Food: sacrificing it gives +4/+4 and the Food is gone") {
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
            val food = game.findPermanent("Food")!!
            game.castSpell(1, "Pippin's Bravery", targetId = bear).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(food))
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 6
            game.state.projectedState.getToughness(bear) shouldBe 6
            game.findPermanent("Food") shouldBe null
        }

        test("with a Food: declining the sacrifice keeps the Food and gives only +2/+2") {
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
            game.hasPendingDecision() shouldBe true
            game.skipSelection()
            game.resolveStack()

            game.state.projectedState.getPower(bear) shouldBe 4
            game.state.projectedState.getToughness(bear) shouldBe 4
            game.findPermanent("Food") shouldNotBe null
        }
    }
}
