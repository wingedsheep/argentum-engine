package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Forlorn Pseudamma (BNG #71) — "Inspired — Whenever this creature becomes untapped, you may pay
 * {2}{B}. If you do, create a 2/2 black Zombie enchantment creature token."
 */
class ForlornPseudammaScenarioTest : ScenarioTestBase() {

    private fun untapPseudamma(): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Forlorn Pseudamma", tapped = true)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("paying {2}{B} on untap creates a 2/2 black Zombie enchantment creature token") {
            val game = untapPseudamma()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val zombie = game.findPermanents("Zombie Token").single()
            val projected = game.state.projectedState
            withClue("the token is a 2/2 black Zombie enchantment creature") {
                projected.getPower(zombie) shouldBe 2
                projected.getToughness(zombie) shouldBe 2
                projected.getColors(zombie) shouldBe setOf("BLACK")
                projected.hasType(zombie, "ENCHANTMENT") shouldBe true
                projected.isCreature(zombie) shouldBe true
            }
        }

        test("declining the payment creates nothing") {
            val game = untapPseudamma()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findPermanents("Zombie Token").shouldBeEmpty()
        }
    }
}
