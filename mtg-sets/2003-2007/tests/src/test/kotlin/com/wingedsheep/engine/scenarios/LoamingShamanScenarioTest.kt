package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Loaming Shaman (DIS #87) — "When this creature enters, target player shuffles any number of
 * target cards from their graveyard into their library."
 *
 * The enters trigger's card slot depends on its player slot: only the chosen player's graveyard
 * is offered, and that player shuffles even when no cards are chosen.
 */
class LoamingShamanScenarioTest : ScenarioTestBase() {

    private fun shamanGame() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Loaming Shaman")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInGraveyard(1, "Hill Giant")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Llanowar Elves")
        .withCardInGraveyard(2, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castShaman() {
        castSpell(1, "Loaming Shaman").error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Loaming Shaman") {

            test("after choosing a player, only that player's graveyard is offered; chosen cards go to their library") {
                val game = shamanGame()
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                val elves = game.findCardsInGraveyard(2, "Llanowar Elves").single()
                val forest = game.findCardsInGraveyard(2, "Forest").single()
                val bobLibrary = game.librarySize(2)
                game.castShaman()

                val first = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                first.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(game.player1Id, game.player2Id)
                game.selectTargets(listOf(game.player2Id)).error shouldBe null

                val second = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("Bob's graveyard only — Alice's Hill Giant isn't offered") {
                    second.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, elves, forest)
                }
                second.targetRequirements.single().minTargets shouldBe 0
                game.selectTargets(listOf(bears, forest)).error shouldBe null
                game.resolveStack()

                game.state.getGraveyard(game.player2Id) shouldBe listOf(elves)
                game.librarySize(2) shouldBe bobLibrary + 2
                game.graveyardSize(1) shouldBe 1
            }

            test("choosing no cards leaves the graveyard alone") {
                val game = shamanGame()
                val bobLibrary = game.librarySize(2)
                game.castShaman()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.graveyardSize(2) shouldBe 3
                game.librarySize(2) shouldBe bobLibrary
            }
        }
    }
}
