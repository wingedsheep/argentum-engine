package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Damage Control Crew (SPM #99) —
 * {3}{G} Creature — Human Citizen, 3/3, Uncommon.
 *
 *   When this creature enters, choose one —
 *   • Repair — Return target card with mana value 4 or greater from your graveyard to your hand.
 *   • Impound — Exile target artifact or enchantment.
 *
 * Covers both ETB modes: the graveyard-return restricted to mana value 4 or greater across
 * any card type (Repair), and the exile of a battlefield artifact-or-enchantment (Impound).
 */
class DamageControlCrewScenarioTest : ScenarioTestBase() {

    init {
        context("Damage Control Crew") {

            test("Repair returns only a card with mana value 4 or greater from your graveyard") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Damage Control Crew")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Hill Giant")    // {3}{R} — mana value 4, eligible
                    .withCardInGraveyard(1, "Grizzly Bears") // {1}{G} — mana value 2, ineligible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("precondition: two cards in your graveyard") {
                    game.graveyardSize(1) shouldBe 2
                }

                val cast = game.castSpell(1, "Damage Control Crew")
                withClue("Damage Control Crew should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

                val targetDecision = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for Repair; got ${game.getPendingDecision()}")
                val giantInGrave = game.findCardsInGraveyard(1, "Hill Giant").single()
                game.selectTargets(listOf(giantInGrave))
                game.resolveStack()

                withClue("the mana value 4 card returns to your hand") {
                    game.findCardsInHand(1, "Hill Giant").size shouldBe 1
                }
                withClue("the mana value 2 card is not eligible and stays in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("Impound exiles target artifact or enchantment") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Damage Control Crew")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Hill Giant")    // {3}{R} — makes Repair a legal mode too
                    .withCardOnBattlefield(2, "Ornithopter") // an artifact the opponent controls
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ornithopter = game.findPermanent("Ornithopter")!!

                val cast = game.castSpell(1, "Damage Control Crew")
                withClue("Damage Control Crew should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 1))

                val targetDecision = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for Impound; got ${game.getPendingDecision()}")
                game.selectTargets(listOf(ornithopter))
                game.resolveStack()

                withClue("the targeted artifact is exiled") {
                    game.isInExile(2, "Ornithopter") shouldBe true
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }
            }
        }
    }
}
