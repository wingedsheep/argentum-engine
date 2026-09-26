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
 * Vine Kami (CHK #249) — "Menace. Soulshift 6."
 *
 * Pins the mana-value bound as inclusive ("6 or less") and that Vine Kami (MV 7) can't return itself.
 */
class VineKamiScenarioTest : ScenarioTestBase() {

    init {
        context("Vine Kami") {

            test("has menace") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Vine Kami")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kami = game.findPermanent("Vine Kami")!!
                game.state.projectedState.hasKeyword(kami, Keyword.MENACE) shouldBe true
            }

            test("soulshift 6 can return a mana value 6 Spirit, but not the MV-7 Vine Kami itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Vine Kami")
                    .withCardInHand(1, "Terror")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Kami of Lunacy")   // Spirit, MV 6 — the inclusive bound
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vineKami = game.findPermanent("Vine Kami")!!
                game.castSpell(1, "Terror", vineKami).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Vine Kami") shouldBe true

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val lunacy = game.findCardsInGraveyard(1, "Kami of Lunacy").single()
                val hunt = game.findCardsInGraveyard(1, "Kami of the Hunt").single()
                withClue("both Spirits at MV 6 or less are legal; Vine Kami itself (MV 7) is not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(lunacy, hunt)
                }
                game.selectTargets(listOf(lunacy))
                game.resolveStack()

                game.isInHand(1, "Kami of Lunacy") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
                game.isInGraveyard(1, "Vine Kami") shouldBe true
            }
        }
    }
}
