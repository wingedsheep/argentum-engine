package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.ImprobableAlliance
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Improbable Alliance (ELD #193) — {U}{R} Enchantment.
 *
 * "Whenever you draw your second card each turn, create a 1/1 blue Faerie creature token with
 *  flying. {4}{U}{R}: Draw a card, then discard a card."
 */
class ImprobableAllianceScenarioTest : ScenarioTestBase() {

    private val loot = ImprobableAlliance.activatedAbilities.single().id

    private fun game(drawnAlready: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Improbable Alliance")
        .withCardsDrawnThisTurn(1, drawnAlready)
        .withLandsOnBattlefield(1, "Island", 5)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, "Grizzly Bears")
        .also { builder -> repeat(3) { builder.withCardInLibrary(1, "Island") } }
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateLootAndDiscardBears() {
        execute(
            ActivateAbility(playerId = player1Id, sourceId = findPermanent("Improbable Alliance")!!, abilityId = loot),
        ).error shouldBe null
        resolveStack()
        getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        selectCards(findCardsInHand(1, "Grizzly Bears")).error shouldBe null
        resolveStack()
    }

    init {
        context("Improbable Alliance") {

            test("the loot draws the second card of the turn and makes a 1/1 flying Faerie") {
                val game = game(drawnAlready = 1)
                game.activateLootAndDiscardBears()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                val faeries = game.findPermanents("Faerie Token")
                faeries.size shouldBe 1
                val faerie = faeries.single()
                game.state.projectedState.getPower(faerie) shouldBe 1
                game.state.projectedState.getToughness(faerie) shouldBe 1
                game.state.projectedState.hasKeyword(faerie, Keyword.FLYING) shouldBe true
            }

            test("the first card drawn in a turn makes no Faerie") {
                val game = game(drawnAlready = 0)
                game.activateLootAndDiscardBears()

                withClue("only the second draw of the turn triggers") {
                    game.findPermanents("Faerie Token").size shouldBe 0
                }
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
