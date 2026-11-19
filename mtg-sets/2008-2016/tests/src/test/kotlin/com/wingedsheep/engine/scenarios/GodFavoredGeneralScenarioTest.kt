package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * God-Favored General (BNG #14) — "Inspired — Whenever this creature becomes untapped, you may pay
 * {2}{W}. If you do, create two 1/1 white Soldier enchantment creature tokens."
 */
class GodFavoredGeneralScenarioTest : ScenarioTestBase() {
    init {
        test("paying {2}{W} on untap creates two 1/1 white Soldier enchantment creature tokens") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "God-Favored General", tapped = true)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val soldiers = game.findPermanents("Soldier Token")
            soldiers.size shouldBe 2
            val projected = game.state.projectedState
            soldiers.forEach { soldier ->
                withClue("each token is a 1/1 white Soldier enchantment creature") {
                    projected.getPower(soldier) shouldBe 1
                    projected.getToughness(soldier) shouldBe 1
                    projected.getColors(soldier) shouldBe setOf("WHITE")
                    projected.hasType(soldier, "ENCHANTMENT") shouldBe true
                    projected.isCreature(soldier) shouldBe true
                }
            }
        }
    }
}
