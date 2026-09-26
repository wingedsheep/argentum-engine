package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thousand-legged Kami (CHK #246) — "Soulshift 7."
 *
 * Pins the mana-value bound as inclusive ("7 or less"), and that an MV-8 Spirit — including the Kami
 * itself — is not a legal target.
 */
class ThousandleggedKamiScenarioTest : ScenarioTestBase() {

    init {
        context("Thousand-legged Kami") {

            test("is a 6/6") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Thousand-legged Kami")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kami = game.findPermanent("Thousand-legged Kami")!!
                game.state.projectedState.getPower(kami) shouldBe 6
                game.state.projectedState.getToughness(kami) shouldBe 6
            }

            test("soulshift 7 can return a mana value 7 Spirit, but not an MV-8 one or the Kami itself") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Thousand-legged Kami")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Eternal Dragon")          // Dragon Spirit, MV 7 — inclusive bound
                    .withCardInGraveyard(1, "Myojin of Night's Reach") // Spirit, MV 8 — too big
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kami = game.findPermanent("Thousand-legged Kami")!!
                game.castSpell(1, "Murder", kami).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Thousand-legged Kami") shouldBe true

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val dragon = game.findCardsInGraveyard(1, "Eternal Dragon").single()
                withClue("only the MV-7 Spirit is legal; the MV-8 Myojin and the Kami (MV 8) are not") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(dragon)
                }
                game.selectTargets(listOf(dragon))
                game.resolveStack()

                game.isInHand(1, "Eternal Dragon") shouldBe true
                game.isInGraveyard(1, "Myojin of Night's Reach") shouldBe true
                game.isInGraveyard(1, "Thousand-legged Kami") shouldBe true
            }
        }
    }
}
