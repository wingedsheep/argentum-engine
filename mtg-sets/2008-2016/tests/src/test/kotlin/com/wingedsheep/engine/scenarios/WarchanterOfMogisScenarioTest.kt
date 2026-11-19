package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Warchanter of Mogis (BNG #85) — "Inspired — Whenever this creature becomes untapped, target
 * creature you control gains intimidate until end of turn."
 */
class WarchanterOfMogisScenarioTest : ScenarioTestBase() {
    init {
        test("untapping gives a creature you control intimidate; opponents' creatures aren't legal targets") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Warchanter of Mogis", tapped = true)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            val warchanter = game.findPermanent("Warchanter of Mogis")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("only creatures P1 controls are legal") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(warchanter, bears)
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears gains intimidate") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
                game.state.projectedState.hasKeyword(warchanter, Keyword.INTIMIDATE) shouldBe false
            }
        }
    }
}
