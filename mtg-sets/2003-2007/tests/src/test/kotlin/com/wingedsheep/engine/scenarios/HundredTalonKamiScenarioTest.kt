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
 * Hundred-Talon Kami (CHK #16) — "Flying. Soulshift 4."
 *
 * Pins the mana-value bound as inclusive ("4 or less") and that the Kami (MV 5) can't return itself.
 */
class HundredTalonKamiScenarioTest : ScenarioTestBase() {

    init {
        context("Hundred-Talon Kami") {

            test("has flying") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Hundred-Talon Kami")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val talonKami = game.findPermanent("Hundred-Talon Kami")!!
                game.state.projectedState.hasKeyword(talonKami, Keyword.FLYING) shouldBe true
            }

            test("soulshift 4 can return a mana value 4 Spirit, but not the MV-5 Kami itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Hundred-Talon Kami")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(1, "Ghost Ship")       // Spirit, MV 4 — the inclusive bound
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talonKami = game.findPermanent("Hundred-Talon Kami")!!
                game.castSpell(1, "Lightning Bolt", talonKami).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Hundred-Talon Kami") shouldBe true

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val ghostShip = game.findCardsInGraveyard(1, "Ghost Ship").single()
                val kami = game.findCardsInGraveyard(1, "Kami of the Hunt").single()
                withClue("both Spirits at MV 4 or less are legal; the Kami itself (MV 5) is not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(ghostShip, kami)
                }
                game.selectTargets(listOf(ghostShip))
                game.resolveStack()

                game.isInHand(1, "Ghost Ship") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
                game.isInGraveyard(1, "Hundred-Talon Kami") shouldBe true
            }
        }
    }
}
