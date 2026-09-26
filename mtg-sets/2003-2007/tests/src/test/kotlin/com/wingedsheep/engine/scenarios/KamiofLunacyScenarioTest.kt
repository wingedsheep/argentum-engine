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
 * Kami of Lunacy (CHK #119) — "Flying. Soulshift 5."
 *
 * Pins the mana-value bound as inclusive ("5 or less") and that Kami of Lunacy (MV 6) can't return itself.
 */
class KamiofLunacyScenarioTest : ScenarioTestBase() {

    init {
        context("Kami of Lunacy") {

            test("has flying") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Kami of Lunacy")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kami = game.findPermanent("Kami of Lunacy")!!
                game.state.projectedState.hasKeyword(kami, Keyword.FLYING) shouldBe true
            }

            test("soulshift 5 can return a mana value 5 Spirit, but not the MV-6 Kami itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Kami of Lunacy")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(1, "Venerable Kumo")   // Spirit, MV 5 — the inclusive bound
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lunacy = game.findPermanent("Kami of Lunacy")!!
                game.castSpell(1, "Lightning Bolt", lunacy).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Kami of Lunacy") shouldBe true

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val kumo = game.findCardsInGraveyard(1, "Venerable Kumo").single()
                val hunt = game.findCardsInGraveyard(1, "Kami of the Hunt").single()
                withClue("both Spirits at MV 5 or less are legal; Kami of Lunacy itself (MV 6) is not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(kumo, hunt)
                }
                game.selectTargets(listOf(kumo))
                game.resolveStack()

                game.isInHand(1, "Venerable Kumo") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
                game.isInGraveyard(1, "Kami of Lunacy") shouldBe true
            }
        }
    }
}
