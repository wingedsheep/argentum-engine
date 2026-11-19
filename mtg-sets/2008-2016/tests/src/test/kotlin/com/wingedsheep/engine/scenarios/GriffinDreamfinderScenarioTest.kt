package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Griffin Dreamfinder (BNG #16) — "When this creature enters, return target enchantment card from
 * your graveyard to your hand."
 */
class GriffinDreamfinderScenarioTest : ScenarioTestBase() {
    init {
        test("entering returns the chosen enchantment card from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Griffin Dreamfinder")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardInGraveyard(1, "Evanescent Intellect")
                .withCardInGraveyard(1, "Ephara's Radiance")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val intellect = game.findCardsInGraveyard(1, "Evanescent Intellect").single()

            game.castSpell(1, "Griffin Dreamfinder").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("only enchantment cards in your graveyard are legal targets") {
                decision.legalTargets[0]!!.toSet() shouldBe setOf(
                    intellect,
                    game.findCardsInGraveyard(1, "Ephara's Radiance").single(),
                )
            }
            game.selectTargets(listOf(intellect)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Evanescent Intellect") shouldBe true
            game.isInGraveyard(1, "Ephara's Radiance") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
