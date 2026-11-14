package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.SphinxsTutelage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Sphinx's Tutelage (ORI #76): "Whenever you draw a card, target opponent mills two cards. If two
 * nonland cards that share a color were milled this way, repeat this process.
 * {5}{U}: Draw a card, then discard a card."
 *
 * The draw comes from the card's own loot ability. These pin the repeat loop: it keeps going only
 * while each pass's pair is two nonland cards sharing a colour, it stops on a colourless pair
 * (2015-06-22 ruling) and on a pair holding a land even when that land has the shared colour, and an
 * opponent whose library runs out ends the loop without losing the game.
 */
class SphinxsTutelageScenarioTest : ScenarioTestBase() {

    private val lootId = SphinxsTutelage.activatedAbilities.first().id

    /**
     * The Tutelage, six Islands and a Plains in hand; the controller's library holds one Island to
     * draw. [opponentLibrary] is listed top first.
     */
    private fun setup(opponentLibrary: List<String>): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Sphinx's Tutelage")
            .withLandsOnBattlefield(1, "Island", 6)
            .withCardInHand(1, "Plains")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponentLibrary.forEach { builder = builder.withCardInLibrary(2, it) } // first added = top
        return builder.build()
    }

    /** Activates the loot, discards the Plains, points the draw trigger at the opponent and resolves. */
    private fun TestGame.lootAndResolve() {
        val tutelage = findPermanent("Sphinx's Tutelage")!!
        val result = execute(ActivateAbility(playerId = player1Id, sourceId = tutelage, abilityId = lootId))
        withClue("${result.error}") { result.error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()

        var guard = 0
        while ((state.stack.isNotEmpty() || getPendingDecision() != null) && guard++ < 40) {
            when (getPendingDecision()) {
                null -> resolveStack()
                is ChooseTargetsDecision -> selectTargets(listOf(player2Id)).error shouldBe null
                // The loot's discard: throw away the Plains, keep the drawn Island.
                else -> selectCards(findCardsInHand(1, "Plains")).error shouldBe null
            }
        }
        withClue("everything resolved") {
            state.stack.isEmpty() shouldBe true
            getPendingDecision() shouldBe null
        }
    }

    private fun TestGame.namesIn(ids: List<EntityId>): List<String> =
        ids.map { state.getEntity(it)!!.get<CardComponent>()!!.name }

    init {
        test("repeats while the milled pair is two nonland cards sharing a colour, stopping on the first pair that doesn't") {
            val game = setup(
                listOf(
                    "Grizzly Bears", "Giant Growth",      // both green → repeat
                    "Lightning Bolt", "Lightning Helix",  // red and red-white share red → repeat
                    "Grizzly Bears", "Lightning Bolt",    // green vs red → stop
                    "Island", "Island",                   // never milled
                )
            )

            game.lootAndResolve()

            withClue("the loot drew the Island and discarded the Plains") {
                game.findCardsInHand(1, "Island").size shouldBe 1
                game.isInGraveyard(1, "Plains") shouldBe true
            }
            game.namesIn(game.state.getGraveyard(game.player2Id)).shouldContainExactlyInAnyOrder(
                "Grizzly Bears", "Giant Growth",
                "Lightning Bolt", "Lightning Helix",
                "Grizzly Bears", "Lightning Bolt",
            )
            game.namesIn(game.state.getLibrary(game.player2Id)).shouldContainExactly("Island", "Island")
        }

        test("two colourless cards don't share a colour, so the process stops after one pass") {
            val game = setup(listOf("Ornithopter", "Ornithopter", "Grizzly Bears", "Grizzly Bears"))

            game.lootAndResolve()

            game.namesIn(game.state.getGraveyard(game.player2Id))
                .shouldContainExactlyInAnyOrder("Ornithopter", "Ornithopter")
            game.librarySize(2) shouldBe 2
        }

        test("a land in the pair stops the process even when it shares the other card's colour") {
            // Dryad Arbor is a green land creature: green like the Bears, but a land card.
            val game = setup(listOf("Dryad Arbor", "Grizzly Bears", "Grizzly Bears", "Giant Growth"))

            game.lootAndResolve()

            game.namesIn(game.state.getGraveyard(game.player2Id))
                .shouldContainExactlyInAnyOrder("Dryad Arbor", "Grizzly Bears")
            game.librarySize(2) shouldBe 2
        }

        test("the loop ends when the opponent's library runs out, and milling it empty doesn't lose them the game") {
            // Five green creatures: two passes repeat, the third mills the last card alone and stops.
            val game = setup(List(5) { "Grizzly Bears" })

            game.lootAndResolve()

            game.graveyardSize(2) shouldBe 5
            game.librarySize(2) shouldBe 0
            game.state.gameOver shouldBe false
        }
    }
}
