package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Enhanced Surveillance (GRN #40) — {1}{U} Enchantment.
 *
 *   You may look at an additional two cards each time you surveil.
 *   Exile this enchantment: Shuffle your graveyard into your library.
 *
 * Cruel Witness ("Whenever you cast a noncreature spell, surveil 1") supplies the surveil.
 */
class EnhancedSurveillanceScenarioTest : ScenarioTestBase() {

    private fun baseScenario() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Enhanced Surveillance")
        .withCardOnBattlefield(1, "Cruel Witness", summoningSickness = false)
        .withCardInHand(1, "Lightning Bolt")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Enhanced Surveillance") {

            test("surveil 1 looks at three cards, and the extra two may go to the graveyard too") {
                val game = baseScenario().build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("surveil 1 plus two additional cards") {
                    decision.options.size shouldBe 3
                    decision.maxSelections shouldBe 3
                }
                game.selectCards(decision.options).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                withClue("Bolt plus all three surveiled cards are in the graveyard") {
                    game.graveyardSize(1) shouldBe 4
                }
                game.librarySize(1) shouldBe 1
            }

            test("exiling it shuffles your graveyard into your library") {
                val game = baseScenario()
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .build()
                val surveillance = game.findPermanent("Enhanced Surveillance")!!
                val abilityId = cardRegistry.getCard("Enhanced Surveillance")!!.activatedAbilities[0].id

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = surveillance, abilityId = abilityId))
                    .error shouldBe null
                game.resolveStack()

                game.findPermanent("Enhanced Surveillance") shouldBe null
                game.graveyardSize(1) shouldBe 0
                game.librarySize(1) shouldBe 6
            }
        }
    }
}
