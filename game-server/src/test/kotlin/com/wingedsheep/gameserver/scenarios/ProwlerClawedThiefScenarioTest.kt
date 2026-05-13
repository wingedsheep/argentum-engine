package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Prowler, Clawed Thief.
 *
 * Card reference:
 * - Prowler, Clawed Thief ({1}{U}{B}): Legendary Creature — Human Rogue Villain, 2/3
 *   "Menace"
 *   "Connive — Whenever another Villain you control enters, Prowler, Clawed Thief connives."
 */
class ProwlerClawedThiefScenarioTest : ScenarioTestBase() {

    init {
        context("Prowler, Clawed Thief — cast for mana cost") {

            test("resolves as a 2/3 Legendary Human Rogue Villain with Menace") {
                val game = scenario()
                    .withPlayers("ActivePlayer", "Opponent")
                    .withCardInHand(1, "Prowler, Clawed Thief")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Prowler, Clawed Thief")
                withClue("Casting Prowler, Clawed Thief should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Prowler, Clawed Thief should be on the battlefield") {
                    game.isOnBattlefield("Prowler, Clawed Thief") shouldBe true
                }

                val prowlerId = game.findPermanent("Prowler, Clawed Thief")!!
                val projected = game.state.projectedState

                withClue("Should be a 2/3") {
                    projected.getPower(prowlerId) shouldBe 2
                    projected.getToughness(prowlerId) shouldBe 3
                }

                withClue("Should be Legendary") {
                    projected.isLegendary(prowlerId) shouldBe true
                }

                withClue("Should be a Human Rogue Villain") {
                    projected.hasSubtype(prowlerId, "Human") shouldBe true
                    projected.hasSubtype(prowlerId, "Rogue") shouldBe true
                    projected.hasSubtype(prowlerId, "Villain") shouldBe true
                }

                withClue("Should have Menace") {
                    projected.hasKeyword(prowlerId, Keyword.MENACE) shouldBe true
                }
            }
        }
    }
}
