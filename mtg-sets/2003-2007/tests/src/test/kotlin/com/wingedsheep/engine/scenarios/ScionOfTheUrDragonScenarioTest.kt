package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ScionOfTheUrDragon
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scion of the Ur-Dragon (TSP #246) — "{2}: Search your library for a Dragon permanent card and put
 * it into your graveyard. If you do, Scion of the Ur-Dragon becomes a copy of that card until end of
 * turn. Then shuffle."
 *
 * Pins the composition: the copy reads the card in the graveyard (`sourceFromAnyZone`), is gated on
 * a card actually having been put there ("if you do"), and reverts at end of turn.
 */
class ScionOfTheUrDragonScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = ScionOfTheUrDragon.activatedAbilities[0].id

        context("Scion of the Ur-Dragon") {

            fun board(withDragon: Boolean): TestGame {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scion of the Ur-Dragon")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (withDragon) builder.withCardInLibrary(1, "Shivan Dragon")
                repeat(5) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Island")
                }
                return builder.build()
            }

            fun TestGame.activate() {
                val scion = findPermanent("Scion of the Ur-Dragon")!!
                val result = execute(ActivateAbility(playerId = player1Id, sourceId = scion, abilityId = abilityId))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
            }

            test("puts the Dragon into the graveyard and becomes a copy of it until end of turn") {
                val game = board(withDragon = true)
                val scion = game.findPermanent("Scion of the Ur-Dragon")!!

                game.activate()
                val dragon = game.findCardsInLibrary(1, "Shivan Dragon").single()
                game.selectCards(listOf(dragon)).error shouldBe null
                game.resolveStack()

                withClue("the found Dragon went to the graveyard") {
                    game.isInGraveyard(1, "Shivan Dragon") shouldBe true
                }
                withClue("Scion is now a copy of Shivan Dragon") {
                    game.state.getEntity(scion)!!.get<CardComponent>()!!.name shouldBe "Shivan Dragon"
                    game.state.projectedState.getPower(scion) shouldBe 5
                    game.state.projectedState.getToughness(scion) shouldBe 5
                    game.state.projectedState.hasKeyword(scion, Keyword.FLYING) shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the copy ends with the turn") {
                    game.state.getEntity(scion)!!.get<CardComponent>()!!.name shouldBe "Scion of the Ur-Dragon"
                    game.state.projectedState.getPower(scion) shouldBe 4
                }
            }

            test("finding nothing leaves Scion as itself") {
                val game = board(withDragon = false)
                val scion = game.findPermanent("Scion of the Ur-Dragon")!!

                game.activate()
                if (game.hasPendingDecision()) game.skipSelection()
                game.resolveStack()

                withClue("no card was put into the graveyard, so there is nothing to copy") {
                    game.graveyardSize(1) shouldBe 0
                    game.state.getEntity(scion)!!.get<CardComponent>()!!.name shouldBe "Scion of the Ur-Dragon"
                }
            }
        }
    }
}
