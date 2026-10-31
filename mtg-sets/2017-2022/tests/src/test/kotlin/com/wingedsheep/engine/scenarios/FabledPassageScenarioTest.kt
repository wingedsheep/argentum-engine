package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.FabledPassage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fabled Passage (ELD #244) — "{T}, Sacrifice this land: Search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle. Then if you control four or more lands, untap
 * that land."
 *
 * The land count is taken after the fetched land enters and after the Passage is sacrificed (per the
 * 2020-06-23 ruling): three other lands plus the fetched one reaches four and untaps it; two other
 * lands plus the fetched one is only three and leaves it tapped.
 */
class FabledPassageScenarioTest : ScenarioTestBase() {

    private val activateId = FabledPassage.activatedAbilities.first().id

    private fun fetchWithOtherLands(otherLands: Int): Boolean {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Fabled Passage")
        repeat(otherLands) { builder = builder.withCardOnBattlefield(1, "Plains") }
        val game = builder
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val passage = game.findPermanent("Fabled Passage")!!
        game.execute(ActivateAbility(game.player1Id, passage, activateId)).error shouldBe null
        game.resolveStack()

        withClue("the library search should be pending") { game.hasPendingDecision() shouldBe true }
        game.selectCards(game.findCardsInLibrary(1, "Forest"))
        game.resolveStack()

        withClue("the Passage sacrificed itself") { game.isInGraveyard(1, "Fabled Passage") shouldBe true }
        val forest = game.findPermanent("Forest")!!
        return game.state.getEntity(forest)?.has<TappedComponent>() == true
    }

    init {
        context("Fabled Passage — untap the fetched land at four or more lands") {

            test("three other lands plus the fetched land is four: the land is untapped") {
                withClue("the fetched land counts toward four, so it untaps") {
                    fetchWithOtherLands(3) shouldBe false
                }
            }

            test("two other lands plus the fetched land is three: the land stays tapped") {
                withClue("the sacrificed Passage doesn't count, so only three lands remain") {
                    fetchWithOtherLands(2) shouldBe true
                }
            }
        }
    }
}
