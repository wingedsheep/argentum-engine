package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Satyr Nyx-Smith (BNG #109) — "Inspired — Whenever this creature becomes untapped, you may pay {2}{R}. If you do, create a 3/1 red Elemental enchantment creature token with haste."
 */
class SatyrNyxSmithScenarioTest : ScenarioTestBase() {
    private fun untap(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Satyr Nyx-Smith", tapped = true)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .extra()
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        return game
    }

    init {
        test("paying on untap creates a 3/1 enchantment creature token") {
            val game = untap()
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val token = game.findPermanents("Elemental Token").single()
            val projected = game.state.projectedState
            withClue("the token is a 3/1 enchantment creature") {
                projected.getPower(token) shouldBe 3
                projected.getToughness(token) shouldBe 1
                projected.hasType(token, "ENCHANTMENT") shouldBe true
                projected.isCreature(token) shouldBe true
                projected.hasKeyword(token, Keyword.HASTE) shouldBe true
            }
        }

        test("declining the payment creates nothing") {
            val game = untap()
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findPermanents("Elemental Token").shouldBeEmpty()
        }
    }
}
