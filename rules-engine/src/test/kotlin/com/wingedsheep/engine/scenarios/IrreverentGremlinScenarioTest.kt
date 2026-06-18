package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Irreverent Gremlin (DSK #142) — {1}{R} Gremlin 2/2 with Menace.
 *
 * "Whenever another creature you control with power 2 or less enters, you may discard a card.
 *  If you do, draw a card. Do this only once each turn."
 *
 * The rummage is MayEffect(IfYouDoEffect(discard, draw)); "Do this only once each turn" is the
 * trigger-level oncePerTurn cap (CR 603.3b).
 */
class IrreverentGremlinScenarioTest : ScenarioTestBase() {

    init {
        context("Irreverent Gremlin rummage trigger") {

            test("a small creature entering lets you discard then draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Irreverent Gremlin")
                    .withCardInHand(1, "Grizzly Bears") // small creature to cast (triggers)
                    .withCardInHand(1, "Hill Giant")     // a card to discard
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Mountain")    // the card we draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                // Gremlin's "may discard a card. If you do, draw a card." — accept.
                if (game.hasPendingDecision()) {
                    game.answerYesNo(true)
                    game.resolveStack()
                }
                // Choose the card to discard (Hill Giant) if a selection is required.
                if (game.hasPendingDecision()) {
                    val hillGiant = game.state.getHand(game.player1Id).first { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                    }
                    game.selectCards(listOf(hillGiant))
                    game.resolveStack()
                }

                withClue("Hill Giant should have been discarded") {
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 1
                }
                withClue("the Mountain should have been drawn into hand") {
                    game.state.getHand(game.player1Id).count { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain"
                    } shouldBe 1
                }
            }

            test("the rummage triggers only once each turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Irreverent Gremlin")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Llanowar Elves") // second small creature, same turn
                    .withCardInHand(1, "Hill Giant")    // a single discardable card
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First small creature triggers the rummage — accept and discard Hill Giant.
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) { game.answerYesNo(true); game.resolveStack() }
                if (game.hasPendingDecision()) {
                    val hillGiant = game.state.getHand(game.player1Id).first { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                    }
                    game.selectCards(listOf(hillGiant))
                    game.resolveStack()
                }
                val graveyardAfterFirst = game.findCardsInGraveyard(1, "Hill Giant").size

                // Second small creature this turn — must NOT trigger the rummage again (oncePerTurn).
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()

                withClue("no second rummage decision should be pending") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no extra card was discarded by the second creature (oncePerTurn)") {
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe graveyardAfterFirst
                }
            }
        }
    }
}
