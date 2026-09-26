package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gibbering Kami (CHK #112) — "Flying. Soulshift 3."
 *
 * Pins the mana-value bound ("3 or less") and that the Kami (MV 4) can't return itself.
 */
class GibberingKamiScenarioTest : ScenarioTestBase() {

    init {
        context("Gibbering Kami") {

            test("has flying") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Gibbering Kami")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val gk = game.findPermanent("Gibbering Kami")!!
                game.state.projectedState.hasKeyword(gk, Keyword.FLYING) shouldBe true
            }

            test("soulshift 3 returns a mana value 3 Spirit, but not an MV-4 Spirit or the Kami itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Gibbering Kami")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(1, "Ghost Ship")       // Spirit, MV 4 — above the bound
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3 — the inclusive bound
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gk = game.findPermanent("Gibbering Kami")!!
                game.castSpell(1, "Lightning Bolt", gk).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Gibbering Kami") shouldBe true

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val kami = game.findCardsInGraveyard(1, "Kami of the Hunt").single()
                withClue("only the MV-3 Spirit is legal; Ghost Ship and the Kami itself (both MV 4) are not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(kami)
                }
                game.selectTargets(listOf(kami))
                game.resolveStack()

                game.isInHand(1, "Kami of the Hunt") shouldBe true
                game.isInGraveyard(1, "Ghost Ship") shouldBe true
                game.isInGraveyard(1, "Gibbering Kami") shouldBe true
            }
        }
    }
}
