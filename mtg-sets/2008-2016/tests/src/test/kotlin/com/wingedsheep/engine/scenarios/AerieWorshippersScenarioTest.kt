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
 * Aerie Worshippers (BNG #30) — "Inspired — Whenever this creature becomes untapped, you may pay
 * {2}{U}. If you do, create a 2/2 blue Bird enchantment creature token with flying."
 */
class AerieWorshippersScenarioTest : ScenarioTestBase() {

    private fun untapWorshippers(): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Aerie Worshippers", tapped = true)
            .withLandsOnBattlefield(1, "Island", 3)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("paying {2}{U} on untap creates a 2/2 flying Bird enchantment creature token") {
            val game = untapWorshippers()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val bird = game.findPermanents("Bird Token").single()
            val projected = game.state.projectedState
            withClue("the token is a 2/2 flying enchantment creature") {
                projected.getPower(bird) shouldBe 2
                projected.getToughness(bird) shouldBe 2
                projected.hasKeyword(bird, Keyword.FLYING) shouldBe true
                projected.hasType(bird, "ENCHANTMENT") shouldBe true
                projected.isCreature(bird) shouldBe true
            }
        }

        test("declining the payment creates nothing") {
            val game = untapWorshippers()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findPermanents("Bird Token").shouldBeEmpty()
        }
    }
}
